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
            Toast.makeText(this, getString(R.string.empresa_nao_encontrada), Toast.LENGTH_SHORT).show()
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
            atualizarEstadoVazio(listaPendentes.isEmpty(), getString(R.string.gerenciar_vazio_pendentes))
        } else {
            rvGerenciar.adapter = membroAdapter
            atualizarEstadoVazio(listaMembros.isEmpty(), getString(R.string.gerenciar_vazio_membros))
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
                txtTitulo.text = getString(R.string.gerenciar_titulo, empresa.nome)

                val uidAtual = auth.currentUser?.uid ?: ""
                btnExcluirEmpresa.visibility =
                    if (empresa.criadorId == uidAtual) View.VISIBLE else View.GONE

                carregarMembros(empresa)
                carregarPendentes(empresa)
                trocarAba()
            } catch (e: Exception) {
                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.geral_erro, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
                        nome = doc.getString("nome") ?: getString(R.string.admin_usuario_padrao),
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
                        nome = doc.getString("nome") ?: getString(R.string.admin_usuario_padrao),
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                )
            } catch (_: Exception) { }
        }
        listaPendentes = lista.sortedBy { it.nome.lowercase() }
        pendenteAdapter.atualizarLista(listaPendentes)

        btnTabPendentes.text = if (listaPendentes.isEmpty())
            getString(R.string.gerenciar_tab_pendentes) else getString(R.string.gerenciar_tab_pendentes_com, listaPendentes.size)
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
                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_aprovado, pendente.nome), Toast.LENGTH_SHORT).show()
                carregarTudo()
            } else {
                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_erro_aprovar), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejeitar(pendente: Pendente) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.gerenciar_rejeitar_titulo))
            .setMessage(getString(R.string.gerenciar_rejeitar_msg, pendente.nome))
            .setPositiveButton(getString(R.string.gerenciar_btn_excluir_empresa)) { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.rejeitarMembro(empresaId, pendente.uid)
                    if (ok) {
                        Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_rejeitado), Toast.LENGTH_SHORT).show()
                        carregarTudo()
                    }
                }
            }
            .setNegativeButton(getString(R.string.geral_cancelar), null)
            .show()
    }

    private fun abrirDialogAcoes(membro: MembroGerenciar) {
        val uidAtual = auth.currentUser?.uid ?: return
        val empresa = empresaAtual ?: return

        if (membro.ehDono) {
            Toast.makeText(this, getString(R.string.gerenciar_so_dono), Toast.LENGTH_SHORT).show()
            return
        }

        if (membro.uid == uidAtual) {
            Toast.makeText(this, getString(R.string.gerenciar_nao_alterar_self), Toast.LENGTH_SHORT).show()
            return
        }

        val opcoes = mutableListOf<String>()
        if (membro.ehAdmin) {
            opcoes.add(getString(R.string.gerenciar_opcoes_rebaixar))
        } else {
            opcoes.add(getString(R.string.gerenciar_opcoes_promover))
        }
        opcoes.add(getString(R.string.gerenciar_opcoes_remover))

        AlertDialog.Builder(this)
            .setTitle(membro.nome)
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    getString(R.string.gerenciar_opcoes_promover) -> promover(membro)
                    getString(R.string.gerenciar_opcoes_rebaixar) -> rebaixar(membro)
                    getString(R.string.gerenciar_opcoes_remover) -> confirmarRemover(membro)
                }
            }
            .show()
    }

    private fun promover(membro: MembroGerenciar) {
        lifecycleScope.launch {
            val ok = repository.promoverAdmin(empresaId, membro.uid)
            if (ok) {
                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_agora_admin, membro.nome), Toast.LENGTH_SHORT).show()
                carregarTudo()
            }
        }
    }

    private fun rebaixar(membro: MembroGerenciar) {
        lifecycleScope.launch {
            val ok = repository.rebaixarAdmin(empresaId, membro.uid)
            if (ok) {
                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_nao_admin, membro.nome), Toast.LENGTH_SHORT).show()
                carregarTudo()
            }
        }
    }

    private fun confirmarRemover(membro: MembroGerenciar) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.gerenciar_remover_titulo))
            .setMessage(getString(R.string.gerenciar_remover_msg, membro.nome))
            .setPositiveButton(getString(R.string.gerenciar_opcoes_remover)) { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.removerMembro(empresaId, membro.uid)
                    if (ok) {
                        Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_removido), Toast.LENGTH_SHORT).show()
                        carregarTudo()
                    }
                }
            }
            .setNegativeButton(getString(R.string.geral_cancelar), null)
            .show()
    }

    private fun confirmarExcluirEmpresa() {
        val empresa = empresaAtual ?: return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.gerenciar_excluir_titulo))
            .setMessage(getString(R.string.gerenciar_excluir_msg, empresa.nome))
            .setPositiveButton(getString(R.string.perfil_excluir_btn)) { _, _ -> executarExclusao() }
            .setNegativeButton(getString(R.string.geral_cancelar), null)
            .show()
    }

    private fun executarExclusao() {
        Toast.makeText(this, getString(R.string.gerenciar_excluindo), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val ok = repository.deletarEmpresa(empresaId)

            if (ok) {
                // Notifica membros (Texto dinâmico, deixaremos a tradução para a próxima fase de Notificações)
                empresaAtual?.membros?.forEach { uid ->
                    if (uid != auth.currentUser?.uid) {
                        NotificacaoHelper.enviar(
                            uidDestino = uid,
                            titulo = getString(R.string.gerenciar_excluida),
                            mensagem = "A empresa '${empresaAtual?.nome}' foi excluída pelo dono."
                        )
                    }
                }

                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_excluida), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@GerenciarEmpresaActivity, getString(R.string.gerenciar_erro_excluir), Toast.LENGTH_SHORT).show()
            }
        }
    }
}