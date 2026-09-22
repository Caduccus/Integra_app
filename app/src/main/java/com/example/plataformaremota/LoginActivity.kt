package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : BaseActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: Button
    private lateinit var btnCadastrar: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtEmail = findViewById(R.id.edtEmail)
        edtSenha = findViewById(R.id.edtSenha)
        btnEntrar = findViewById(R.id.btnEntrar)
        btnCadastrar = findViewById(R.id.btnCadastrar)

        // Botão de tema (canto superior direito)
        val btnTema: ImageView? = findViewById(R.id.btnTemaLogin)
        btnTema?.setOnClickListener { abrirDialogTemas() }

        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null) {
            irParaHome(usuarioAtual.uid)
            return
        }

        btnEntrar.setOnClickListener { realizarLogin() }
        btnCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun abrirDialogTemas() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_temas, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.temaPadrao).setOnClickListener {
            ThemeManager.setTema(this, ThemeManager.TEMA_PADRAO)
            dialog.dismiss()
            recreate()
        }
        dialogView.findViewById<View>(R.id.temaVermelho).setOnClickListener {
            ThemeManager.setTema(this, ThemeManager.TEMA_VERMELHO)
            dialog.dismiss()
            recreate()
        }
        dialogView.findViewById<View>(R.id.temaAzul).setOnClickListener {
            ThemeManager.setTema(this, ThemeManager.TEMA_AZUL)
            dialog.dismiss()
            recreate()
        }
        dialogView.findViewById<View>(R.id.temaVerde).setOnClickListener {
            ThemeManager.setTema(this, ThemeManager.TEMA_VERDE)
            dialog.dismiss()
            recreate()
        }

        dialog.show()
    }

    private fun realizarLogin() {
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        btnEntrar.isEnabled = false
        btnEntrar.text = "ENTRANDO..."

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                btnEntrar.isEnabled = true
                btnEntrar.text = "ENTRAR"

                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) irParaHome(uid)
                } else {
                    Toast.makeText(this, "E-mail ou senha incorretos", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun irParaHome(uid: String) {
        lifecycleScope.launch {
            val nome = try {
                val doc = db.collection("usuarios").document(uid).get().await()
                doc.getString("nome") ?: "Usuário"
            } catch (e: Exception) { "Usuário" }

            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
            intent.putExtra("usuarioId", uid)
            intent.putExtra("nomeUsuario", nome)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}