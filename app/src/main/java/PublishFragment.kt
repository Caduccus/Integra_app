package com.example.plataformaremota

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.data.entity.Empresa
import com.example.plataformaremota.data.entity.Trabalho
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.example.plataformaremota.data.repository.TrabalhoRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PublishFragment : Fragment() {

    private lateinit var edtTitulo: TextInputEditText
    private lateinit var edtDescricao: TextInputEditText
    private lateinit var edtCategoria: AutoCompleteTextView
    private lateinit var edtNivel: AutoCompleteTextView
    private lateinit var edtPrazo: TextInputEditText
    private lateinit var edtPublicarComo: AutoCompleteTextView
    private lateinit var btnAddBanner: TextView // ⭐ NOVO
    private lateinit var btnPublicar: MaterialButton
    private lateinit var layoutInfoVisibilidade: View
    private lateinit var imgIconeVisibilidade: ImageView
    private lateinit var txtInfoVisibilidade: TextView

    private lateinit var repository: TrabalhoRepository
    private lateinit var empresaRepository: EmpresaRepository

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""
    private var bannerUrlUploaded: String = "" // ⭐ NOVO

    private var minhasEmpresas: List<Empresa> = emptyList()
    private var empresaSelecionada: Empresa? = null

    // ⭐ Launcher para escolher a imagem do Banner
    private val pickBannerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Toast.makeText(requireContext(), "Enviando banner...", Toast.LENGTH_SHORT).show()
            try {
                MediaManager.get().upload(uri)
                    .unsigned("fotos_perfil")
                    .option("folder", "banners/trabalhos")
                    .callback(object : UploadCallback {
                        override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                            val url = resultData?.get("secure_url") as? String
                                ?: resultData?.get("url") as? String
                                ?: return
                            bannerUrlUploaded = url.replace("http://", "https://")

                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Banner anexado!", Toast.LENGTH_SHORT).show()
                                btnAddBanner.text = "Banner adicionado ✅"
                            }
                        }
                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Erro no banner: ${error?.description}", Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                    }).dispatch()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_publish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtTitulo = view.findViewById(R.id.edtTitulo)
        edtDescricao = view.findViewById(R.id.edtDescricao)
        edtCategoria = view.findViewById(R.id.edtCategoria)
        edtNivel = view.findViewById(R.id.edtNivel)
        edtPrazo = view.findViewById(R.id.edtPrazo)
        edtPublicarComo = view.findViewById(R.id.edtPublicarComo)
        btnAddBanner = view.findViewById(R.id.btnAddBanner) // ⭐ NOVO
        btnPublicar = view.findViewById(R.id.btnPublicar)
        layoutInfoVisibilidade = view.findViewById(R.id.layoutInfoVisibilidade)
        imgIconeVisibilidade = view.findViewById(R.id.imgIconeVisibilidade)
        txtInfoVisibilidade = view.findViewById(R.id.txtInfoVisibilidade)

        repository = TrabalhoRepository(requireContext())
        empresaRepository = EmpresaRepository()

        usuarioId = arguments?.getString("usuarioId") ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        configurarDropdownCategoria()
        configurarDropdownNivel()
        carregarEmpresas()

        btnAddBanner.setOnClickListener {
            pickBannerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnPublicar.setOnClickListener { publicarTrabalho() }

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
    }

    private fun configurarDropdownCategoria() {
        val opcoes = arrayOf(
            "Tecnologia", "Design", "Marketing", "Vendas",
            "Suporte", "Educação", "Saúde", "Finanças",
            "Recursos Humanos", "Administrativo", "Engenharia", "Outros"
        )
        edtCategoria.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opcoes)
        )
    }

    private fun configurarDropdownNivel() {
        val opcoes = arrayOf("Júnior", "Pleno", "Sênior", "Especialista")
        edtNivel.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opcoes)
        )
    }

    private fun carregarEmpresas() {
        lifecycleScope.launch {
            try {
                edtPublicarComo.setText(getString(R.string.publicar_pessoal), false)
                minhasEmpresas = empresaRepository.listarMinhas()

                val nomes = mutableListOf<String>()
                nomes.add(getString(R.string.publicar_pessoal))
                minhasEmpresas.forEach { nomes.add(it.nome) }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    nomes
                )
                edtPublicarComo.setAdapter(adapter)
                edtPublicarComo.setText(getString(R.string.publicar_pessoal), false)
                empresaSelecionada = null
                atualizarInfoVisibilidade()

                edtPublicarComo.setOnItemClickListener { _, _, position, _ ->
                    empresaSelecionada = if (position == 0) null else minhasEmpresas[position - 1]
                    atualizarInfoVisibilidade()
                }
            } catch (e: Exception) {
                edtPublicarComo.setText(getString(R.string.publicar_pessoal), false)
                empresaSelecionada = null
                atualizarInfoVisibilidade()
            }
        }
    }

    private fun atualizarInfoVisibilidade() {
        if (empresaSelecionada == null) {
            txtInfoVisibilidade.text = getString(R.string.publicar_info_publico)
            imgIconeVisibilidade.setImageResource(android.R.drawable.ic_menu_info_details)
        } else {
            txtInfoVisibilidade.text = getString(R.string.publicar_info_privado, empresaSelecionada!!.nome)
            imgIconeVisibilidade.setImageResource(android.R.drawable.ic_lock_lock)
        }
    }

    private fun publicarTrabalho() {
        val titulo = edtTitulo.text.toString().trim()
        val descricao = edtDescricao.text.toString().trim()
        val categoria = edtCategoria.text.toString().trim()
        val nivel = edtNivel.text.toString().trim()
        val prazo = edtPrazo.text.toString().trim()

        if (titulo.isEmpty() || descricao.isEmpty() || categoria.isEmpty() ||
            nivel.isEmpty() || prazo.isEmpty()
        ) {
            Toast.makeText(requireContext(), getString(R.string.publicar_erro_preencher), Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.publicar_erro_usuario), Toast.LENGTH_SHORT).show()
            return
        }

        btnPublicar.isEnabled = false
        btnPublicar.text = getString(R.string.publicar_publicando)

        lifecycleScope.launch {
            val nomeCriador = buscarNomeUsuario(uid) ?: nomeUsuario

            val trabalho = Trabalho(
                titulo = titulo,
                descricao = descricao,
                categoria = categoria,
                prazo = prazo,
                tipoContrato = "",
                nivel = nivel,
                criadorId = uid,
                nomeCriador = nomeCriador,
                timestamp = System.currentTimeMillis(),
                empresaId = empresaSelecionada?.id ?: "",
                empresaNome = empresaSelecionada?.nome ?: "",
                bannerUrl = bannerUrlUploaded // ⭐ ADICIONADO AQUI
            )

            val sucesso = repository.publicar(trabalho)
            btnPublicar.isEnabled = true
            btnPublicar.text = getString(R.string.publicar_btn)

            if (sucesso) {
                val msg = if (empresaSelecionada != null) {
                    getString(R.string.publicar_sucesso_empresa, empresaSelecionada!!.nome)
                } else {
                    getString(R.string.publicar_sucesso)
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                limparFormulario()
            } else {
                Toast.makeText(requireContext(), getString(R.string.geral_erro, ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun buscarNomeUsuario(uid: String): String? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("nome")
        } catch (e: Exception) { null }
    }

    private fun limparFormulario() {
        edtTitulo.text?.clear()
        edtDescricao.text?.clear()
        edtCategoria.text?.clear()
        edtNivel.text?.clear()
        edtPrazo.text?.clear()
        edtPublicarComo.setText(getString(R.string.publicar_pessoal), false)
        empresaSelecionada = null
        bannerUrlUploaded = ""
        btnAddBanner.text = "Toque para adicionar um Banner (Opcional)"
        atualizarInfoVisibilidade()
    }
}