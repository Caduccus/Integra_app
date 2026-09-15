package com.example.plataformaremota

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.plataformaremota.data.database.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity2 : AppCompatActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var txtTituloDetalhe: TextView
    private lateinit var txtDescricaoDetalhe: TextView
    private lateinit var txtCategoriaDetalhe: TextView
    private lateinit var txtTipoContratoDetalhe: TextView
    private lateinit var txtNivelDetalhe: TextView
    private lateinit var txtPrazoDetalhe: TextView

    private lateinit var btnExcluir: MaterialButton
    private lateinit var btnEditar: MaterialButton
    private lateinit var btnChat: MaterialButton

    private var trabalhoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main2)

        btnVoltar = findViewById(R.id.btnVoltar)
        txtTituloDetalhe = findViewById(R.id.txtTituloDetalhe)
        txtDescricaoDetalhe = findViewById(R.id.txtDescricaoDetalhe)
        txtCategoriaDetalhe = findViewById(R.id.txtCategoriaDetalhe)
        txtTipoContratoDetalhe = findViewById(R.id.txtTipoContratoDetalhe)
        txtNivelDetalhe = findViewById(R.id.txtNivelDetalhe)
        txtPrazoDetalhe = findViewById(R.id.txtPrazoDetalhe)

        btnExcluir = findViewById(R.id.btnExcluir)
        btnEditar = findViewById(R.id.btnEditar)
        btnChat = findViewById(R.id.btnChat)

        trabalhoId = intent.getIntExtra("trabalhoId", 0)

        btnVoltar.setOnClickListener {
            finish()
        }

        btnExcluir.setOnClickListener {
            Toast.makeText(this, "Excluir em breve", Toast.LENGTH_SHORT).show()
        }

        btnEditar.setOnClickListener {
            Toast.makeText(this, "Editar em breve", Toast.LENGTH_SHORT).show()
        }

        btnChat.setOnClickListener {
            Toast.makeText(this, "Chat em breve", Toast.LENGTH_SHORT).show()
        }

        carregarTrabalho()
    }

    private fun carregarTrabalho() {

        if (trabalhoId == 0) {
            Toast.makeText(this, "Trabalho não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {

            val database = AppDatabase.getDatabase(this@MainActivity2)

            val trabalho = database.trabalhoDao().buscarPorId(trabalhoId)

            if (trabalho == null) {
                Toast.makeText(
                    this@MainActivity2,
                    "Trabalho não encontrado",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            txtTituloDetalhe.text = trabalho.titulo
            txtDescricaoDetalhe.text = trabalho.descricao
            txtCategoriaDetalhe.text = trabalho.categoria
            txtTipoContratoDetalhe.text = trabalho.tipoContrato
            txtNivelDetalhe.text = trabalho.nivel
            txtPrazoDetalhe.text = trabalho.prazo
        }
    }
}