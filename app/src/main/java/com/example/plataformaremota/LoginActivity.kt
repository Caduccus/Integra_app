package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : BaseActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: Button
    private lateinit var btnCadastrar: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

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

        val txtEsqueci: View? = findViewById(R.id.txtEsqueciSenha)
        txtEsqueci?.setOnClickListener { abrirDialogEsqueciSenha() }

        // Se já tem usuário logado, vai direto
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

    private fun abrirDialogEsqueciSenha() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_esqueci_senha, null)
        val edtEmailReset = dialogView.findViewById<EditText>(R.id.edtEmailReset)

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
                    Toast.makeText(this, "Não foi possível enviar: $msg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun abrirDialogTemas() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_temas, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

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

        // ⭐ Timeout de 20s — evita ficar travado pra sempre
        timeoutRunnable = Runnable {
            if (!isFinishing) {
                resetarBotao()
                Toast.makeText(
                    this,
                    "Tempo esgotado. Verifique sua internet e tente novamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        handler.postDelayed(timeoutRunnable!!, 20000)

        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                Log.d("LOGIN", "✅ Login bem-sucedido")
                cancelarTimeout()
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    irParaHome(uid)
                } else {
                    resetarBotao()
                    Toast.makeText(this, "Erro: usuário não identificado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LOGIN", "❌ Falha no login: ${e.message}", e)
                cancelarTimeout()
                resetarBotao()
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun resetarBotao() {
        btnEntrar.isEnabled = true
        btnEntrar.text = "ENTRAR"
    }

    private fun cancelarTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    // ⭐ SEM await() — não depende do Firestore pra navegar
    private fun irParaHome(uid: String) {
        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        intent.putExtra("usuarioId", uid)
        intent.putExtra("nomeUsuario", "")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        cancelarTimeout()
        super.onDestroy()
    }
}