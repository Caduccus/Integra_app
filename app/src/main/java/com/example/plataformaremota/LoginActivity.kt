package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

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

        btnEntrar.setOnClickListener {
            realizarLogin()
        }

        btnCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun realizarLogin() {
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        buscarNomeEIrParaHome(uid)
                    } else {
                        Toast.makeText(this, "Erro: usuário não encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "E-mail ou senha incorretos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun buscarNomeEIrParaHome(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                val nome = document.getString("nome") ?: "Usuário"

                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("usuarioId", uid)
                intent.putExtra("nomeUsuario", nome)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                // Se não achar o nome, ainda loga (só fica sem nome)
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("usuarioId", uid)
                intent.putExtra("nomeUsuario", "Usuário")
                startActivity(intent)
                finish()
            }
    }
}