package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
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
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var txtBoasVindas: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var recyclerTrabalhos: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View
    private lateinit var txtEstadoVazio: TextView
    private lateinit var chipTodos: Chip
    private lateinit var chipMeus: Chip

    private lateinit var adapter: TrabalhoAdapter
    private lateinit var repository: TrabalhoRepository

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

    private var todosTrabalhos: List<Trabalho> = emptyList()

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
        chipTodos = view.findViewById(R.id.chipTodos)
        chipMeus = view.findViewById(R.id.chipMeus)

        repository = TrabalhoRepository(requireContext())

        usuarioId = arguments?.getString("usuarioId") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        txtBoasVindas.text = if (nomeUsuario.isEmpty()) "Olá!" else "Olá, $nomeUsuario!"

        configurarRecyclerView()
        configurarChips()

        btnLogout.setOnClickListener { fazerLogout() }
        carregarTrabalhos()
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

    private fun configurarChips() {
        chipTodos.setOnClickListener {
            chipTodos.isChecked = true
            chipMeus.isChecked = false
            aplicarFiltro()
        }
        chipMeus.setOnClickListener {
            chipMeus.isChecked = true
            chipTodos.isChecked = false
            aplicarFiltro()
        }
    }

    // ─────────────────────────────────────────────
    // CARREGAR DO FIRESTORE
    // ─────────────────────────────────────────────
    private fun carregarTrabalhos() {
        mostrarLoading()

        lifecycleScope.launch {
            todosTrabalhos = repository.listarTodos()
            aplicarFiltro()
        }
    }

    // ─────────────────────────────────────────────
    // FILTRAR
    // ─────────────────────────────────────────────
    private fun aplicarFiltro() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: usuarioId

        val filtrados = if (chipMeus.isChecked) {
            todosTrabalhos.filter { it.criadorId == uid }
        } else {
            todosTrabalhos
        }

        adapter.atualizarLista(filtrados)

        if (filtrados.isEmpty()) {
            txtEstadoVazio.text = if (chipMeus.isChecked) {
                "Você ainda não publicou nenhum trabalho"
            } else {
                "Nenhum trabalho disponível ainda"
            }
            mostrarEstadoVazio()
        } else {
            mostrarLista()
        }
    }

    // ─────────────────────────────────────────────
    // CONTROLE DE VISIBILIDADE
    // ─────────────────────────────────────────────
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
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            carregarTrabalhos()
        }
    }
}