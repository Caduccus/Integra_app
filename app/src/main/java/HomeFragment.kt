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
import com.example.plataformaremota.data.database.AppDatabase
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var txtBoasVindas: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var recyclerTrabalhos: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var adapter: TrabalhoAdapter

    private var usuarioId: Int = 0
    private var nomeUsuario: String = ""

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

        // Recebe os dados do usuário via arguments
        usuarioId = arguments?.getInt("usuarioId", 0) ?: 0
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        txtBoasVindas.text = if (nomeUsuario.isEmpty()) {
            "Olá!"
        } else {
            "Olá, $nomeUsuario!"
        }

        configurarRecyclerView()

        btnLogout.setOnClickListener {
            fazerLogout()
        }

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

    private fun carregarTrabalhos() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val trabalhos = database.trabalhoDao().listarTodos()

            adapter.atualizarLista(trabalhos)

            if (trabalhos.isEmpty()) {
                layoutEstadoVazio.visibility = View.VISIBLE
                recyclerTrabalhos.visibility = View.GONE
            } else {
                layoutEstadoVazio.visibility = View.GONE
                recyclerTrabalhos.visibility = View.VISIBLE
            }
        }
    }

    private fun fazerLogout() {
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