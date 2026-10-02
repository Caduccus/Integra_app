package com.example.plataformaremota

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.card.MaterialCardView
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
    private lateinit var cardAvatarCadastro: MaterialCardView
    private lateinit var imgFotoCadastro: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // ⭐ Foto escolhida pelo usuário (só usada no final)
    private var fotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri
            imgFotoCadastro.setImageURI(uri)
            imgFotoCadastro.imageTintList = null
            imgFotoCadastro.scaleType = ImageView.ScaleType.CENTER_CROP
            imgFotoCadastro.setPadding(0, 0, 0, 0)
        }
    }

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
        cardAvatarCadastro = findViewById(R.id.cardAvatarCadastro)
        imgFotoCadastro = findViewById(R.id.imgFotoCadastro)

        btnVoltarCadastro.setOnClickListener { finish() }
        btnSalvar.setOnClickListener { cadastrarUsuario() }

        // ⭐ Abre a galeria
        cardAvatarCadastro.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun cadastrarUsuario() {
        val nome = edtNome.text.toString().trim()
        val username = edtUsername.text.toString().trim().lowercase().replace(" ", "")
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString().trim()
        val profissao = edtProfissao.text.toString().trim()
        val bio = edtBio.text.toString().trim()

        if (nome.isEmpty() || username.isEmpty() || email.isEmpty() || senha.isEmpty() || profissao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 3) {
            Toast.makeText(this, "Username precisa ter no mínimo 3 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha precisa ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        btnSalvar.isEnabled = false
        btnSalvar.text = "CADASTRANDO..."

        db.collection("usuarios").whereEqualTo("username", username).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    Toast.makeText(this, "Este username já está em uso", Toast.LENGTH_LONG).show()
                    btnSalvar.isEnabled = true
                    btnSalvar.text = "CADASTRAR"
                    return@addOnSuccessListener
                }
                criarConta(username, nome, email, senha, profissao, bio)
            }
            .addOnFailureListener {
                criarConta(username, nome, email, senha, profissao, bio)
            }
    }

    private fun criarConta(
        username: String, nome: String, email: String,
        senha: String, profissao: String, bio: String
    ) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        salvarDadosNoFirestore(uid, nome, username, email, profissao, bio)
                    }
                } else {
                    btnSalvar.isEnabled = true
                    btnSalvar.text = "CADASTRAR"
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
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(uid)
            .set(usuario)
            .addOnSuccessListener {
                // ⭐ Só faz upload se o usuário escolheu foto
                if (fotoUri != null) {
                    uploadFoto(uid)
                } else {
                    finalizarCadastro()
                }
            }
            .addOnFailureListener { e ->
                btnSalvar.isEnabled = true
                btnSalvar.text = "CADASTRAR"
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ⭐ Upload pro Cloudinary + update no Firestore
    private fun uploadFoto(uid: String) {
        btnSalvar.text = "ENVIANDO FOTO..."

        try {
            MediaManager.get().upload(fotoUri!!)
                .unsigned("fotos_perfil")
                .option("folder", "perfis")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String

                        if (url.isNullOrEmpty()) {
                            finalizarCadastro()
                            return
                        }

                        val secureUrl = url.replace("http://", "https://")

                        db.collection("usuarios").document(uid)
                            .update("fotoUrl", secureUrl)
                            .addOnCompleteListener { finalizarCadastro() }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Toast.makeText(
                            this@CadastroActivity,
                            "Erro ao enviar foto: ${error?.description}",
                            Toast.LENGTH_LONG
                        ).show()
                        finalizarCadastro()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                })
                .dispatch()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            finalizarCadastro()
        }
    }

    private fun finalizarCadastro() {
        Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}