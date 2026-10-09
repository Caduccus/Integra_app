package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AdminChecker.isAdmin) {
            finish()
            return
        }

        setContentView(R.layout.activity_admin)

        findViewById<ImageView>(R.id.btnVoltarAdmin).setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.btnGerenciarUsuarios).setOnClickListener {
            startActivity(Intent(this, AdminUsuariosActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnGerenciarEmpresas).setOnClickListener {
            startActivity(Intent(this, AdminEmpresasActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnGerenciarTrabalhos).setOnClickListener {
            startActivity(Intent(this, AdminTrabalhosActivity::class.java))
        }

        carregarStats()
    }

    private fun carregarStats() {
        lifecycleScope.launch {
            try {
                coroutineScope {
                    val u = async { db.collection("usuarios").get().await().size() }
                    val e = async { db.collection("empresas").get().await().size() }
                    val t = async { db.collection("trabalhos").get().await().size() }
                    val c = async { db.collection("chats").get().await().size() }

                    findViewById<TextView>(R.id.txtStatUsuarios).text = u.await().toString()
                    findViewById<TextView>(R.id.txtStatEmpresas).text = e.await().toString()
                    findViewById<TextView>(R.id.txtStatTrabalhos).text = t.await().toString()
                    findViewById<TextView>(R.id.txtStatChats).text = c.await().toString()
                }
            } catch (_: Exception) { }
        }
    }
}