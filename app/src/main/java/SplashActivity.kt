package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val cardLogo = findViewById<View>(R.id.cardSplashLogo)
        val txtNome = findViewById<TextView>(R.id.txtSplashNome)
        val txtSlogan = findViewById<TextView>(R.id.txtSplashSlogan)

        cardLogo.alpha = 0f
        cardLogo.scaleX = 0.6f
        cardLogo.scaleY = 0.6f
        txtNome.alpha = 0f
        txtSlogan.alpha = 0f

        cardLogo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700).setStartDelay(150).start()

        txtNome.animate().alpha(1f).setDuration(500).setStartDelay(600).start()
        txtSlogan.animate().alpha(1f).setDuration(500).setStartDelay(850).start()

        // ⭐ Depois de 2s, decide — sem depender de Firestore
        lifecycleScope.launch {
            delay(2000)
            decidirDestino()
        }

        // ⭐ Rede de segurança: se por algum motivo ficar travado 5s, força
        handler.postDelayed({
            if (!isFinishing) {
                Log.w("SPLASH", "Timeout — forçando destino")
                decidirDestino()
            }
        }, 5000)
    }

    private fun decidirDestino() {
        if (isFinishing || isDestroyed) return

        val user = auth.currentUser
        if (user == null) {
            irParaLogin()
        } else {
            irParaHome(user.uid)
        }
    }

    private fun irParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irParaHome(uid: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("usuarioId", uid)
        intent.putExtra("nomeUsuario", "")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}