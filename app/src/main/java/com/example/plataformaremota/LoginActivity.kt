package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : BaseActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: Button
    private lateinit var btnCadastrar: Button
    private lateinit var btnGoogle: MaterialButton

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_GOOGLE_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        edtEmail = findViewById(R.id.edtEmail)
        edtSenha = findViewById(R.id.edtSenha)
        btnEntrar = findViewById(R.id.btnEntrar)
        btnCadastrar = findViewById(R.id.btnCadastrar)
        btnGoogle = findViewById(R.id.btnGoogle)

        val btnTema: ImageView? = findViewById(R.id.btnTemaLogin)
        btnTema?.setOnClickListener { abrirDialogTemas() }

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
        btnGoogle.setOnClickListener { fazerLoginGoogle() }

        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun fazerLoginGoogle() {
        btnGoogle.isEnabled = false
        btnGoogle.text = getString(R.string.login_btn_google_carregando)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthComGoogle(idToken)
                } else {
                    resetarBotaoGoogle()
                    Toast.makeText(this, getString(R.string.login_erro_token), Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("GOOGLE", "Erro sign-in: ${e.statusCode} - ${e.message}")
                resetarBotaoGoogle()
                Toast.makeText(
                    this,
                    "${getString(R.string.login_erro_google)}: ${e.statusCode}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun firebaseAuthComGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        verificarPerfil(
                            user.uid,
                            user.displayName ?: "",
                            user.email ?: "",
                            user.photoUrl?.toString() ?: ""
                        )
                    } else {
                        resetarBotaoGoogle()
                    }
                } else {
                    resetarBotaoGoogle()
                    Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verificarPerfil(uid: String, nome: String, email: String, fotoUrl: String) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()

                if (doc.exists()) {
                    irParaHome(uid)
                } else {
                    val intent = Intent(this@LoginActivity, CadastroActivity::class.java)
                    intent.putExtra("modoGoogle", true)
                    intent.putExtra("uid", uid)
                    intent.putExtra("nome", nome)
                    intent.putExtra("email", email)
                    intent.putExtra("fotoUrl", fotoUrl)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                resetarBotaoGoogle()
                Toast.makeText(this@LoginActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resetarBotaoGoogle() {
        btnGoogle.isEnabled = true
        btnGoogle.text = getString(R.string.login_btn_google)
    }

    private fun abrirDialogEsqueciSenha() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_esqueci_senha, null)
        val edtEmailReset = dialogView.findViewById<EditText>(R.id.edtEmailReset)

        val emailDigitado = edtEmail.text.toString().trim()
        if (emailDigitado.isNotEmpty()) edtEmailReset.setText(emailDigitado)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recuperar_titulo))
            .setMessage(getString(R.string.recuperar_msg))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.recuperar_btn_enviar)) { _, _ ->
                val email = edtEmailReset.text.toString().trim()
                if (email.isEmpty() || !email.contains("@")) {
                    Toast.makeText(this, getString(R.string.recuperar_email_invalido), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                enviarEmailReset(email)
            }
            .setNegativeButton(getString(R.string.recuperar_btn_cancelar), null)
            .show()
    }

    private fun enviarEmailReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.recuperar_email_enviado_titulo))
                        .setMessage(getString(R.string.recuperar_email_enviado_msg, email))
                        .setPositiveButton(getString(R.string.recuperar_ok), null)
                        .show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.recuperar_erro, task.exception?.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
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
            Toast.makeText(this, getString(R.string.login_erro_campos_vazios), Toast.LENGTH_SHORT).show()
            return
        }

        btnEntrar.isEnabled = false
        btnEntrar.text = getString(R.string.login_btn_entrando)

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                btnEntrar.isEnabled = true
                btnEntrar.text = getString(R.string.login_btn_entrar)

                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) irParaHome(uid)
                } else {
                    Toast.makeText(this, getString(R.string.login_erro_credenciais), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun irParaHome(uid: String) {
        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        intent.putExtra("usuarioId", uid)
        intent.putExtra("nomeUsuario", "")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}