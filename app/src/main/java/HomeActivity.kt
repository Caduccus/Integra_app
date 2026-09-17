package com.example.plataformaremota

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private var usuarioId: Int = 0
    private var nomeUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usuarioId = intent.getIntExtra("usuarioId", 0)
        nomeUsuario = intent.getStringExtra("nomeUsuario") ?: ""

        bottomNav = findViewById(R.id.bottomNav)

        // Carrega o fragment inicial (Início)
        if (savedInstanceState == null) {
            trocarFragment(HomeFragment())
            bottomNav.selectedItemId = R.id.nav_inicio
        }

        // Listener dos cliques na navbar
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    trocarFragment(HomeFragment())
                    true
                }
                R.id.nav_publicar -> {
                    trocarFragment(PublishFragment())
                    true
                }
                R.id.nav_mensagens -> {
                    trocarFragment(ChatListFragment())
                    true
                }
                R.id.nav_perfil -> {
                    trocarFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun trocarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}