package com.example.plataformaremota

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""
    private var fragmentAtualId: Int = R.id.nav_inicio

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatsListener: ListenerRegistration? = null
    private val statusListeners = mutableMapOf<String, ListenerRegistration>()
    private val unreadPorChat = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usuarioId = intent.getStringExtra("usuarioId") ?: ""
        nomeUsuario = intent.getStringExtra("nomeUsuario") ?: ""

        if (nomeUsuario.isEmpty() && usuarioId.isNotEmpty()) {
            db.collection("usuarios").document(usuarioId).get()
                .addOnSuccessListener { doc ->
                    nomeUsuario = doc.getString("nome") ?: ""
                }
        }

        bottomNav = findViewById(R.id.bottomNav)

        val tema = ThemeManager.getTemaAtual(this)
        bottomNav.setBackgroundColor(ThemeManager.getPrimary(tema))

        // ⭐ Primeira vez: cria fragment inicial.
        //    Restore: sincroniza fragmentAtualId com o estado salvo.
        if (savedInstanceState == null) {
            trocarFragment(HomeFragment(), false)
            bottomNav.selectedItemId = R.id.nav_inicio
            fragmentAtualId = R.id.nav_inicio
        } else {
            // Recupera qual aba estava selecionada antes do recreate
            fragmentAtualId = savedInstanceState.getInt(
                "fragmentAtualId", bottomNav.selectedItemId
            )
            // Garante que o bottomNav e o fragmentAtualId batem
            bottomNav.post {
                if (bottomNav.selectedItemId != fragmentAtualId) {
                    bottomNav.selectedItemId = fragmentAtualId
                }
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == fragmentAtualId) {
                return@setOnItemSelectedListener true
            }

            fragmentAtualId = item.itemId

            when (item.itemId) {
                R.id.nav_inicio -> { trocarFragment(HomeFragment(), true); true }
                R.id.nav_empresas -> { trocarFragment(EmpresasFragment(), true); true }
                R.id.nav_publicar -> { trocarFragment(PublishFragment(), true); true }
                R.id.nav_mensagens -> { trocarFragment(ChatListFragment(), true); true }
                R.id.nav_perfil -> { trocarFragment(ProfileFragment(), true); true }
                else -> false
            }
        }

        iniciarBadgeNaoLidas()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // ⭐ Salva qual aba estava ativa pra restaurar depois do recreate
        outState.putInt("fragmentAtualId", fragmentAtualId)
    }

    fun abrirAbaPublicar() {
        bottomNav.selectedItemId = R.id.nav_publicar
    }

    private fun trocarFragment(fragment: Fragment, animar: Boolean) {
        val bundle = Bundle().apply {
            putString("usuarioId", usuarioId)
            putString("nomeUsuario", nomeUsuario)
        }
        fragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()

        // ⭐ Só anima se não tiver "reduzir animações" ativo
        if (animar && !AcessibilidadePrefs.isReduzirAnimacoes(this)) {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        }

        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }

    private fun iniciarBadgeNaoLidas() {
        val uid = auth.currentUser?.uid ?: return

        chatsListener = db.collection("chats")
            .whereArrayContains("participantes", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val chatIds = snapshot.documents
                    .filter { doc ->
                        val deletados = (doc.get("deletadosPara") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()
                        uid !in deletados
                    }
                    .map { it.id }
                    .toSet()

                val removidos = statusListeners.keys - chatIds
                removidos.forEach { chatId ->
                    statusListeners[chatId]?.remove()
                    statusListeners.remove(chatId)
                    unreadPorChat.remove(chatId)
                }

                chatIds.forEach { chatId ->
                    if (!statusListeners.containsKey(chatId)) {
                        val reg = db.collection("chats")
                            .document(chatId)
                            .collection("status")
                            .document(uid)
                            .addSnapshotListener { doc, _ ->
                                val count = doc?.getLong("unreadCount")?.toInt() ?: 0
                                unreadPorChat[chatId] = count
                                atualizarBadge()
                            }
                        statusListeners[chatId] = reg
                    }
                }

                atualizarBadge()
            }
    }

    private fun atualizarBadge() {
        val chatAtivo = ChatAtivoManager.chatAtivo
        val total = unreadPorChat
            .filterKeys { it != chatAtivo }
            .values
            .sum()

        val badge = bottomNav.getOrCreateBadge(R.id.nav_mensagens)

        if (total > 0) {
            badge.isVisible = true
            badge.number = total
            badge.backgroundColor = Color.RED
        } else {
            badge.isVisible = false
            badge.clearNumber()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarBadge()
    }

    override fun onDestroy() {
        chatsListener?.remove()
        statusListeners.values.forEach { it.remove() }
        statusListeners.clear()
        super.onDestroy()
    }
}