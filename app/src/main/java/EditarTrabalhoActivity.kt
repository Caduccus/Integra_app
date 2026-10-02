package com.example.plataformaremota

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.plataformaremota.data.repository.TrabalhoRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditarTrabalhoActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var edtTitulo: TextInputEditText
    private lateinit var edtDescricao: TextInputEditText
    private lateinit var edtCategoria: AutoCompleteTextView
    private lateinit var edtNivel: AutoCompleteTextView
    private lateinit var edtPrazo: TextInputEditText
    private lateinit var btnSalvar: MaterialButton

    private lateinit var repository: TrabalhoRepository
    private val db = FirebaseFirestore.getInstance()

    private var trabalhoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_trabalho)

        repository = TrabalhoRepository(this)

        trabalhoId = intent.getStringExtra("trabalhoId") ?: ""
        if (trabalhoId.isEmpty()) {
            Toast.makeText(this, "Trabalho não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnVoltar = findViewById(R.id.btnVoltarEditarTrabalho)
        edtTitulo = findViewById(R.id.edtTitulo)
        edtDescricao = findViewById(R.id.edtDescricao)
        edtCategoria = findViewById(R.id.edtCategoria)
        edtNivel = findViewById(R.id.edtNivel)
        edtPrazo = findViewById(R.id.edtPrazo)
        btnSalvar = findViewById(R.id.btnPublicar)

        // ⭐ Esconde o header interno do fragment_publish (título duplicado)
        findViewById<View>(R.id.layoutHeaderPublicar)?.visibility = View.GONE

        // ⭐ Esconde toda a seção "Visibilidade" (dono e empresa não mudam em edição)
        findViewById<View>(R.id.txtSecaoVisibilidade)?.visibility = View.GONE
        findViewById<View>(R.id.tilPublicarComo)?.visibility = View.GONE
        findViewById<View>(R.id.layoutInfoVisibilidade)?.visibility = View.GONE

        configurarDropdowns()

        btnSalvar.text = "SALVAR ALTERAÇÕES"
        btnSalvar.setOnClickListener { salvar() }
        btnVoltar.setOnClickListener { finish() }

        carregarTrabalho()

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun configurarDropdowns() {
        edtCategoria.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                arrayOf(
                    "Tecnologia", "Design", "Marketing", "Vendas", "Suporte",
                    "Educação", "Saúde", "Finanças", "Recursos Humanos",
                    "Administrativo", "Engenharia", "Outros"
                )
            )
        )
        edtNivel.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                arrayOf("Júnior", "Pleno", "Sênior", "Especialista")
            )
        )
    }

    private fun carregarTrabalho() {
        lifecycleScope.launch {
            try {
                val doc = db.collection("trabalhos").document(trabalhoId).get().await()

                if (!doc.exists()) {
                    Toast.makeText(this@EditarTrabalhoActivity, "Trabalho não existe", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                edtTitulo.setText(doc.getString("titulo") ?: "")
                edtDescricao.setText(doc.getString("descricao") ?: "")
                edtCategoria.setText(doc.getString("categoria") ?: "", false)
                edtNivel.setText(doc.getString("nivel") ?: "", false)
                edtPrazo.setText(doc.getString("prazo") ?: "")
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditarTrabalhoActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun salvar() {
        val titulo = edtTitulo.text.toString().trim()
        val descricao = edtDescricao.text.toString().trim()
        val categoria = edtCategoria.text.toString().trim()
        val nivel = edtNivel.text.toString().trim()
        val prazo = edtPrazo.text.toString().trim()

        if (titulo.isEmpty() || descricao.isEmpty() || categoria.isEmpty() ||
            nivel.isEmpty() || prazo.isEmpty()
        ) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        btnSalvar.isEnabled = false
        btnSalvar.text = "SALVANDO..."

        lifecycleScope.launch {
            val ok = repository.atualizar(
                trabalhoId,
                mapOf(
                    "titulo" to titulo,
                    "descricao" to descricao,
                    "categoria" to categoria,
                    "nivel" to nivel,
                    "prazo" to prazo
                )
            )

            btnSalvar.isEnabled = true
            btnSalvar.text = "SALVAR ALTERAÇÕES"

            if (ok) {
                Toast.makeText(
                    this@EditarTrabalhoActivity,
                    "Trabalho atualizado!",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(
                    this@EditarTrabalhoActivity,
                    "Erro ao salvar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}