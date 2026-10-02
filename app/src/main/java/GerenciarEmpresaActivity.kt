package com.example.plataformaremota

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.MembroGerenciar
import com.example.plataformaremota.adapter.MembroGerenciarAdapter
import com.example.plataformaremota.adapter.Pendente
import com.example.plataformaremota.adapter.PendenteAdapter
import com.example.plataformaremota.data.entity.Empresa
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GerenciarEmpresaActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var txtTitulo: TextView
    private lateinit var btnTabMembros: MaterialButton
    private lateinit var btnTabPendentes: MaterialButton
    private lateinit var rvGerenciar: RecyclerView
    private lateinit var layoutVazio: View
    private lateinit var layoutLoading: View
    private lateinit var txtVazio: TextView
    private lateinit var btnExcluirEmpresa: MaterialButton

    private lateinit var membroAdapter: MembroGerenciarAdapter
    private lateinit var pendenteAdapter: PendenteAdapter

    private lateinit var repository: EmpresaRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var empresaId: String = ""
    private var empresaAtual: Empresa? = null
    private var listaMembros: List<MembroGerenciar> = emptyList()
    private var listaPendentes: List<Pendente> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gerenciar_empresa)

        empresaId = intent.getStringExtra("empresaId") ?: ""
        if (empresaId.isEmpty()) {
            Toast.makeText(this, "Empresa não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        repository = EmpresaRepository()

        btnVoltar = findViewById(R.id.btnVoltarGerenciar)
        txtTitulo = findViewById(R.id.txtTituloGerenciar)
        btnTabMembros = findViewById(R.id.btnTabMembros)
        btnTabPendentes = findViewById(R.id.btnTabPendentes)
        rvGerenciar = findViewById(R.id.rvGerenciar)
        layoutVazio = findViewById(R.id.layoutVazioGerenciar)
        layoutLoading = findViewById(R.id.layoutLoadingGerenciar)
        txtVazio = findViewById(R.id.txtVazioGerenciar)
        btnExcluirEmpresa = findViewById(R.id.btnExcluirEmpresa)

        btnExcluirEmpresa.setOnClickListener { confirmarExcluirEmpresa() }

        configurarAdapters()
        configurarBotoes()

        btnVoltar.setOnClickListener { finish() }

        carregarTudo()

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun configurarAdapters() {
        membroAdapter = MembroGerenciarAdapter(emptyList()) { membro ->
            abrirDialogAcoes(membro)
        }
        pendenteAdapter = PendenteAdapter(
            pendentes = emptyList(),
            onAprovar = { pendente -> aprovar(pendente) },
            onRejeitar = { pendente -> rejeitar(pendente) }
        )
        rvGerenciar.layoutManager = LinearLayoutManager(this)
    }

    private fun configurarBotoes() {
        btnTabMembros.setOnClickListener {
            btnTabMembros.isChecked = true
            btnTabPendentes.isChecked = false
            trocarAba()
        }
        btnTabPendentes.setOnClickListener {
            btnTabPendentes.isChecked = true
            btnTabMembros.isChecked = false
            trocarAba()
        }
    }

    private fun trocarAba() {
        if (btnTabPendentes.isChecked) {
            rvGerenciar.adapter = pendenteAdapter
            atualizarEstadoVazio(listaPendentes.isEmpty(), "Nenhum pedido pendente")
        } else {
            rvGerenciar.adapter = membroAdapter
            atualizarEstadoVazio(listaMembros.isEmpty(), "Nenhum membro na empresa")
        }
    }

    private fun carregarTudo() {
        layoutLoading.visibility = View.VISIBLE
        rvGerenciar.visibility = View.GONE
        layoutVazio.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val empresa = repository.buscarPorId(empresaId)
                if (empresa == null) {
                    finish()
                    return@launch
                }
                empresaAtual = empresa
                txtTitulo.text = "Gerenciar: ${empresa.nome}"

                val uidAtual = auth.currentUser?.uid ?: ""
                btnExcluirEmpresa.visibility =
                    if (empresa.criadorId == uidAtual) View.VISIBLE else View.GONE

                carregarMembros(empresa)
                carregarPendentes(empresa)
                trocarAba()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            layoutLoading.visibility = View.GONE
        }
    }

    private suspend fun carregarMembros(empresa: Empresa) {
        val lista = mutableListOf<MembroGerenciar>()
        for (uid in empresa.membros) {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                lista.add(
                    MembroGerenciar(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: "",
                        ehAdmin = empresa.admins.contains(uid),
                        ehDono = empresa.criadorId == uid
                    )
                )
            } catch (_: Exception) { }
        }
        listaMembros = lista.sortedWith(
            compareByDescending<MembroGerenciar> { it.ehDono }
                .thenByDescending { it.ehAdmin }
                .thenBy { it.nome.lowercase() }
        )
        membroAdapter.atualizarLista(listaMembros)
    }

    private suspend fun carregarPendentes(empresa: Empresa) {
        val lista = mutableListOf<Pendente>()
        for (uid in empresa.pendentes) {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                lista.add(
                    Pendente(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                )
            } catch (_: Exception) { }
        }
        listaPendentes = lista.sortedBy { it.nome.lowercase() }
        pendenteAdapter.atualizarLista(listaPendentes)

        btnTabPendentes.text = if (listaPendentes.isEmpty())
            "Pendentes" else "Pendentes (${listaPendentes.size})"
    }

    private fun atualizarEstadoVazio(vazio: Boolean, msg: String) {
        if (vazio) {
            rvGerenciar.visibility = View.GONE
            layoutVazio.visibility = View.VISIBLE
            txtVazio.text = msg
        } else {
            rvGerenciar.visibility = View.VISIBLE
            layoutVazio.visibility = View.GONE
        }
    }

    private fun aprovar(pendente: Pendente) {
        lifecycleScope.launch {
            val ok = repository.aprovarMembro(empresaId, pendente.uid)
            if (ok) {
                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "${pendente.nome} aprovado!",
                    Toast.LENGTH_SHORT
                ).show()
                carregarTudo()
            } else {
                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "Erro ao aprovar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rejeitar(pendente: Pendente) {
        AlertDialog.Builder(this)
            .setTitle("Rejeitar pedido")
            .setMessage("Rejeitar o pedido de ${pendente.nome}?")
            .setPositiveButton("Rejeitar") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.rejeitarMembro(empresaId, pendente.uid)
                    if (ok) {
                        Toast.makeText(
                            this@GerenciarEmpresaActivity,
                            "Pedido rejeitado",
                            Toast.LENGTH_SHORT
                        ).show()
                        carregarTudo()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirDialogAcoes(membro: MembroGerenciar) {
        val uidAtual = auth.currentUser?.uid ?: return
        val empresa = empresaAtual ?: return

        if (membro.ehDono) {
            Toast.makeText(this, "O dono não pode ser alterado", Toast.LENGTH_SHORT).show()
            return
        }

        if (membro.uid == uidAtual) {
            Toast.makeText(this, "Você não pode alterar a si mesmo", Toast.LENGTH_SHORT).show()
            return
        }

        val opcoes = mutableListOf<String>()
        if (membro.ehAdmin) {
            opcoes.add("Rebaixar admin")
        } else {
            opcoes.add("Promover a admin")
        }
        opcoes.add("Remover do grupo")

        AlertDialog.Builder(this)
            .setTitle(membro.nome)
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "Promover a admin" -> promover(membro)
                    "Rebaixar admin" -> rebaixar(membro)
                    "Remover do grupo" -> confirmarRemover(membro)
                }
            }
            .show()
    }

    private fun promover(membro: MembroGerenciar) {
        lifecycleScope.launch {
            val ok = repository.promoverAdmin(empresaId, membro.uid)
            if (ok) {
                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "${membro.nome} agora é admin",
                    Toast.LENGTH_SHORT
                ).show()
                carregarTudo()
            }
        }
    }

    private fun rebaixar(membro: MembroGerenciar) {
        lifecycleScope.launch {
            val ok = repository.rebaixarAdmin(empresaId, membro.uid)
            if (ok) {
                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "${membro.nome} não é mais admin",
                    Toast.LENGTH_SHORT
                ).show()
                carregarTudo()
            }
        }
    }

    private fun confirmarRemover(membro: MembroGerenciar) {
        AlertDialog.Builder(this)
            .setTitle("Remover membro")
            .setMessage("Remover ${membro.nome} do grupo?")
            .setPositiveButton("Remover") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.removerMembro(empresaId, membro.uid)
                    if (ok) {
                        Toast.makeText(
                            this@GerenciarEmpresaActivity,
                            "Removido",
                            Toast.LENGTH_SHORT
                        ).show()
                        carregarTudo()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExcluirEmpresa() {
        val empresa = empresaAtual ?: return
        AlertDialog.Builder(this)
            .setTitle("⚠️ Excluir empresa")
            .setMessage(
                "Tem CERTEZA que quer excluir a empresa '${empresa.nome}'?\n\n" +
                        "Isso vai apagar:\n" +
                        "• A empresa\n" +
                        "• O grupo de chat \"Geral\"\n" +
                        "• Todas as mensagens do grupo\n\n" +
                        "Os trabalhos publicados vão continuar existindo, mas sem vínculo com a empresa.\n\n" +
                        "Essa ação NÃO pode ser desfeita!"
            )
            .setPositiveButton("EXCLUIR TUDO") { _, _ -> executarExclusao() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarExclusao() {
        Toast.makeText(this, "Excluindo empresa...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val ok = repository.deletarEmpresa(empresaId)

            if (ok) {
                empresaAtual?.membros?.forEach { uid ->
                    if (uid != auth.currentUser?.uid) {
                        NotificacaoHelper.enviar(
                            uidDestino = uid,
                            titulo = "Empresa excluída",
                            mensagem = "A empresa '${empresaAtual?.nome}' foi excluída pelo dono."
                        )
                    }
                }

                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "Empresa excluída",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this@GerenciarEmpresaActivity,
                    "Erro ao excluir empresa",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}