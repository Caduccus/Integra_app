package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.EmpresaAdapter
import com.example.plataformaremota.data.entity.Empresa
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EmpresasFragment : Fragment() {

    private lateinit var rvEmpresas: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View
    private lateinit var txtVazioTitulo: TextView
    private lateinit var txtVazioSubtitulo: TextView
    private lateinit var btnCriarEmpresaVazio: MaterialButton
    private lateinit var btnCriarEmpresaTop: MaterialButton
    private lateinit var btnDescobrir: MaterialButton
    private lateinit var btnMinhasEmpresas: MaterialButton
    private lateinit var btnPedidosEmpresa: MaterialButton

    private lateinit var adapter: EmpresaAdapter
    private lateinit var repository: EmpresaRepository

    private val auth = FirebaseAuth.getInstance()

    private var todasDescobrir: List<Empresa> = emptyList()
    private var todasMinhas: List<Empresa> = emptyList()
    private var todasPedidos: List<Empresa> = emptyList()

    private val criarEmpresaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            carregarTudo()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_empresas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvEmpresas = view.findViewById(R.id.rvEmpresas)
        layoutEstadoVazio = view.findViewById(R.id.layoutEstadoVazioEmpresas)
        layoutLoading = view.findViewById(R.id.layoutLoadingEmpresas)
        txtVazioTitulo = view.findViewById(R.id.txtVazioTitulo)
        txtVazioSubtitulo = view.findViewById(R.id.txtVazioSubtitulo)
        btnCriarEmpresaVazio = view.findViewById(R.id.btnCriarEmpresaVazio)
        btnCriarEmpresaTop = view.findViewById(R.id.btnCriarEmpresaTop)
        btnDescobrir = view.findViewById(R.id.btnDescobrir)
        btnMinhasEmpresas = view.findViewById(R.id.btnMinhasEmpresas)
        btnPedidosEmpresa = view.findViewById(R.id.btnPedidosEmpresa)

        repository = EmpresaRepository()

        configurarRecyclerView()
        configurarBotoes()

        btnCriarEmpresaTop.setOnClickListener { abrirCriarEmpresa() }
        btnCriarEmpresaVazio.setOnClickListener { abrirCriarEmpresa() }

        carregarTudo()

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
    }

    private fun configurarRecyclerView() {
        adapter = EmpresaAdapter(
            empresas = emptyList(),
            onCardClick = { empresa -> abrirDetalhes(empresa) },
            onBotaoClick = { empresa -> acaoBotao(empresa) },
            textoBotao = { empresa -> textoBotao(empresa) },
            corBotao = { empresa -> corBotao(empresa) }
        )
        rvEmpresas.layoutManager = LinearLayoutManager(requireContext())
        rvEmpresas.adapter = adapter
    }

    private fun configurarBotoes() {
        btnDescobrir.setOnClickListener {
            btnDescobrir.isChecked = true
            btnMinhasEmpresas.isChecked = false
            btnPedidosEmpresa.isChecked = false
            aplicarFiltro()
        }
        btnMinhasEmpresas.setOnClickListener {
            btnMinhasEmpresas.isChecked = true
            btnDescobrir.isChecked = false
            btnPedidosEmpresa.isChecked = false
            aplicarFiltro()
        }
        btnPedidosEmpresa.setOnClickListener {
            btnPedidosEmpresa.isChecked = true
            btnDescobrir.isChecked = false
            btnMinhasEmpresas.isChecked = false
            aplicarFiltro()
        }
    }

    private fun carregarTudo() {
        mostrarLoading()
        lifecycleScope.launch {
            try {
                todasDescobrir = repository.descobrir()
                todasMinhas = repository.listarMinhas()
                todasPedidos = repository.listarPendentesMinhas()
                aplicarFiltro()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                mostrarEstadoVazio()
            }
        }
    }

    private fun aplicarFiltro() {
        val lista = when {
            btnMinhasEmpresas.isChecked -> todasMinhas
            btnPedidosEmpresa.isChecked -> todasPedidos
            else -> todasDescobrir
        }

        adapter.atualizarLista(lista)

        if (lista.isEmpty()) {
            when {
                btnMinhasEmpresas.isChecked -> {
                    txtVazioTitulo.text = "Você não tem empresas"
                    txtVazioSubtitulo.text = "Crie uma ou peça pra entrar em uma"
                    btnCriarEmpresaVazio.visibility = View.VISIBLE
                }
                btnPedidosEmpresa.isChecked -> {
                    txtVazioTitulo.text = "Sem pedidos pendentes"
                    txtVazioSubtitulo.text = "Quando você pedir pra entrar, aparece aqui"
                    btnCriarEmpresaVazio.visibility = View.GONE
                }
                else -> {
                    txtVazioTitulo.text = "Nenhuma empresa ainda"
                    txtVazioSubtitulo.text = "Seja o primeiro a criar uma!"
                    btnCriarEmpresaVazio.visibility = View.VISIBLE
                }
            }
            mostrarEstadoVazio()
        } else {
            mostrarLista()
        }
    }

    private fun textoBotao(empresa: Empresa): String {
        val uid = auth.currentUser?.uid ?: return ""
        return when {
            uid in empresa.pendentes -> "CANCELAR"
            uid in empresa.membros -> ""
            else -> "PEDIR"
        }
    }

    private fun corBotao(empresa: Empresa): Int {
        val uid = auth.currentUser?.uid ?: return 0xFF43A047.toInt()
        return when {
            uid in empresa.pendentes -> 0xFFFB8C00.toInt()
            else -> 0xFF43A047.toInt()
        }
    }

    private fun acaoBotao(empresa: Empresa) {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            if (uid in empresa.pendentes) {
                val ok = repository.cancelarPedido(empresa.id)
                if (ok) {
                    Toast.makeText(requireContext(), "Pedido cancelado", Toast.LENGTH_SHORT).show()
                    carregarTudo()
                }
            } else {
                val ok = repository.pedirParaEntrar(empresa.id)
                if (ok) {
                    Toast.makeText(requireContext(), "Pedido enviado! Aguarde aprovação.", Toast.LENGTH_LONG).show()
                    carregarTudo()
                } else {
                    Toast.makeText(requireContext(), "Erro ao pedir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirCriarEmpresa() {
        val intent = Intent(requireContext(), CadastroEmpresaActivity::class.java)
        criarEmpresaLauncher.launch(intent)
    }

    private fun abrirDetalhes(empresa: Empresa) {
        val intent = Intent(requireContext(), DetalhesEmpresaActivity::class.java)
        intent.putExtra("empresaId", empresa.id)
        startActivity(intent)
    }

    private fun mostrarLoading() {
        layoutLoading.visibility = View.VISIBLE
        rvEmpresas.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarLista() {
        layoutLoading.visibility = View.GONE
        rvEmpresas.visibility = View.VISIBLE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarEstadoVazio() {
        layoutLoading.visibility = View.GONE
        rvEmpresas.visibility = View.GONE
        layoutEstadoVazio.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            carregarTudo()
        }
    }
}