package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var edtProfissao: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnVoltarCadastro: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var cadastroFinalizado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtNome = findViewById(R.id.edtNome)
        edtEmail = findViewById(R.id.edtEmail)
        edtSenha = findViewById(R.id.edtSenha)
        edtProfissao = findViewById(R.id.edtProfissao)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnVoltarCadastro = findViewById(R.id.btnVoltarCadastro)

        btnVoltarCadastro.setOnClickListener { finish() }
        btnSalvar.setOnClickListener { cadastrarUsuario() }
    }

    private fun cadastrarUsuario() {
        val nome = edtNome.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()
        val profissao = edtProfissao.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || profissao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha precisa ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        cadastroFinalizado = false
        btnSalvar.isEnabled = false
        btnSalvar.text = "CADASTRANDO..."

        Log.d("CADASTRO", "=== Iniciando cadastro ===")
        Log.d("CADASTRO", "Email: $email")

        // TIMEOUT DE 20 SEGUNDOS
        Handler(Looper.getMainLooper()).postDelayed({
            if (!cadastroFinalizado) {
                cadastroFinalizado = true
                restaurarBotao()
                Log.e("CADASTRO", "⏰ TIMEOUT! Firestore não respondeu em 20s")
                Toast.makeText(
                    this,
                    "Firestore não respondeu. Verifica a internet do emulador!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 20000)

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                Log.d("CADASTRO", "Auth callback. Sucesso: ${task.isSuccessful}")

                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    Log.d("CADASTRO", "UID criado: $uid")

                    if (uid != null) {
                        salvarNoFirestore(uid, nome, email, profissao)
                    } else {
                        cadastroFinalizado = true
                        restaurarBotao()
                        Toast.makeText(this, "Erro: UID nulo", Toast.LENGTH_LONG).show()
                    }
                } else {
                    cadastroFinalizado = true
                    restaurarBotao()
                    Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun salvarNoFirestore(uid: String, nome: String, email: String, profissao: String) {
        Log.d("CADASTRO", "Salvando no Firestore...")

        val usuario = hashMapOf(
            "nome" to nome,
            "email" to email,
            "profissao" to profissao,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(uid)
            .set(usuario)
            .addOnSuccessListener {
                cadastroFinalizado = true
                Log.d("CADASTRO", "✅ Firestore salvou!")
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                auth.signOut()
                irParaLogin()
            }
            .addOnFailureListener { e ->
                cadastroFinalizado = true
                Log.e("CADASTRO", "❌ Firestore falhou: ${e.message}")
                restaurarBotao()
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun irParaLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun restaurarBotao() {
        btnSalvar.isEnabled = true
        btnSalvar.text = "CADASTRAR"
    }
}