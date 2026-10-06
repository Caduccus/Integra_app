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

        val btnTema: ImageView? = findViewById(R.id.btnTemaLogin)
        btnTema?.setOnClickListener { abrirDialogTemas() }

        // ⭐ Esqueci minha senha
        val txtEsqueci: View? = findViewById(R.id.txtEsqueciSenha)
        txtEsqueci?.setOnClickListener { abrirDialogEsqueciSenha() }

        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null) {
            irParaHome(usuarioAtual.uid)
            return
        }

        btnEntrar.setOnClickListener { realizarLogin() }
        btnCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    // ⭐ NOVO
    private fun abrirDialogEsqueciSenha() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_esqueci_senha, null)
        val edtEmailReset = dialogView.findViewById<EditText>(R.id.edtEmailReset)

        // Pré-preenche com o que o usuário já digitou
        val emailDigitado = edtEmail.text.toString().trim()
        if (emailDigitado.isNotEmpty()) {
            edtEmailReset.setText(emailDigitado)
        }

        AlertDialog.Builder(this)
            .setTitle("Recuperar senha")
            .setMessage("Informe o e-mail cadastrado. Vamos enviar um link para você redefinir a senha.")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val email = edtEmailReset.text.toString().trim()

                if (email.isEmpty() || !email.contains("@")) {
                    Toast.makeText(this, "Digite um e-mail válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                enviarEmailReset(email)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarEmailReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AlertDialog.Builder(this)
                        .setTitle("E-mail enviado ✅")
                        .setMessage(
                            "Enviamos um link de recuperação para:\n\n" +
                                    "$email\n\n" +
                                    "Verifique sua caixa de entrada e a pasta de spam."
                        )
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val msg = task.exception?.message ?: "Erro desconhecido"
                    Toast.makeText(
                        this,
                        "Não foi possível enviar: $msg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun abrirDialogTemas() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_temas, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val temas = mapOf(
            R.id.temaPadrao to ThemeManager.TEMA_PADRAO,
            R.id.temaVermelho to ThemeManager.TEMA_VERMELHO,
            R.id.temaAzul to ThemeManager.TEMA_AZUL,
            R.id.temaVerde to ThemeManager.TEMA_VERDE,
            R.id.temaPreto to ThemeManager.TEMA_PRETO,
            R.id.temaBranco to ThemeManager.TEMA_BRANCO,
            R.id.temaAmarelo to ThemeManager.TEMA_AMARELO
        )

        for ((id, tema) in temas) {
            dialogView.findViewById<View>(id).setOnClickListener {
                ThemeManager.setTema(this, tema)
                dialog.dismiss()
                recreate()
            }
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