package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.SkeletonAdapter
import com.example.plataformaremota.adapter.TrabalhoAdapter
import com.example.plataformaremota.data.entity.Trabalho
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.example.plataformaremota.data.repository.TrabalhoRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {

    private lateinit var txtBoasVindas: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var recyclerTrabalhos: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View
    private lateinit var txtEstadoVazio: TextView
    private lateinit var txtEstadoVazioTitulo: TextView
    private lateinit var btnPublicarVazio: MaterialButton
    private lateinit var btnTodos: MaterialButton
    private lateinit var btnMeus: MaterialButton
    private lateinit var btnInscritos: MaterialButton
    private lateinit var btnEmpresaFiltro: MaterialButton
    private lateinit var btnFavoritos: MaterialButton
    private lateinit var btnVerHistoricoCandidaturas: MaterialButton
    private lateinit var btnSinoNotif: ImageView
    private lateinit var badgeSino: TextView

    private var edtBuscaHome: EditText? = null
    private var termoBusca: String = ""

    private lateinit var adapter: TrabalhoAdapter
    private lateinit var skeletonAdapter: SkeletonAdapter

    private lateinit var repository: TrabalhoRepository
    private lateinit var empresaRepository: EmpresaRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

    private var todosTrabalhos: List<Trabalho> = emptyList()
    private var idsCandidatados: Set<String> = emptySet()
    private var minhasEmpresaIds: Set<String> = emptySet()
    private var meusFavoritos: Set<String> = emptySet()

    private val skeletonHandler = Handler(Looper.getMainLooper())
    private var skeletonRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtBoasVindas = view.findViewById(R.id.txtBoasVindas)
        btnLogout = view.findViewById(R.id.btnLogout)
        recyclerTrabalhos = view.findViewById(R.id.recyclerTrabalhos)
        layoutEstadoVazio = view.findViewById(R.id.layoutEstadoVazio)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        txtEstadoVazio = view.findViewById(R.id.txtEstadoVazio)
        txtEstadoVazioTitulo = view.findViewById(R.id.txtEstadoVazioTitulo)
        btnPublicarVazio = view.findViewById(R.id.btnPublicarVazio)
        btnTodos = view.findViewById(R.id.btnTodos)
        btnMeus = view.findViewById(R.id.btnMeus)
        btnInscritos = view.findViewById(R.id.btnInscritos)
        btnEmpresaFiltro = view.findViewById(R.id.btnEmpresaFiltro)
        btnFavoritos = view.findViewById(R.id.btnFavoritos)
        btnVerHistoricoCandidaturas = view.findViewById(R.id.btnVerHistoricoCandidaturas)
        btnSinoNotif = view.findViewById(R.id.btnSinoNotif)
        badgeSino = view.findViewById(R.id.badgeSino)

        edtBuscaHome = view.findViewById(R.id.edtBuscaHome)
        edtBuscaHome?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                termoBusca = s?.toString()?.trim()?.lowercase() ?: ""
                aplicarFiltro()
            }
        })

        repository = TrabalhoRepository(requireContext())
        empresaRepository = EmpresaRepository()

        usuarioId = arguments?.getString("usuarioId") ?: auth.currentUser?.uid ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        txtBoasVindas.text = if (nomeUsuario.isEmpty()) "Olá!" else "Olá, $nomeUsuario!"

        configurarRecyclerView()
        configurarBotoes()

        btnLogout.setOnClickListener { fazerLogout() }
        btnPublicarVazio.setOnClickListener {
            (requireActivity() as? HomeActivity)?.abrirAbaPublicar()
        }
        btnVerHistoricoCandidaturas.setOnClickListener {
            startActivity(Intent(requireContext(), MinhasCandidaturasActivity::class.java))
        }
        btnSinoNotif.setOnClickListener {
            startActivity(Intent(requireContext(), NotificacoesActivity::class.java))
        }
        view.findViewById<View>(R.id.btnBuscaGlobal).setOnClickListener {
            startActivity(Intent(requireContext(), BuscaGlobalActivity::class.java))
        }

        iniciarBadgeSino()
        carregarTudo()

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
    }

    private fun iniciarBadgeSino() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).collection("notificacoes")
            .whereEqualTo("lida", false)
            .addSnapshotListener { snap, _ ->
                if (!isAdded) return@addSnapshotListener
                val qtd = snap?.size() ?: 0
                if (qtd > 0) {
                    badgeSino.visibility = View.VISIBLE
                    badgeSino.text = if (qtd > 99) "99+" else qtd.toString()
                } else {
                    badgeSino.visibility = View.GONE
                }
            }
    }

    private fun configurarRecyclerView() {
        adapter = TrabalhoAdapter(
            trabalhos = emptyList(),
            onClick = { trabalho ->
                val intent = Intent(requireContext(), MainActivity2::class.java)
                intent.putExtra("trabalhoId", trabalho.id)
                startActivity(intent)
            },
            mostrarFavorito = true,
            favoritos = emptySet(),
            onFavoritarClick = { trabalho -> toggleFavorito(trabalho) }
        )

        skeletonAdapter = SkeletonAdapter(R.layout.item_skeleton_trabalho, 5)

        recyclerTrabalhos.layoutManager = LinearLayoutManager(requireContext())
        recyclerTrabalhos.adapter = adapter
    }

    private fun configurarBotoes() {
        btnTodos.setOnClickListener { selecionarFiltro(btnTodos); aplicarFiltro() }
        btnMeus.setOnClickListener { selecionarFiltro(btnMeus); aplicarFiltro() }
        btnInscritos.setOnClickListener { selecionarFiltro(btnInscritos); aplicarFiltro() }
        btnEmpresaFiltro.setOnClickListener { selecionarFiltro(btnEmpresaFiltro); aplicarFiltro() }
        btnFavoritos.setOnClickListener { selecionarFiltro(btnFavoritos); aplicarFiltro() }
    }

    private fun selecionarFiltro(btn: MaterialButton) {
        btnTodos.isChecked = (btn == btnTodos)
        btnMeus.isChecked = (btn == btnMeus)
        btnInscritos.isChecked = (btn == btnInscritos)
        btnEmpresaFiltro.isChecked = (btn == btnEmpresaFiltro)
        btnFavoritos.isChecked = (btn == btnFavoritos)

        btnVerHistoricoCandidaturas.visibility =
            if (btn == btnInscritos) View.VISIBLE else View.GONE
    }

    private fun carregarTudo() {
        lifecycleScope.launch {
            val cache = repository.listarCache()
            if (cache.isNotEmpty() && todosTrabalhos.isEmpty()) {
                todosTrabalhos = cache
                aplicarFiltro()
            } else {
                mostrarLoading()
            }

            try {
                coroutineScope {
                    val trabalhosDeferred = async { repository.listarTodos() }
                    val empresasDeferred = async { empresaRepository.listarMinhas() }
                    val candidaturasDeferred = async { buscarMinhasCandidaturas() }
                    val favoritosDeferred = async { buscarMeusFavoritos() }

                    todosTrabalhos = trabalhosDeferred.await()
                    minhasEmpresaIds = empresasDeferred.await().map { it.id }.toSet()
                    idsCandidatados = candidaturasDeferred.await()
                    meusFavoritos = favoritosDeferred.await()
                }
                adapter.atualizarFavoritos(meusFavoritos)
                aplicarFiltro()
            } catch (e: Exception) {
                Log.e("HOME", "Erro: ${e.message}")
                if (todosTrabalhos.isEmpty()) mostrarEstadoVazio()
            }
        }
    }

    private suspend fun buscarMeusFavoritos(): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            (doc.get("favoritos") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        } catch (e: Exception) { emptySet() }
    }

    private fun toggleFavorito(trabalho: Trabalho) {
        val uid = auth.currentUser?.uid ?: return
        val jaFavorito = trabalho.id in meusFavoritos

        meusFavoritos = if (jaFavorito) meusFavoritos - trabalho.id else meusFavoritos + trabalho.id
        adapter.atualizarFavoritos(meusFavoritos)

        if (btnFavoritos.isChecked) aplicarFiltro()

        lifecycleScope.launch {
            try {
                val campo = if (jaFavorito) FieldValue.arrayRemove(trabalho.id)
                else FieldValue.arrayUnion(trabalho.id)
                db.collection("usuarios").document(uid)
                    .update("favoritos", campo).await()
            } catch (e: Exception) {
                meusFavoritos = if (jaFavorito) meusFavoritos + trabalho.id
                else meusFavoritos - trabalho.id
                adapter.atualizarFavoritos(meusFavoritos)
                Log.e("HOME", "Erro ao favoritar: ${e.message}")
            }
        }
    }

    private suspend fun buscarMinhasCandidaturas(): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        return try {
            val trabalhos = db.collection("trabalhos").get().await()
            coroutineScope {
                trabalhos.documents.map { doc ->
                    async {
                        try {
                            val c = doc.reference
                                .collection("candidaturas")
                                .document(uid)
                                .get().await()
                            if (c.exists()) doc.id else null
                        } catch (e: Exception) { null }
                    }
                }.awaitAll().filterNotNull().toSet()
            }
        } catch (e: Exception) { emptySet() }
    }

    private fun aplicarFiltro() {
        val uid = auth.currentUser?.uid ?: usuarioId

        var filtrados = when {
            btnMeus.isChecked -> todosTrabalhos.filter { it.criadorId == uid }
            btnInscritos.isChecked -> todosTrabalhos.filter { it.id in idsCandidatados }
            btnEmpresaFiltro.isChecked -> todosTrabalhos.filter {
                it.empresaId.isNotEmpty() && it.empresaId in minhasEmpresaIds
            }
            btnFavoritos.isChecked -> todosTrabalhos.filter { it.id in meusFavoritos }
            else -> todosTrabalhos.filter {
                it.empresaId.isEmpty() || it.empresaId in minhasEmpresaIds
            }
        }

        if (termoBusca.isNotEmpty()) {
            filtrados = filtrados.filter {
                it.titulo.lowercase().contains(termoBusca) ||
                        it.descricao.lowercase().contains(termoBusca) ||
                        it.categoria.lowercase().contains(termoBusca)
            }
        }

        adapter.atualizarLista(filtrados)

        if (filtrados.isEmpty()) {
            when {
                btnMeus.isChecked -> {
                    txtEstadoVazioTitulo.text = "Nada publicado"
                    txtEstadoVazio.text = "Você ainda não publicou nenhum trabalho"
                    btnPublicarVazio.visibility = View.VISIBLE
                }
                btnInscritos.isChecked -> {
                    txtEstadoVazioTitulo.text = "Sem inscrições"
                    txtEstadoVazio.text = "Candidate-se a um trabalho para aparecer aqui"
                    btnPublicarVazio.visibility = View.GONE
                }
                btnEmpresaFiltro.isChecked -> {
                    txtEstadoVazioTitulo.text = "Sem trabalhos de empresa"
                    txtEstadoVazio.text = "Nenhum trabalho das suas empresas por enquanto"
                    btnPublicarVazio.visibility = View.GONE
                }
                btnFavoritos.isChecked -> {
                    txtEstadoVazioTitulo.text = "Sem favoritos"
                    txtEstadoVazio.text = "Toque no coração de um trabalho para salvá-lo aqui"
                    btnPublicarVazio.visibility = View.GONE
                }
                else -> {
                    txtEstadoVazioTitulo.text = "Nada por aqui ainda"
                    txtEstadoVazio.text = "Seja o primeiro a publicar um trabalho!"
                    btnPublicarVazio.visibility = View.VISIBLE
                }
            }
            mostrarEstadoVazio()
        } else {
            mostrarLista()
        }
    }

    private fun mostrarLoading() {
        recyclerTrabalhos.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
        layoutLoading.visibility = View.GONE

        skeletonRunnable?.let { skeletonHandler.removeCallbacks(it) }
        skeletonRunnable = Runnable {
            if (!isAdded) return@Runnable
            recyclerTrabalhos.adapter = skeletonAdapter
            recyclerTrabalhos.visibility = View.VISIBLE
        }
        skeletonHandler.postDelayed(skeletonRunnable!!, 350)
    }

    private fun cancelarSkeleton() {
        skeletonRunnable?.let { skeletonHandler.removeCallbacks(it) }
        skeletonRunnable = null
    }

    private fun mostrarLista() {
        cancelarSkeleton()
        layoutLoading.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
        recyclerTrabalhos.adapter = adapter
        recyclerTrabalhos.visibility = View.VISIBLE
    }

    private fun mostrarEstadoVazio() {
        cancelarSkeleton()
        layoutLoading.visibility = View.GONE
        recyclerTrabalhos.visibility = View.GONE
        layoutEstadoVazio.visibility = View.VISIBLE
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) carregarTudo()
    }

    override fun onDestroyView() {
        cancelarSkeleton()
        super.onDestroyView()
    }
}