package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : BaseActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var edtProfissao: EditText
    private lateinit var edtBio: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnVoltarCadastro: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var modoGoogle: Boolean = false
    private var uidGoogle: String = ""
    private var emailGoogle: String = ""
    private var fotoGoogle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtNome = findViewById(R.id.edtNome)
        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtSenha = findViewById(R.id.edtSenha)
        edtProfissao = findViewById(R.id.edtProfissao)
        edtBio = findViewById(R.id.edtBio)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnVoltarCadastro = findViewById(R.id.btnVoltarCadastro)

        modoGoogle = intent.getBooleanExtra("modoGoogle", false)

        if (modoGoogle) {
            uidGoogle = intent.getStringExtra("uid") ?: ""
            val nomeGoogle = intent.getStringExtra("nome") ?: ""
            emailGoogle = intent.getStringExtra("email") ?: ""
            fotoGoogle = intent.getStringExtra("fotoUrl") ?: ""

            edtNome.setText(nomeGoogle)
            edtEmail.setText(emailGoogle)

            edtEmail.visibility = View.GONE
            edtSenha.visibility = View.GONE

            btnSalvar.text = "FINALIZAR CADASTRO"
        }

        btnVoltarCadastro.setOnClickListener {
            if (modoGoogle) auth.signOut()
            finish()
        }

        btnSalvar.setOnClickListener { cadastrarUsuario() }
    }

    private fun cadastrarUsuario() {
        val nome = edtNome.text.toString().trim()
        val username = edtUsername.text.toString().trim().lowercase().replace(" ", "")
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()
        val profissao = edtProfissao.text.toString().trim()
        val bio = edtBio.text.toString().trim()

        if (nome.isEmpty() || username.isEmpty() || profissao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        if (!modoGoogle && (email.isEmpty() || senha.isEmpty())) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 3) {
            Toast.makeText(this, "Username precisa ter no mínimo 3 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (!modoGoogle && senha.length < 6) {
            Toast.makeText(this, "A senha precisa ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios").whereEqualTo("username", username).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    Toast.makeText(this, "Este username já está em uso", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (modoGoogle) {
                    salvarPerfilGoogle(nome, username, profissao, bio)
                } else {
                    criarConta(username, nome, email, senha, profissao, bio)
                }
            }
            .addOnFailureListener {
                if (modoGoogle) {
                    salvarPerfilGoogle(nome, username, profissao, bio)
                } else {
                    criarConta(username, nome, email, senha, profissao, bio)
                }
            }
    }

    private fun salvarPerfilGoogle(nome: String, username: String, profissao: String, bio: String) {
        val usuario = hashMapOf(
            "nome" to nome,
            "username" to username,
            "email" to emailGoogle,
            "profissao" to profissao,
            "bio" to bio,
            "fotoUrl" to fotoGoogle,
            "status" to "online",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(uidGoogle).set(usuario)
            .addOnSuccessListener {
                Toast.makeText(this, "Cadastro realizado!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("usuarioId", uidGoogle)
                intent.putExtra("nomeUsuario", nome)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun criarConta(username: String, nome: String, email: String, senha: String, profissao: String, bio: String) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        salvarDadosNoFirestore(uid, nome, username, email, profissao, bio)
                    }
                } else {
                    Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun salvarDadosNoFirestore(
        uid: String, nome: String, username: String,
        email: String, profissao: String, bio: String
    ) {
        val usuario = hashMapOf(
            "nome" to nome,
            "username" to username,
            "email" to email,
            "profissao" to profissao,
            "bio" to bio,
            "fotoUrl" to "",
            "status" to "online",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(uid)
            .set(usuario)
            .addOnSuccessListener {
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}