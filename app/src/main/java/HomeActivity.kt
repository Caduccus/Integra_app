package com.example.plataformaremota

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

    private var fragmentAtualId: Int = R.id.nav_inicio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usuarioId = intent.getStringExtra("usuarioId") ?: ""
        nomeUsuario = intent.getStringExtra("nomeUsuario") ?: ""

        bottomNav = findViewById(R.id.bottomNav)

        val tema = ThemeManager.getTemaAtual(this)
        bottomNav.setBackgroundColor(ThemeManager.getPrimary(tema))

        if (savedInstanceState == null) {
            trocarFragment(HomeFragment(), false)
            bottomNav.selectedItemId = R.id.nav_inicio
            fragmentAtualId = R.id.nav_inicio
        }

        bottomNav.setOnItemSelectedListener { item ->
            // ⭐ Não faz nada se já tá na mesma aba (evita re-criar fragment)
            if (item.itemId == fragmentAtualId) {
                return@setOnItemSelectedListener true
            }

            fragmentAtualId = item.itemId

            when (item.itemId) {
                R.id.nav_inicio -> { trocarFragment(HomeFragment(), true); true }
                R.id.nav_publicar -> { trocarFragment(PublishFragment(), true); true }
                R.id.nav_mensagens -> { trocarFragment(ChatListFragment(), true); true }
                R.id.nav_perfil -> { trocarFragment(ProfileFragment(), true); true }
                else -> false
            }
        }
    }

    private fun trocarFragment(fragment: Fragment, animar: Boolean) {
        val bundle = Bundle().apply {
            putString("usuarioId", usuarioId)
            putString("nomeUsuario", nomeUsuario)
        }
        fragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()

        // ⭐ Aplica animação suave
        if (animar) {
            transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
        }

        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}