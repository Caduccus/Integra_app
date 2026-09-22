package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.TrabalhoAdapter
import com.example.plataformaremota.data.entity.Trabalho
import com.example.plataformaremota.data.repository.TrabalhoRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {

    private lateinit var txtBoasVindas: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var recyclerTrabalhos: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View
    private lateinit var txtEstadoVazio: TextView
    private lateinit var btnTodos: MaterialButton
    private lateinit var btnMeus: MaterialButton
    private lateinit var btnInscritos: MaterialButton

    private lateinit var adapter: TrabalhoAdapter
    private lateinit var repository: TrabalhoRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

    private var todosTrabalhos: List<Trabalho> = emptyList()
    private var idsCandidatados: Set<String> = emptySet()

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
        btnTodos = view.findViewById(R.id.btnTodos)
        btnMeus = view.findViewById(R.id.btnMeus)
        btnInscritos = view.findViewById(R.id.btnInscritos)

        repository = TrabalhoRepository(requireContext())

        usuarioId = arguments?.getString("usuarioId") ?: auth.currentUser?.uid ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        txtBoasVindas.text = if (nomeUsuario.isEmpty()) "Olá!" else "Olá, $nomeUsuario!"

        configurarRecyclerView()
        configurarBotoes()

        btnLogout.setOnClickListener { fazerLogout() }
        carregarTudo()
    }

    private fun configurarRecyclerView() {
        adapter = TrabalhoAdapter(emptyList()) { trabalho ->
            val intent = Intent(requireContext(), MainActivity2::class.java)
            intent.putExtra("trabalhoId", trabalho.id)
            startActivity(intent)
        }

        recyclerTrabalhos.layoutManager = LinearLayoutManager(requireContext())
        recyclerTrabalhos.adapter = adapter
    }

    private fun configurarBotoes() {
        btnTodos.setOnClickListener {
            btnTodos.isChecked = true
            btnMeus.isChecked = false
            btnInscritos.isChecked = false
            aplicarFiltro()
        }
        btnMeus.setOnClickListener {
            btnMeus.isChecked = true
            btnTodos.isChecked = false
            btnInscritos.isChecked = false
            aplicarFiltro()
        }
        btnInscritos.setOnClickListener {
            btnInscritos.isChecked = true
            btnTodos.isChecked = false
            btnMeus.isChecked = false
            aplicarFiltro()
        }
    }

    private fun carregarTudo() {
        mostrarLoading()

        lifecycleScope.launch {
            todosTrabalhos = repository.listarTodos()
            carregarCandidaturas()
            aplicarFiltro()
        }
    }

    private suspend fun carregarCandidaturas() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val trabalhosSnapshot = db.collection("trabalhos").get().await()
            val set = mutableSetOf<String>()

            for (doc in trabalhosSnapshot.documents) {
                val candidatura = doc.reference
                    .collection("candidaturas")
                    .document(uid)
                    .get()
                    .await()

                if (candidatura.exists()) {
                    set.add(doc.id)
                }
            }

            idsCandidatados = set
            Log.d("HOME", "✅ ${idsCandidatados.size} candidaturas encontradas")
        } catch (e: Exception) {
            Log.e("HOME", "❌ Erro candidaturas: ${e.message}")
            idsCandidatados = emptySet()
        }
    }

    private fun aplicarFiltro() {
        val uid = auth.currentUser?.uid ?: usuarioId

        val filtrados = when {
            btnMeus.isChecked -> todosTrabalhos.filter { it.criadorId == uid }
            btnInscritos.isChecked -> todosTrabalhos.filter { it.id in idsCandidatados }
            else -> todosTrabalhos
        }

        adapter.atualizarLista(filtrados)

        if (filtrados.isEmpty()) {
            txtEstadoVazio.text = when {
                btnMeus.isChecked -> "Você ainda não publicou nenhum trabalho"
                btnInscritos.isChecked -> "Você ainda não se inscreveu em nenhum trabalho"
                else -> "Nenhum trabalho disponível ainda"
            }
            mostrarEstadoVazio()
        } else {
            mostrarLista()
        }
    }

    private fun mostrarLoading() {
        layoutLoading.visibility = View.VISIBLE
        recyclerTrabalhos.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarLista() {
        layoutLoading.visibility = View.GONE
        recyclerTrabalhos.visibility = View.VISIBLE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarEstadoVazio() {
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
        if (::adapter.isInitialized) {
            carregarTudo()
        }
    }
}