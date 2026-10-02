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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

    private var minhasEmpresas: List<Empresa> = emptyList()
    private var empresaSelecionada: Empresa? = null

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
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                opcoes
            )
        )
    }

    private fun configurarDropdownNivel() {
        val opcoes = arrayOf("Júnior", "Pleno", "Sênior", "Especialista")
        edtNivel.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                opcoes
            )
        )
    }

    private fun carregarEmpresas() {
        lifecycleScope.launch {
            try {
                edtPublicarComo.setText("Pessoal (sem empresa)", false)

                minhasEmpresas = empresaRepository.listarMinhas()

                val nomes = mutableListOf<String>()
                nomes.add("Pessoal (sem empresa)")
                minhasEmpresas.forEach { nomes.add(it.nome) }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    nomes
                )
                edtPublicarComo.setAdapter(adapter)
                edtPublicarComo.setText("Pessoal (sem empresa)", false)
                empresaSelecionada = null
                atualizarInfoVisibilidade()

                edtPublicarComo.setOnItemClickListener { _, _, position, _ ->
                    empresaSelecionada =
                        if (position == 0) null else minhasEmpresas[position - 1]
                    atualizarInfoVisibilidade()
                }
            } catch (e: Exception) {
                edtPublicarComo.setText("Pessoal (sem empresa)", false)
                empresaSelecionada = null
                atualizarInfoVisibilidade()
            }
        }
    }

    // ⭐ Atualiza a caixa de info baseada no que está selecionado
    private fun atualizarInfoVisibilidade() {
        if (empresaSelecionada == null) {
            txtInfoVisibilidade.text =
                "Trabalho público: qualquer pessoa pode ver e se candidatar."
            imgIconeVisibilidade.setImageResource(android.R.drawable.ic_menu_info_details)
        } else {
            txtInfoVisibilidade.text =
                "Trabalho privado: visível apenas para membros de ${empresaSelecionada!!.nome}. Ninguém de fora pode se candidatar."
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
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }

        btnPublicar.isEnabled = false
        btnPublicar.text = "PUBLICANDO..."

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
                empresaNome = empresaSelecionada?.nome ?: ""
            )

            val sucesso = repository.publicar(trabalho)
            btnPublicar.isEnabled = true
            btnPublicar.text = "PUBLICAR TRABALHO"

            if (sucesso) {
                val msg = if (empresaSelecionada != null) {
                    "Trabalho privado publicado em ${empresaSelecionada!!.nome}!"
                } else {
                    "Trabalho publicado!"
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                limparFormulario()
            } else {
                Toast.makeText(requireContext(), "Erro ao publicar", Toast.LENGTH_LONG).show()
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
        edtPublicarComo.setText("Pessoal (sem empresa)", false)
        empresaSelecionada = null
        atualizarInfoVisibilidade()
    }
}