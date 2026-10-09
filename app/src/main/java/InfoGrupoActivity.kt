package com.example.plataformaremota

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.adapter.Membro
import com.example.plataformaremota.adapter.MembroGrupoAdapter
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InfoGrupoActivity : BaseActivity() {

    private lateinit var btnApagarGrupo: MaterialButton

    private var souCriador: Boolean = false
    private lateinit var btnVoltar: ImageView
    private lateinit var cardFotoGrupo: MaterialCardView
    private lateinit var badgeCamera: MaterialCardView
    private lateinit var imgFotoGrupo: ImageView
    private lateinit var txtNomeGrupo: TextView
    private lateinit var txtTotalMembros: TextView
    private lateinit var btnEditarNome: MaterialButton
    private lateinit var btnAdicionarMembros: MaterialButton
    private lateinit var btnSair: MaterialButton
    private lateinit var layoutAcoesAdmin: LinearLayout
    private lateinit var rvMembros: RecyclerView
    private lateinit var layoutLoading: View

    private lateinit var adapter: MembroGrupoAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId: String = ""
    private var chatAtual: Chat? = null
    private var souAdmin: Boolean = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uploadFotoGrupo(uri)
        }
    }

    private val adicionarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            carregarInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_grupo)

        repository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltarInfoGrupo)
        cardFotoGrupo = findViewById(R.id.cardFotoGrupo)
        badgeCamera = findViewById(R.id.badgeCameraGrupo)
        imgFotoGrupo = findViewById(R.id.imgFotoGrupo)
        txtNomeGrupo = findViewById(R.id.txtNomeGrupo)
        txtTotalMembros = findViewById(R.id.txtTotalMembros)
        btnEditarNome = findViewById(R.id.btnEditarNomeGrupo)
        btnAdicionarMembros = findViewById(R.id.btnAdicionarMembros)
        btnSair = findViewById(R.id.btnSairGrupo)
        btnApagarGrupo = findViewById(R.id.btnApagarGrupo)
        layoutAcoesAdmin = findViewById(R.id.layoutAcoesAdmin)
        rvMembros = findViewById(R.id.rvMembros)
        layoutLoading = findViewById(R.id.layoutLoadingMembros)

        chatId = intent.getStringExtra("chatId") ?: ""

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Erro ao abrir grupo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarRecyclerView()

        btnVoltar.setOnClickListener { finish() }
        btnSair.setOnClickListener { confirmarSaida() }
        btnEditarNome.setOnClickListener { abrirDialogNome() }
        btnAdicionarMembros.setOnClickListener { abrirAdicionarMembros() }
        btnApagarGrupo.setOnClickListener { confirmarApagarGrupo() }
        cardFotoGrupo.setOnClickListener { if (souAdmin) escolherFoto() }

        carregarInfo()
    }

    private fun configurarRecyclerView() {
        adapter = MembroGrupoAdapter(emptyList()) { membro ->
            val intent = Intent(this, PerfilUsuarioActivity::class.java)
            intent.putExtra("uidUsuario", membro.uid)
            intent.putExtra("chatId", chatId)
            startActivity(intent)
        }
        rvMembros.layoutManager = LinearLayoutManager(this)
        rvMembros.adapter = adapter
    }

    private fun carregarInfo() {
        val uidAtual = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                val chat = doc.toObject(Chat::class.java)

                if (chat == null) {
                    Toast.makeText(this@InfoGrupoActivity, "Grupo não encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                chatAtual = chat
                souAdmin = chat.admins.contains(uidAtual) || chat.criadorId == uidAtual
                souCriador = chat.criadorId == uidAtual

                txtNomeGrupo.text = chat.nome.ifEmpty { "Grupo" }
                txtTotalMembros.text = "${chat.participantes.size} membros"

                if (chat.fotoUrl.isNotEmpty()) {
                    Glide.with(this@InfoGrupoActivity)
                        .load(chat.fotoUrl).circleCrop().into(imgFotoGrupo)
                    imgFotoGrupo.imageTintList = null
                    imgFotoGrupo.setPadding(0, 0, 0, 0)
                }

                val ehGrupoEmpresa = chat.empresaId.isNotEmpty()

                badgeCamera.visibility =
                    if (souAdmin && !ehGrupoEmpresa) View.VISIBLE else View.GONE
                btnAdicionarMembros.visibility =
                    if (souAdmin && !ehGrupoEmpresa) View.VISIBLE else View.GONE
                btnApagarGrupo.visibility =
                    if (souCriador && !ehGrupoEmpresa) View.VISIBLE else View.GONE
                btnSair.visibility =
                    if (ehGrupoEmpresa) View.GONE else View.VISIBLE

                carregarMembros(chat)

            } catch (e: Exception) {
                Toast.makeText(this@InfoGrupoActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            layoutLoading.visibility = View.GONE
        }
    }

    private suspend fun carregarMembros(chat: Chat) {
        val membros = mutableListOf<Membro>()

        for (uid in chat.participantes) {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                membros.add(
                    Membro(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: "",
                        ehAdmin = chat.admins.contains(uid) || chat.criadorId == uid,
                        status = doc.getString("status") ?: ThemeManager.STATUS_ONLINE
                    )
                )
            } catch (_: Exception) { }
        }

        val ordenados = membros.sortedWith(
            compareByDescending<Membro> { it.ehAdmin }.thenBy { it.nome.lowercase() }
        )

        adapter.atualizarLista(ordenados)
        rvMembros.visibility = View.VISIBLE
    }

    private fun abrirAdicionarMembros() {
        val intent = Intent(this, AdicionarMembrosActivity::class.java)
        intent.putExtra("chatId", chatId)
        adicionarLauncher.launch(intent)
    }

    private fun abrirDialogNome() {
        val edt = EditText(this).apply {
            setText(chatAtual?.nome ?: "")
            setHint("Novo nome do grupo")
            setSelection(text.length)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar nome do grupo")
            .setView(edt)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = edt.text.toString().trim()
                if (novoNome.isNotEmpty()) {
                    lifecycleScope.launch {
                        val ok = repository.atualizarNomeGrupo(chatId, novoNome)
                        if (ok) {
                            txtNomeGrupo.text = novoNome
                            Toast.makeText(this@InfoGrupoActivity, "Nome atualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun escolherFoto() {
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun uploadFotoGrupo(uri: Uri) {
        Toast.makeText(this, "Enviando foto...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "grupos/$chatId")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        lifecycleScope.launch {
                            val ok = repository.atualizarFotoGrupo(chatId, secureUrl)
                            if (ok) {
                                Glide.with(this@InfoGrupoActivity)
                                    .load(secureUrl).circleCrop().into(imgFotoGrupo)
                                imgFotoGrupo.imageTintList = null
                                imgFotoGrupo.setPadding(0, 0, 0, 0)
                                Toast.makeText(this@InfoGrupoActivity, "Foto atualizada!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Toast.makeText(this@InfoGrupoActivity, "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmarSaida() {
        if (chatAtual?.empresaId?.isNotEmpty() == true) {
            Toast.makeText(
                this,
                "Você só pode sair se um admin da empresa te remover",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Sair do grupo")
            .setMessage("Tem certeza que quer sair do grupo?")
            .setPositiveButton("Sair") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.sairDoGrupo(chatId)
                    if (ok) {
                        Toast.makeText(this@InfoGrupoActivity, "Você saiu do grupo", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@InfoGrupoActivity, "Erro ao sair", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarApagarGrupo() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Apagar grupo")
            .setMessage(
                "Tem CERTEZA que quer apagar este grupo?\n\n" +
                        "Isso vai apagar:\n" +
                        "• O grupo\n" +
                        "• Todas as mensagens\n" +
                        "• Todos os membros serão removidos\n\n" +
                        "Essa ação NÃO pode ser desfeita!"
            )
            .setPositiveButton("APAGAR") { _, _ ->
                HapticHelper.destrutiva(this)
                apagarGrupo()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun apagarGrupo() {
        Toast.makeText(this, "Apagando grupo...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val ok = repository.apagarGrupo(chatId)

            if (ok) {
                chatAtual?.participantes?.forEach { uid ->
                    if (uid != auth.currentUser?.uid) {
                        NotificacaoHelper.enviar(
                            uidDestino = uid,
                            titulo = "Grupo apagado",
                            mensagem = "O grupo '${chatAtual?.nome}' foi apagado pelo criador."
                        )
                    }
                }

                Toast.makeText(this@InfoGrupoActivity, "Grupo apagado!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this@InfoGrupoActivity, "Erro ao apagar grupo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            carregarInfo()
        }
    }
}