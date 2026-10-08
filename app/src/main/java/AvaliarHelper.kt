package com.example.plataformaremota

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.plataformaremota.data.entity.Avaliacao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AvaliarHelper {

    private var notaSelecionada = 5

    fun abrirDialog(
        context: Context,
        alvoId: String,
        alvoTipo: String,
        alvoNome: String,
        trabalhoId: String = "",
        trabalhoTitulo: String = "",
        onSucesso: () -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // ⭐ Não pode avaliar a si mesmo
        if (alvoTipo == "usuario" && alvoId == uid) {
            Toast.makeText(context, "Você não pode avaliar a si mesmo", Toast.LENGTH_SHORT).show()
            return
        }

        // ⭐ Não pode avaliar a própria empresa
        if (alvoTipo == "empresa") {
            db.collection("empresas").document(alvoId).get()
                .addOnSuccessListener { doc ->
                    val criadorId = doc.getString("criadorId") ?: ""
                    if (criadorId == uid) {
                        Toast.makeText(
                            context,
                            "Você não pode avaliar sua própria empresa",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        mostrarDialog(context, alvoId, alvoTipo, alvoNome, trabalhoId, trabalhoTitulo, onSucesso)
                    }
                }
                .addOnFailureListener {
                    mostrarDialog(context, alvoId, alvoTipo, alvoNome, trabalhoId, trabalhoTitulo, onSucesso)
                }
            return
        }

        mostrarDialog(context, alvoId, alvoTipo, alvoNome, trabalhoId, trabalhoTitulo, onSucesso)
    }

    private fun mostrarDialog(
        context: Context,
        alvoId: String,
        alvoTipo: String,
        alvoNome: String,
        trabalhoId: String,
        trabalhoTitulo: String,
        onSucesso: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_avaliar, null)

        val txtNomeAlvo = dialogView.findViewById<TextView>(R.id.txtNomeAlvo)
        val linhaEstrelas = dialogView.findViewById<LinearLayout>(R.id.linhaEstrelas)
        val edtComentario = dialogView.findViewById<android.widget.EditText>(
            R.id.edtComentarioAvaliacao
        )

        txtNomeAlvo.text = "Avaliar $alvoNome"
        notaSelecionada = 5

        val estrelas = mutableListOf<ImageView>()
        for (i in 1..5) {
            val iv = ImageView(context)
            val size = (40 * context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = (4 * context.resources.displayMetrics.density).toInt()
            iv.layoutParams = params
            iv.setPadding(4, 4, 4, 4)
            iv.setOnClickListener {
                notaSelecionada = i
                atualizarEstrelas(estrelas, i)
            }
            linhaEstrelas.addView(iv)
            estrelas.add(iv)
        }
        atualizarEstrelas(estrelas, 5)

        AlertDialog.Builder(context)
            .setTitle("Deixar avaliação")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val comentario = edtComentario.text.toString().trim()

                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val autorNome = doc.getString("nome") ?: "Usuário"

                        val avaliacao = Avaliacao(
                            autorId = uid,
                            autorNome = autorNome,
                            alvoId = alvoId,
                            alvoTipo = alvoTipo,
                            alvoNome = alvoNome,
                            trabalhoId = trabalhoId,
                            trabalhoTitulo = trabalhoTitulo,
                            estrelas = notaSelecionada,
                            comentario = comentario,
                            timestamp = System.currentTimeMillis()
                        )

                        db.collection("avaliacoes").add(avaliacao)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Avaliação enviada!", Toast.LENGTH_SHORT).show()
                                onSucesso()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarEstrelas(estrelas: List<ImageView>, nota: Int) {
        estrelas.forEachIndexed { i, iv ->
            if (i < nota) {
                iv.setImageResource(R.drawable.ic_star_filled)
                iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFC107"))
            } else {
                iv.setImageResource(R.drawable.ic_star_outline)
                iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
            }
        }
    }
}