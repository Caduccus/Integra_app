package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
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
import com.example.plataformaremota.adapter.TrabalhoAdapter
import com.example.plataformaremota.data.entity.Trabalho
import com.example.plataformaremota.data.repository.EmpresaRepository
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
    private lateinit var txtEstadoVazioTitulo: TextView
    private lateinit var btnPublicarVazio: MaterialButton
    private lateinit var btnTodos: MaterialButton
    private lateinit var btnMeus: MaterialButton
    private lateinit var btnInscritos: MaterialButton
    private lateinit var btnEmpresaFiltro: MaterialButton

    // ⭐ Busca (só funciona se você adicionar edtBuscaHome no fragment_home.xml)
    private var edtBuscaHome: EditText? = null
    private var termoBusca: String = ""

    private lateinit var adapter: TrabalhoAdapter
    private lateinit var repository: TrabalhoRepository
    private lateinit var empresaRepository: EmpresaRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""

    private var todosTrabalhos: List<Trabalho> = emptyList()
    private var idsCandidatados: Set<String> = emptySet()
    private var minhasEmpresaIds: Set<String> = emptySet()

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

        // ⭐ Campo de busca (opcional — só se existir no layout)
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

        carregarTudo()

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
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
            selecionarFiltro(btnTodos)
            aplicarFiltro()
        }
        btnMeus.setOnClickListener {
            selecionarFiltro(btnMeus)
            aplicarFiltro()
        }
        btnInscritos.setOnClickListener {
            selecionarFiltro(btnInscritos)
            aplicarFiltro()
        }
        btnEmpresaFiltro.setOnClickListener {
            selecionarFiltro(btnEmpresaFiltro)
            aplicarFiltro()
        }
    }

    private fun selecionarFiltro(btn: MaterialButton) {
        btnTodos.isChecked = (btn == btnTodos)
        btnMeus.isChecked = (btn == btnMeus)
        btnInscritos.isChecked = (btn == btnInscritos)
        btnEmpresaFiltro.isChecked = (btn == btnEmpresaFiltro)
    }

    private fun carregarTudo() {
        mostrarLoading()

        lifecycleScope.launch {
            try {
                todosTrabalhos = repository.listarTodos()
                carregarCandidaturas()
                carregarMinhasEmpresas()
                aplicarFiltro()
            } catch (e: Exception) {
                Log.e("HOME", "Erro: ${e.message}")
                mostrarEstadoVazio()
            }
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

    private suspend fun carregarMinhasEmpresas() {
        try {
            val minhasEmpresas = empresaRepository.listarMinhas()
            minhasEmpresaIds = minhasEmpresas.map { it.id }.toSet()
            Log.d("HOME", "✅ ${minhasEmpresaIds.size} empresas do usuário")
        } catch (e: Exception) {
            Log.e("HOME", "❌ Erro empresas: ${e.message}")
            minhasEmpresaIds = emptySet()
        }
    }

    private fun aplicarFiltro() {
        val uid = auth.currentUser?.uid ?: usuarioId

        var filtrados = when {
            btnMeus.isChecked -> todosTrabalhos.filter { it.criadorId == uid }

            btnInscritos.isChecked -> todosTrabalhos.filter { it.id in idsCandidatados }

            btnEmpresaFiltro.isChecked -> {
                todosTrabalhos.filter {
                    it.empresaId.isNotEmpty() && it.empresaId in minhasEmpresaIds
                }
            }

            else -> {
                todosTrabalhos.filter {
                    it.empresaId.isEmpty() || it.empresaId in minhasEmpresaIds
                }
            }
        }

        // ⭐ Filtro de busca textual (só aplica se tiver texto)
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