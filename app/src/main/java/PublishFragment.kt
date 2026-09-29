package com.example.plataformaremota

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.plataformaremota.data.entity.Trabalho
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
    private lateinit var edtCategoria: AutoCompleteTextView   // ⭐ MUDOU
    private lateinit var edtNivel: AutoCompleteTextView
    private lateinit var edtPrazo: TextInputEditText
    private lateinit var btnPublicar: MaterialButton

    private lateinit var repository: TrabalhoRepository
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

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
        btnPublicar = view.findViewById(R.id.btnPublicar)

        repository = TrabalhoRepository(requireContext())
        usuarioId = arguments?.getString("usuarioId") ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        configurarDropdowns()
        btnPublicar.setOnClickListener { publicarTrabalho() }

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
    }

    private fun configurarDropdowns() {
        // ⭐ CATEGORIA (dropdown com opções)
        val opcoesCategoria = arrayOf(
            "Tecnologia",
            "Design",
            "Marketing",
            "Vendas",
            "Suporte",
            "Educação",
            "Saúde",
            "Finanças",
            "Recursos Humanos",
            "Administrativo",
            "Engenharia",
            "Outros"
        )
        edtCategoria.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opcoesCategoria)
        )

        // NÍVEL (dropdown com texto livre)
        val opcoesNivel = arrayOf("Júnior", "Pleno", "Sênior", "Especialista")
        edtNivel.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opcoesNivel)
        )

        // Mostra a lista assim que clica
        edtCategoria.setOnClickListener {
            edtCategoria.showDropDown()
        }
        edtNivel.setOnClickListener {
            edtNivel.showDropDown()
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
                tipoContrato = "",   // ⭐ Vazio (campo removido)
                nivel = nivel,
                criadorId = uid,
                nomeCriador = nomeCriador,
                timestamp = System.currentTimeMillis()
            )

            val sucesso = repository.publicar(trabalho)
            btnPublicar.isEnabled = true
            btnPublicar.text = "PUBLICAR TRABALHO"

            if (sucesso) {
                Toast.makeText(requireContext(), "Trabalho publicado!", Toast.LENGTH_SHORT).show()
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
    }
}