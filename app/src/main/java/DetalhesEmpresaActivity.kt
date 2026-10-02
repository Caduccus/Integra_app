package com.example.plataformaremota

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.adapter.Membro
import com.example.plataformaremota.adapter.MembroGrupoAdapter
import com.example.plataformaremota.adapter.TrabalhoAdapter
import com.example.plataformaremota.data.entity.Empresa
import com.example.plataformaremota.data.entity.Trabalho
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DetalhesEmpresaActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var cardLogo: MaterialCardView
    private lateinit var badgeCamera: MaterialCardView
    private lateinit var imgLogo: ImageView
    private lateinit var txtNome: TextView
    private lateinit var txtArea: TextView
    private lateinit var txtDescricao: TextView

    private lateinit var linhaCnpj: View
    private lateinit var txtCnpj: TextView
    private lateinit var linhaEndereco: View
    private lateinit var txtEndereco: TextView
    private lateinit var linhaEmail: View
    private lateinit var txtEmail: TextView

    private lateinit var txtTotalMembros: TextView
    private lateinit var blocoPendentes: View
    private lateinit var txtTotalPendentes: TextView

    private lateinit var rvMembros: RecyclerView
    private lateinit var rvTrabalhos: RecyclerView
    private lateinit var txtSemTrabalhos: TextView
    private lateinit var btnAcao: MaterialButton

    private lateinit var membroAdapter: MembroGrupoAdapter
    private lateinit var trabalhoAdapter: TrabalhoAdapter

    private lateinit var empresaRepository: EmpresaRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var empresaId: String = ""
    private var empresaAtual: Empresa? = null
    private var souDono: Boolean = false
    private var uploadingLogo: Boolean = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) uploadLogo(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_empresa)

        empresaId = intent.getStringExtra("empresaId") ?: ""

        if (empresaId.isEmpty()) {
            Toast.makeText(this, "Empresa não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        empresaRepository = EmpresaRepository()

        btnVoltar = findViewById(R.id.btnVoltarDetalhesEmpresa)
        cardLogo = findViewById(R.id.cardLogoDetalhesEmpresa)
        badgeCamera = findViewById(R.id.badgeCameraEmpresa)
        imgLogo = findViewById(R.id.imgLogoDetalhesEmpresa)
        txtNome = findViewById(R.id.txtNomeDetalhesEmpresa)
        txtArea = findViewById(R.id.txtAreaDetalhesEmpresa)
        txtDescricao = findViewById(R.id.txtDescricaoDetalhesEmpresa)

        linhaCnpj = findViewById(R.id.linhaCnpj)
        txtCnpj = findViewById(R.id.txtCnpjDetalhesEmpresa)
        linhaEndereco = findViewById(R.id.linhaEndereco)
        txtEndereco = findViewById(R.id.txtEnderecoDetalhesEmpresa)
        linhaEmail = findViewById(R.id.linhaEmail)
        txtEmail = findViewById(R.id.txtEmailDetalhesEmpresa)

        txtTotalMembros = findViewById(R.id.txtTotalMembrosEmpresa)
        blocoPendentes = findViewById(R.id.blocoPendentes)
        txtTotalPendentes = findViewById(R.id.txtTotalPendentesEmpresa)

        rvMembros = findViewById(R.id.rvMembrosDetalhes)
        rvTrabalhos = findViewById(R.id.rvTrabalhosDetalhes)
        txtSemTrabalhos = findViewById(R.id.txtSemTrabalhos)
        btnAcao = findViewById(R.id.btnAcaoDetalhesEmpresa)

        configurarRecyclers()

        btnVoltar.setOnClickListener { finish() }

        // ⭐ Toque no logo → abre galeria (se for dono)
        cardLogo.setOnClickListener {
            if (!souDono) {
                Toast.makeText(
                    this,
                    "Apenas o dono pode editar a foto da empresa",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (uploadingLogo) return@setOnClickListener
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        carregarTudo()

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun configurarRecyclers() {
        membroAdapter = MembroGrupoAdapter(emptyList()) { membro ->
            val intent = Intent(this, PerfilUsuarioActivity::class.java)
            intent.putExtra("uidUsuario", membro.uid)
            intent.putExtra("chatId", "")
            startActivity(intent)
        }
        rvMembros.layoutManager = LinearLayoutManager(this)
        rvMembros.adapter = membroAdapter

        trabalhoAdapter = TrabalhoAdapter(emptyList()) { trabalho ->
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("trabalhoId", trabalho.id)
            startActivity(intent)
        }
        rvTrabalhos.layoutManager = LinearLayoutManager(this)
        rvTrabalhos.adapter = trabalhoAdapter
    }

    private fun carregarTudo() {
        lifecycleScope.launch {
            try {
                val empresa = empresaRepository.buscarPorId(empresaId)
                if (empresa == null) {
                    Toast.makeText(
                        this@DetalhesEmpresaActivity,
                        "Empresa não encontrada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                empresaAtual = empresa
                preencherInfo(empresa)
                carregarMembros(empresa)
                carregarTrabalhos()
                configurarBotaoAcao(empresa)
            } catch (e: Exception) {
                Toast.makeText(
                    this@DetalhesEmpresaActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun preencherInfo(empresa: Empresa) {
        val uid = auth.currentUser?.uid ?: ""
        souDono = empresa.criadorId == uid

        txtNome.text = empresa.nome
        txtArea.text = empresa.areaAtuacao
        txtDescricao.text = empresa.descricao

        if (empresa.logoUrl.isNotEmpty()) {
            Glide.with(this).load(empresa.logoUrl).circleCrop().into(imgLogo)
            imgLogo.imageTintList = null
            imgLogo.setPadding(0, 0, 0, 0)
        }

        // ⭐ Badge de câmera só pro dono
        badgeCamera.visibility = if (souDono) View.VISIBLE else View.GONE

        if (empresa.cnpj.isNotEmpty()) {
            linhaCnpj.visibility = View.VISIBLE
            txtCnpj.text = empresa.cnpj
        }
        if (empresa.endereco.isNotEmpty()) {
            linhaEndereco.visibility = View.VISIBLE
            txtEndereco.text = empresa.endereco
        }
        if (empresa.emailCorporativo.isNotEmpty()) {
            linhaEmail.visibility = View.VISIBLE
            txtEmail.text = empresa.emailCorporativo
        }

        txtTotalMembros.text = empresa.membros.size.toString()

        val souAdmin = uid in empresa.admins

        if (souAdmin && empresa.pendentes.isNotEmpty()) {
            blocoPendentes.visibility = View.VISIBLE
            txtTotalPendentes.text = empresa.pendentes.size.toString()
        } else {
            blocoPendentes.visibility = View.GONE
        }
    }

    private suspend fun carregarMembros(empresa: Empresa) {
        val membros = mutableListOf<Membro>()

        for (uid in empresa.membros) {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                membros.add(
                    Membro(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: "",
                        ehAdmin = empresa.admins.contains(uid) || empresa.criadorId == uid
                    )
                )
            } catch (_: Exception) { }
        }

        val ordenados = membros.sortedWith(
            compareByDescending<Membro> { it.ehAdmin }.thenBy { it.nome.lowercase() }
        )

        membroAdapter.atualizarLista(ordenados)
    }

    private suspend fun carregarTrabalhos() {
        try {
            val snapshot = db.collection("trabalhos")
                .whereEqualTo("empresaId", empresaId)
                .get()
                .await()

            val lista = snapshot.documents.mapNotNull {
                it.toObject(Trabalho::class.java)
            }.sortedByDescending { it.timestamp }

            trabalhoAdapter.atualizarLista(lista)

            if (lista.isEmpty()) {
                txtSemTrabalhos.visibility = View.VISIBLE
                rvTrabalhos.visibility = View.GONE
            } else {
                txtSemTrabalhos.visibility = View.GONE
                rvTrabalhos.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            txtSemTrabalhos.visibility = View.VISIBLE
            rvTrabalhos.visibility = View.GONE
        }
    }

    private fun configurarBotaoAcao(empresa: Empresa) {
        val uid = auth.currentUser?.uid ?: return

        val ehMembro = uid in empresa.membros
        val ehPendente = uid in empresa.pendentes
        val ehAdmin = uid in empresa.admins

        when {
            ehPendente -> {
                btnAcao.text = "CANCELAR PEDIDO"
                btnAcao.setBackgroundColor(0xFFFB8C00.toInt())
                btnAcao.setOnClickListener { cancelarPedido() }
            }
            ehMembro && ehAdmin -> {
                btnAcao.text = "GERENCIAR EMPRESA"
                btnAcao.setBackgroundColor(0xFF0D226B.toInt())
                btnAcao.setOnClickListener { abrirGerenciar() }
            }
            ehMembro -> {
                btnAcao.text = "ABRIR GRUPO GERAL"
                btnAcao.setBackgroundColor(0xFF0D226B.toInt())
                btnAcao.setOnClickListener { abrirGrupoGeral() }
            }
            else -> {
                btnAcao.text = "PEDIR PRA ENTRAR"
                btnAcao.setBackgroundColor(0xFF43A047.toInt())
                btnAcao.setOnClickListener { pedirParaEntrar() }
            }
        }
    }

    private fun pedirParaEntrar() {
        btnAcao.isEnabled = false
        lifecycleScope.launch {
            val ok = empresaRepository.pedirParaEntrar(empresaId)
            btnAcao.isEnabled = true

            if (ok) {
                Toast.makeText(
                    this@DetalhesEmpresaActivity,
                    "Pedido enviado! Aguarde aprovação.",
                    Toast.LENGTH_LONG
                ).show()
                carregarTudo()
            } else {
                Toast.makeText(
                    this@DetalhesEmpresaActivity,
                    "Erro ao pedir",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun cancelarPedido() {
        btnAcao.isEnabled = false
        lifecycleScope.launch {
            val ok = empresaRepository.cancelarPedido(empresaId)
            btnAcao.isEnabled = true

            if (ok) {
                Toast.makeText(
                    this@DetalhesEmpresaActivity,
                    "Pedido cancelado",
                    Toast.LENGTH_SHORT
                ).show()
                carregarTudo()
            } else {
                Toast.makeText(
                    this@DetalhesEmpresaActivity,
                    "Erro ao cancelar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun abrirGrupoGeral() {
        val grupoId = "empresa_${empresaId}_geral"
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", grupoId)
        startActivity(intent)
    }

    private fun abrirGerenciar() {
        val intent = Intent(this, GerenciarEmpresaActivity::class.java)
        intent.putExtra("empresaId", empresaId)
        startActivity(intent)
    }

    // ⭐ Upload da logo
    private fun uploadLogo(uri: Uri) {
        uploadingLogo = true
        Toast.makeText(this, "Enviando foto...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "empresas/logos")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        lifecycleScope.launch {
                            val ok = empresaRepository.atualizarLogoEmpresa(empresaId, secureUrl)
                            uploadingLogo = false

                            if (ok) {
                                Glide.with(this@DetalhesEmpresaActivity)
                                    .load(secureUrl).circleCrop().into(imgLogo)
                                imgLogo.imageTintList = null
                                imgLogo.setPadding(0, 0, 0, 0)
                                Toast.makeText(
                                    this@DetalhesEmpresaActivity,
                                    "Foto atualizada!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@DetalhesEmpresaActivity,
                                    "Erro ao salvar foto",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        uploadingLogo = false
                        Toast.makeText(
                            this@DetalhesEmpresaActivity,
                            "Erro: ${error?.description}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            uploadingLogo = false
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::empresaRepository.isInitialized) {
            carregarTudo()
        }
    }
}