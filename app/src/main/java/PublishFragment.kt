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
import com.example.plataformaremota.data.database.AppDatabase
import com.example.plataformaremota.data.entity.Trabalho
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class PublishFragment : Fragment() {

    private lateinit var edtTitulo: TextInputEditText
    private lateinit var edtDescricao: TextInputEditText
    private lateinit var edtCategoria: TextInputEditText
    private lateinit var edtTipoContrato: AutoCompleteTextView
    private lateinit var edtNivel: AutoCompleteTextView
    private lateinit var edtPrazo: TextInputEditText
    private lateinit var btnPublicar: MaterialButton

    private var usuarioId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_publish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtTitulo = view.findViewById(R.id.edtTitulo)
        edtDescricao = view.findViewById(R.id.edtDescricao)
        edtCategoria = view.findViewById(R.id.edtCategoria)
        edtTipoContrato = view.findViewById(R.id.edtTipoContrato)
        edtNivel = view.findViewById(R.id.edtNivel)
        edtPrazo = view.findViewById(R.id.edtPrazo)
        btnPublicar = view.findViewById(R.id.btnPublicar)

        // Recebe o usuarioId via arguments
        usuarioId = arguments?.getInt("usuarioId", 0) ?: 0

        configurarDropdowns()

        btnPublicar.setOnClickListener {
            publicarTrabalho()
        }
    }

    private fun configurarDropdowns() {

        val opcoesContrato = arrayOf(
            "CLT", "PJ", "Freelance", "Estágio", "Temporário"
        )

        edtTipoContrato.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                opcoesContrato
            )
        )

        val opcoesNivel = arrayOf(
            "Júnior", "Pleno", "Sênior", "Especialista"
        )

        edtNivel.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                opcoesNivel
            )
        )
    }

    private fun publicarTrabalho() {

        val titulo = edtTitulo.text.toString().trim()
        val descricao = edtDescricao.text.toString().trim()
        val categoria = edtCategoria.text.toString().trim()
        val tipoContrato = edtTipoContrato.text.toString().trim()
        val nivel = edtNivel.text.toString().trim()
        val prazo = edtPrazo.text.toString().trim()

        if (
            titulo.isEmpty() ||
            descricao.isEmpty() ||
            categoria.isEmpty() ||
            tipoContrato.isEmpty() ||
            nivel.isEmpty() ||
            prazo.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (usuarioId == 0) {
            Toast.makeText(requireContext(), "Usuário não identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {

            val database = AppDatabase.getDatabase(requireContext())

            val trabalho = Trabalho(
                titulo = titulo,
                descricao = descricao,
                categoria = categoria,
                prazo = prazo,
                tipoContrato = tipoContrato,
                nivel = nivel,
                criadorId = usuarioId
            )

            database.trabalhoDao().inserir(trabalho)

            // Volta pra Home e limpa o formulário
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Trabalho publicado!",
                    Toast.LENGTH_SHORT
                ).show()
                limparFormulario()
            }
        }
    }

    private fun limparFormulario() {
        edtTitulo.text?.clear()
        edtDescricao.text?.clear()
        edtCategoria.text?.clear()
        edtTipoContrato.text?.clear()
        edtNivel.text?.clear()
        edtPrazo.text?.clear()
    }
}