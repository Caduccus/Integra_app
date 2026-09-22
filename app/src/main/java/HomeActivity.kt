package com.example.plataformaremota

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usuarioId = intent.getStringExtra("usuarioId") ?: ""
        nomeUsuario = intent.getStringExtra("nomeUsuario") ?: ""

        bottomNav = findViewById(R.id.bottomNav)

        // ⭐ Aplica a cor da navbar conforme o tema
        val tema = ThemeManager.getTemaAtual(this)
        bottomNav.setBackgroundColor(ThemeManager.getPrimary(tema))

        if (savedInstanceState == null) {
            trocarFragment(HomeFragment())
            bottomNav.selectedItemId = R.id.nav_inicio
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> { trocarFragment(HomeFragment()); true }
                R.id.nav_publicar -> { trocarFragment(PublishFragment()); true }
                R.id.nav_mensagens -> { trocarFragment(ChatListFragment()); true }
                R.id.nav_perfil -> { trocarFragment(ProfileFragment()); true }
                else -> false
            }
        }
    }

    private fun trocarFragment(fragment: Fragment) {
        val bundle = Bundle().apply {
            putString("usuarioId", usuarioId)
            putString("nomeUsuario", nomeUsuario)
        }
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}