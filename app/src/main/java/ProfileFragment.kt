package com.example.plataformaremota

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.example.plataformaremota.data.entity.LinkContato
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class ProfileFragment : Fragment() {

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var cardAvatarPerfil: MaterialCardView
    private lateinit var txtNomePerfil: TextView
    private lateinit var txtEmailPerfil: TextView
    private lateinit var txtProfissaoPerfil: TextView
    private lateinit var txtBioPerfil: TextView
    private lateinit var txtStatusNotificacoes: TextView

    // Status
    private lateinit var cardStatusPerfil: MaterialCardView
    private lateinit var dotStatusPerfil: View
    private lateinit var txtStatusPerfil: TextView
    private var statusAtual: String = ThemeManager.STATUS_ONLINE

    // Links
    private lateinit var containerLinksPerfil: LinearLayout
    private lateinit var txtSemLinks: TextView
    private var linksAtuais: MutableList<LinkContato> = mutableListOf()

    // Currículo
    private lateinit var btnAnexarCurriculo: View
    private lateinit var containerCurriculoAnexado: View
    private lateinit var txtNomeCurriculo: TextView
    private lateinit var btnRemoverCurriculo: ImageView
    private var curriculoUrlAtual: String = ""
    private var curriculoNomeAtual: String = ""

    // ⭐ Avaliações
    private lateinit var txtTituloAvaliacoesPerfil: TextView
    private lateinit var cardAvaliacoesPerfil: MaterialCardView
    private lateinit var txtMediaAvaliacoesPerfil: TextView
    private lateinit var linhaEstrelasMediaPerfil: LinearLayout
    private lateinit var txtTotalAvaliacoesPerfil: TextView
    private lateinit var containerAvaliacoesPerfil: LinearLayout

    // Conta
    private lateinit var btnLogoutPerfil: View
    private lateinit var btnExcluirConta: View

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""
    private var emailUsuario: String = ""
    private var profissaoUsuario: String = ""
    private var bioUsuario: String = ""
    private var fotoUrlAtual: String = ""

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val PREFS_NAME = "integra_prefs"
    private val KEY_NOTIFICACOES = "notificacoes_ativas"
    private val KEY_FOTO_PERFIL = "foto_perfil"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && isAdded) salvarImagem(uri)
    }

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && isAdded) uploadCurriculo(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgFotoPerfil = view.findViewById(R.id.imgFotoPerfil)
        cardAvatarPerfil = view.findViewById(R.id.cardAvatarPerfil)
        txtNomePerfil = view.findViewById(R.id.txtNomePerfil)
        txtEmailPerfil = view.findViewById(R.id.txtEmailPerfil)
        txtProfissaoPerfil = view.findViewById(R.id.txtProfissaoPerfil)
        txtBioPerfil = view.findViewById(R.id.txtBioPerfil)
        txtStatusNotificacoes = view.findViewById(R.id.txtStatusNotificacoes)

        cardStatusPerfil = view.findViewById(R.id.cardStatusPerfil)
        dotStatusPerfil = view.findViewById(R.id.dotStatusPerfil)
        txtStatusPerfil = view.findViewById(R.id.txtStatusPerfil)

        containerLinksPerfil = view.findViewById(R.id.containerLinksPerfil)
        txtSemLinks = view.findViewById(R.id.txtSemLinks)

        btnAnexarCurriculo = view.findViewById(R.id.btnAnexarCurriculo)
        containerCurriculoAnexado = view.findViewById(R.id.containerCurriculoAnexado)
        txtNomeCurriculo = view.findViewById(R.id.txtNomeCurriculo)
        btnRemoverCurriculo = view.findViewById(R.id.btnRemoverCurriculo)

        txtTituloAvaliacoesPerfil = view.findViewById(R.id.txtTituloAvaliacoesPerfil)
        cardAvaliacoesPerfil = view.findViewById(R.id.cardAvaliacoesPerfil)
        txtMediaAvaliacoesPerfil = view.findViewById(R.id.txtMediaAvaliacoesPerfil)
        linhaEstrelasMediaPerfil = view.findViewById(R.id.linhaEstrelasMediaPerfil)
        txtTotalAvaliacoesPerfil = view.findViewById(R.id.txtTotalAvaliacoesPerfil)
        containerAvaliacoesPerfil = view.findViewById(R.id.containerAvaliacoesPerfil)

        btnLogoutPerfil = view.findViewById(R.id.btnLogoutPerfil)
        btnExcluirConta = view.findViewById(R.id.btnExcluirConta)

        usuarioId = arguments?.getString("usuarioId") ?: auth.currentUser?.uid ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        txtNomePerfil.text = nomeUsuario.ifEmpty { "Carregando..." }

        carregarDadosUsuario()
        carregarFotoSalva()
        atualizarStatusNotificacoes()

        cardAvatarPerfil.setOnClickListener { abrirBottomSheetFoto() }
        cardStatusPerfil.setOnClickListener { abrirDialogStatus() }

        view.findViewById<View>(R.id.opcaoEditarPerfil).setOnClickListener {
            abrirDialogEditarPerfil()
        }
        view.findViewById<View>(R.id.opcaoNotificacoes).setOnClickListener {
            abrirDialogNotificacoes()
        }
        view.findViewById<View>(R.id.opcaoTema).setOnClickListener {
            abrirDialogTemas()
        }
        view.findViewById<View>(R.id.opcaoSobre).setOnClickListener {
            abrirDialogSobre()
        }
        view.findViewById<View>(R.id.btnAddLink).setOnClickListener {
            abrirDialogAddLink()
        }
        view.findViewById<View>(R.id.btnVerVisualizacoes).setOnClickListener {
            startActivity(Intent(requireContext(), VisualizacoesActivity::class.java))
        }

        btnAnexarCurriculo.setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }
        containerCurriculoAnexado.setOnClickListener {
            if (curriculoUrlAtual.isNotEmpty()) abrirCurriculo()
        }
        btnRemoverCurriculo.setOnClickListener { confirmarRemoverCurriculo() }

        btnLogoutPerfil.setOnClickListener { fazerLogout() }
        btnExcluirConta.setOnClickListener { confirmarExcluirConta() }

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
    }

    private fun carregarDadosUsuario() {
        val uid = auth.currentUser?.uid ?: usuarioId
        if (uid.isEmpty()) {
            txtNomePerfil.text = "Usuário não identificado"
            return
        }
        usuarioId = uid

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()

                if (!isAdded || view == null) return@launch

                if (doc.exists()) {
                    nomeUsuario = doc.getString("nome") ?: "Usuário"
                    emailUsuario = doc.getString("email") ?: ""
                    profissaoUsuario = doc.getString("profissao") ?: ""
                    bioUsuario = doc.getString("bio") ?: ""
                    fotoUrlAtual = doc.getString("fotoUrl") ?: ""
                    statusAtual = doc.getString("status") ?: ThemeManager.STATUS_ONLINE
                    curriculoUrlAtual = doc.getString("curriculoUrl") ?: ""
                    curriculoNomeAtual = doc.getString("curriculoNome") ?: "curriculo.pdf"

                    txtNomePerfil.text = nomeUsuario
                    txtEmailPerfil.text = emailUsuario.ifEmpty { "—" }
                    txtProfissaoPerfil.text = profissaoUsuario.ifEmpty { "—" }
                    txtBioPerfil.text = if (bioUsuario.isEmpty()) "Sem bio ainda" else bioUsuario

                    atualizarUiStatus()
                    atualizarUiCurriculo()
                    carregarLinks(doc)
                    carregarAvaliacoesRecebidas()

                    if (fotoUrlAtual.isNotEmpty()) {
                        carregarFotoRemota(fotoUrlAtual)
                    }
                } else {
                    txtNomePerfil.text = nomeUsuario.ifEmpty { "Usuário" }
                    txtEmailPerfil.text = auth.currentUser?.email ?: "—"
                    txtProfissaoPerfil.text = "—"
                    txtBioPerfil.text = "Sem bio ainda"
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // ⭐ AVALIAÇÕES RECEBIDAS
    // ─────────────────────────────────────────────
    private fun carregarAvaliacoesRecebidas() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val snap = db.collection("avaliacoes")
                    .whereEqualTo("alvoId", usuarioId)
                    .whereEqualTo("alvoTipo", "usuario")
                    .get().await()

                val avaliacoes = snap.documents.mapNotNull {
                    it.toObject(com.example.plataformaremota.data.entity.Avaliacao::class.java)
                }.sortedByDescending { it.timestamp }

                if (!isAdded) return@launch

                if (avaliacoes.isEmpty()) {
                    txtTituloAvaliacoesPerfil.visibility = View.GONE
                    cardAvaliacoesPerfil.visibility = View.GONE
                    return@launch
                }

                txtTituloAvaliacoesPerfil.visibility = View.VISIBLE
                cardAvaliacoesPerfil.visibility = View.VISIBLE

                val media = avaliacoes.map { it.estrelas }.average()
                txtMediaAvaliacoesPerfil.text = String.format("%.1f", media)
                txtTotalAvaliacoesPerfil.text =
                    "(${avaliacoes.size} ${if (avaliacoes.size == 1) "avaliação" else "avaliações"})"

                linhaEstrelasMediaPerfil.removeAllViews()
                for (i in 1..5) {
                    val iv = ImageView(requireContext())
                    val size = (18 * resources.displayMetrics.density).toInt()
                    val params = LinearLayout.LayoutParams(size, size)
                    params.marginEnd = (2 * resources.displayMetrics.density).toInt()
                    iv.layoutParams = params
                    if (i <= media.roundToInt()) {
                        iv.setImageResource(R.drawable.ic_star_filled)
                        iv.imageTintList = ColorStateList.valueOf(0xFFFFC107.toInt())
                    } else {
                        iv.setImageResource(R.drawable.ic_star_outline)
                        iv.imageTintList = ColorStateList.valueOf(0xFFBDBDBD.toInt())
                    }
                    linhaEstrelasMediaPerfil.addView(iv)
                }

                containerAvaliacoesPerfil.removeAllViews()
                for (av in avaliacoes) {
                    val itemView = layoutInflater.inflate(
                        R.layout.item_avaliacao, containerAvaliacoesPerfil, false
                    )
                    itemView.findViewById<TextView>(R.id.txtAutorAvaliacao).text = av.autorNome
                    itemView.findViewById<TextView>(R.id.txtComentarioAvaliacao).text =
                        if (av.comentario.isEmpty()) "(sem comentário)" else av.comentario

                    val ctx = itemView.findViewById<TextView>(R.id.txtContextoAvaliacao)
                    if (av.trabalhoTitulo.isNotEmpty()) {
                        ctx.visibility = View.VISIBLE
                        ctx.text = "Sobre: ${av.trabalhoTitulo}"
                    }

                    val linha = itemView.findViewById<LinearLayout>(R.id.linhaEstrelasItem)
                    for (i in 1..5) {
                        val iv = ImageView(requireContext())
                        val size = (14 * resources.displayMetrics.density).toInt()
                        val params = LinearLayout.LayoutParams(size, size)
                        iv.layoutParams = params
                        if (i <= av.estrelas) {
                            iv.setImageResource(R.drawable.ic_star_filled)
                            iv.imageTintList = ColorStateList.valueOf(0xFFFFC107.toInt())
                        } else {
                            iv.setImageResource(R.drawable.ic_star_outline)
                            iv.imageTintList = ColorStateList.valueOf(0xFFBDBDBD.toInt())
                        }
                        linha.addView(iv)
                    }

                    containerAvaliacoesPerfil.addView(itemView)
                }
            } catch (_: Exception) {
                txtTituloAvaliacoesPerfil.visibility = View.GONE
                cardAvaliacoesPerfil.visibility = View.GONE
            }
        }
    }

    // ─────────────────────────────────────────────
    // CURRÍCULO
    // ─────────────────────────────────────────────
    private fun atualizarUiCurriculo() {
        if (curriculoUrlAtual.isEmpty()) {
            btnAnexarCurriculo.visibility = View.VISIBLE
            containerCurriculoAnexado.visibility = View.GONE
        } else {
            btnAnexarCurriculo.visibility = View.GONE
            containerCurriculoAnexado.visibility = View.VISIBLE
            txtNomeCurriculo.text = curriculoNomeAtual.ifEmpty { "curriculo.pdf" }
        }
    }

    private fun uploadCurriculo(uri: Uri) {
        if (!isAdded) return
        Toast.makeText(requireContext(), "Enviando currículo...", Toast.LENGTH_SHORT).show()

        val nome = obterNomeArquivo(uri) ?: "curriculo.pdf"

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "curriculos")
                .option("resource_type", "raw")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        if (!isAdded) return

                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                db.collection("usuarios").document(usuarioId)
                                    .update(mapOf(
                                        "curriculoUrl" to secureUrl,
                                        "curriculoNome" to nome
                                    )).await()
                                if (!isAdded) return@launch
                                curriculoUrlAtual = secureUrl
                                curriculoNomeAtual = nome
                                atualizarUiCurriculo()
                                Toast.makeText(requireContext(), "Currículo anexado!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        if (isAdded) Toast.makeText(requireContext(), "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
                })
                .dispatch()
        } catch (e: Exception) {
            if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun obterNomeArquivo(uri: Uri): String? {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) it.getString(idx) else null
                } else null
            }
        } catch (e: Exception) { null }
    }

    private fun abrirCurriculo() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(curriculoUrlAtual)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Nenhum app pra abrir PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarRemoverCurriculo() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remover currículo")
            .setMessage("Remover o currículo anexado?")
            .setPositiveButton("Remover") { _, _ -> removerCurriculo() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removerCurriculo() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("usuarios").document(usuarioId)
                    .update(mapOf("curriculoUrl" to "", "curriculoNome" to "")).await()
                curriculoUrlAtual = ""
                curriculoNomeAtual = ""
                atualizarUiCurriculo()
                Toast.makeText(requireContext(), "Currículo removido", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // FOTO
    // ─────────────────────────────────────────────
    private fun carregarFotoRemota(url: String) {
        if (!isAdded) return
        Glide.with(this).load(url).circleCrop().into(imgFotoPerfil)
        imgFotoPerfil.imageTintList = null
        imgFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
        imgFotoPerfil.setPadding(0, 0, 0, 0)
    }

    private fun abrirBottomSheetFoto() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_foto_perfil, null)

        sheetView.findViewById<View>(R.id.opcaoGaleria).setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            dialog.dismiss()
        }
        sheetView.findViewById<View>(R.id.opcaoRemover).setOnClickListener {
            removerFoto()
            dialog.dismiss()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun salvarImagem(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            val arquivo = File(requireContext().filesDir, "perfil_foto.jpg")
            val outputStream = FileOutputStream(arquivo)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            imgFotoPerfil.setImageURI(uri)
            imgFotoPerfil.imageTintList = null
            imgFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
            imgFotoPerfil.setPadding(0, 0, 0, 0)
            uploadParaCloudinary(uri)
        } catch (e: Exception) {
            if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadParaCloudinary(uri: Uri) {
        if (!isAdded) return
        Toast.makeText(requireContext(), "Enviando foto...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "perfis")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String ?: return
                        val secureUrl = url.replace("http://", "https://")
                        if (!isAdded) return

                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                db.collection("usuarios").document(usuarioId)
                                    .update("fotoUrl", secureUrl).await()
                                if (!isAdded) return@launch
                                fotoUrlAtual = secureUrl
                                Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        if (isAdded) Toast.makeText(requireContext(), "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
                })
                .dispatch()
        } catch (e: Exception) {
            if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun carregarFotoSalva() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_FOTO_PERFIL, null)

        if (path != null && fotoUrlAtual.isEmpty()) {
            val arquivo = File(path)
            if (arquivo.exists()) {
                imgFotoPerfil.setImageURI(Uri.fromFile(arquivo))
                imgFotoPerfil.imageTintList = null
                imgFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
                imgFotoPerfil.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun removerFoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("usuarios").document(usuarioId).update("fotoUrl", "").await()
                if (!isAdded) return@launch

                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val path = prefs.getString(KEY_FOTO_PERFIL, null)
                if (path != null) {
                    val arquivo = File(path)
                    if (arquivo.exists()) arquivo.delete()
                }
                prefs.edit().remove(KEY_FOTO_PERFIL).apply()
                fotoUrlAtual = ""

                imgFotoPerfil.setImageResource(R.drawable.ic_person)
                imgFotoPerfil.imageTintList = ColorStateList.valueOf(Color.WHITE)
                imgFotoPerfil.scaleType = ImageView.ScaleType.FIT_CENTER
                val padding = (18 * resources.displayMetrics.density).toInt()
                imgFotoPerfil.setPadding(padding, padding, padding, padding)

                Toast.makeText(requireContext(), "Foto removida", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // STATUS
    // ─────────────────────────────────────────────
    private fun atualizarUiStatus() {
        val cor = ThemeManager.getStatusColor(statusAtual)
        dotStatusPerfil.backgroundTintList = ColorStateList.valueOf(cor)
        txtStatusPerfil.text = ThemeManager.getStatusNomeAmigavel(statusAtual)
    }

    private fun abrirDialogStatus() {
        val opcoes = arrayOf(
            ThemeManager.STATUS_ONLINE,
            ThemeManager.STATUS_AUSENTE,
            ThemeManager.STATUS_NAO_PERTUBE,
            ThemeManager.STATUS_INVISIVEL
        )
        val nomes = opcoes.map { ThemeManager.getStatusNomeAmigavel(it) }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Escolher status")
            .setItems(nomes) { _, which -> salvarStatus(opcoes[which]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarStatus(novoStatus: String) {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("usuarios").document(usuarioId)
                    .update("status", novoStatus).await()
                statusAtual = novoStatus
                atualizarUiStatus()
                Toast.makeText(requireContext(), "Status atualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // LINKS
    // ─────────────────────────────────────────────
    private fun carregarLinks(doc: com.google.firebase.firestore.DocumentSnapshot) {
        linksAtuais.clear()
        try {
            val listaRaw = doc.get("links") as? List<*>
            listaRaw?.forEach { item ->
                @Suppress("UNCHECKED_CAST")
                val map = item as? Map<String, Any?> ?: return@forEach
                val tipo = map["tipo"] as? String ?: return@forEach
                val valor = map["valor"] as? String ?: return@forEach
                linksAtuais.add(LinkContato(tipo, valor))
            }
        } catch (_: Exception) { }
        renderizarLinks()
    }

    private fun renderizarLinks() {
        containerLinksPerfil.removeAllViews()

        if (linksAtuais.isEmpty()) {
            txtSemLinks.visibility = View.VISIBLE
            return
        }
        txtSemLinks.visibility = View.GONE

        for (link in linksAtuais) {
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_link_contato, containerLinksPerfil, false)

            val imgIcone = itemView.findViewById<ImageView>(R.id.imgLinkIcone)
            val txtTipo = itemView.findViewById<TextView>(R.id.txtLinkTipo)
            val txtValor = itemView.findViewById<TextView>(R.id.txtLinkValor)
            val btnRemover = itemView.findViewById<ImageView>(R.id.btnRemoverLink)

            txtTipo.text = nomeTipoLink(link.tipo)
            txtValor.text = link.valor
            imgIcone.setImageResource(iconeTipoLink(link.tipo))

            itemView.setOnClickListener { abrirLink(link) }
            btnRemover.setOnClickListener { confirmarRemoverLink(link) }

            containerLinksPerfil.addView(itemView)
        }
    }

    private fun nomeTipoLink(tipo: String): String = when (tipo.lowercase()) {
        "whatsapp" -> "WhatsApp"
        "linkedin" -> "LinkedIn"
        "github" -> "GitHub"
        "instagram" -> "Instagram"
        "site" -> "Site"
        "email" -> "E-mail"
        else -> tipo.replaceFirstChar { it.uppercase() }
    }

    private fun iconeTipoLink(tipo: String): Int = when (tipo.lowercase()) {
        "whatsapp" -> android.R.drawable.ic_dialog_email
        "linkedin" -> android.R.drawable.ic_menu_share
        "github" -> android.R.drawable.ic_menu_manage
        "instagram" -> android.R.drawable.ic_menu_camera
        "site" -> android.R.drawable.ic_menu_view
        "email" -> android.R.drawable.ic_dialog_email
        else -> android.R.drawable.ic_menu_share
    }

    private fun abrirLink(link: LinkContato) {
        val url = when (link.tipo.lowercase()) {
            "whatsapp" -> "https://wa.me/${link.valor.filter { it.isDigit() }}"
            "linkedin" -> if (link.valor.startsWith("http")) link.valor else "https://linkedin.com/in/${link.valor}"
            "github" -> if (link.valor.startsWith("http")) link.valor else "https://github.com/${link.valor}"
            "instagram" -> if (link.valor.startsWith("http")) link.valor else "https://instagram.com/${link.valor}"
            "site" -> if (link.valor.startsWith("http")) link.valor else "https://${link.valor}"
            "email" -> "mailto:${link.valor}"
            else -> if (link.valor.startsWith("http")) link.valor else "https://${link.valor}"
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Nenhum app encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarRemoverLink(link: LinkContato) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remover link")
            .setMessage("Remover o link de ${nomeTipoLink(link.tipo)}?")
            .setPositiveButton("Remover") { _, _ -> removerLink(link) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removerLink(link: LinkContato) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val novos = linksAtuais.toMutableList().apply {
                    removeAll { it.tipo == link.tipo && it.valor == link.valor }
                }
                val listaFirestore = novos.map { mapOf("tipo" to it.tipo, "valor" to it.valor) }
                db.collection("usuarios").document(usuarioId)
                    .update("links", listaFirestore).await()
                linksAtuais = novos
                renderizarLinks()
                Toast.makeText(requireContext(), "Link removido", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirDialogAddLink() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_link, null)

        val edtTipo = dialogView.findViewById<AutoCompleteTextView>(R.id.edtTipoLink)
        val edtValor = dialogView.findViewById<EditText>(R.id.edtValorLink)

        val tipos = arrayOf("WhatsApp", "LinkedIn", "GitHub", "Instagram", "Site", "E-mail")
        edtTipo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos))

        AlertDialog.Builder(requireContext())
            .setTitle("Adicionar link")
            .setView(dialogView)
            .setPositiveButton("Adicionar") { _, _ ->
                val tipo = edtTipo.text.toString().trim().lowercase()
                val valor = edtValor.text.toString().trim()
                if (tipo.isEmpty() || valor.isEmpty()) {
                    Toast.makeText(requireContext(), "Preencha os dois campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                salvarLink(LinkContato(tipo, valor))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarLink(link: LinkContato) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val novos = linksAtuais.toMutableList().apply { add(link) }
                val listaFirestore = novos.map { mapOf("tipo" to it.tipo, "valor" to it.valor) }
                db.collection("usuarios").document(usuarioId)
                    .update("links", listaFirestore).await()
                linksAtuais = novos
                renderizarLinks()
                Toast.makeText(requireContext(), "Link adicionado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // CONFIGURAÇÕES
    // ─────────────────────────────────────────────
    private fun abrirDialogTemas() {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_temas, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val temas = mapOf(
            R.id.temaPadrao to ThemeManager.TEMA_PADRAO,
            R.id.temaVermelho to ThemeManager.TEMA_VERMELHO,
            R.id.temaAzul to ThemeManager.TEMA_AZUL,
            R.id.temaVerde to ThemeManager.TEMA_VERDE,
            R.id.temaPreto to ThemeManager.TEMA_PRETO,
            R.id.temaBranco to ThemeManager.TEMA_BRANCO,
            R.id.temaAmarelo to ThemeManager.TEMA_AMARELO
        )

        for ((id, tema) in temas) {
            dialogView.findViewById<View>(id).setOnClickListener {
                ThemeManager.setTema(requireContext(), tema)
                dialog.dismiss()
                requireActivity().recreate()
            }
        }
        dialog.show()
    }

    private fun abrirDialogEditarPerfil() {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_perfil, null)

        val edtNome = dialogView.findViewById<EditText>(R.id.edtDialogNome)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtDialogEmail)
        val edtProfissao = dialogView.findViewById<EditText>(R.id.edtDialogProfissao)
        val edtBio = dialogView.findViewById<EditText>(R.id.edtDialogBio)

        edtNome.setText(nomeUsuario)
        edtEmail.setText(emailUsuario)
        edtProfissao.setText(profissaoUsuario)
        edtBio.setText(bioUsuario)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar perfil")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                salvarEdicao(
                    edtNome.text.toString().trim(),
                    edtEmail.text.toString().trim(),
                    edtProfissao.text.toString().trim(),
                    edtBio.text.toString().trim()
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarEdicao(nome: String, email: String, profissao: String, bio: String) {
        if (nome.isEmpty() || email.isEmpty() || profissao.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("usuarios").document(usuarioId)
                    .update(mapOf(
                        "nome" to nome,
                        "email" to email,
                        "profissao" to profissao,
                        "bio" to bio
                    )).await()
                if (!isAdded) return@launch
                nomeUsuario = nome
                emailUsuario = email
                profissaoUsuario = profissao
                bioUsuario = bio
                txtNomePerfil.text = nome
                txtEmailPerfil.text = email
                txtProfissaoPerfil.text = profissao
                txtBioPerfil.text = if (bio.isEmpty()) "Sem bio ainda" else bio
                Toast.makeText(requireContext(), "Perfil atualizado!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun abrirDialogNotificacoes() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ativoAtual = prefs.getBoolean(KEY_NOTIFICACOES, true)

        val switch = SwitchMaterial(requireContext()).apply {
            text = "Receber notificações"
            isChecked = ativoAtual
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Notificações")
            .setView(switch)
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit().putBoolean(KEY_NOTIFICACOES, switch.isChecked).apply()
                atualizarStatusNotificacoes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarStatusNotificacoes() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ativo = prefs.getBoolean(KEY_NOTIFICACOES, true)
        txtStatusNotificacoes.text = if (ativo) "Ativado" else "Desativado"
    }

    private fun abrirDialogSobre() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Sobre o Integra")
            .setMessage(
                "Integra\nVersão 1.0\n\n" +
                        "Plataforma de trabalhos remotos.\n\n" +
                        "Conecte-se a oportunidades e publique seus trabalhos."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun confirmarExcluirConta() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Excluir conta")
            .setMessage(
                "Tem CERTEZA que quer excluir sua conta?\n\n" +
                        "Isso vai apagar PERMANENTEMENTE:\n" +
                        "• Seu perfil\n" +
                        "• Todos os seus trabalhos publicados\n" +
                        "• Todas as suas candidaturas\n" +
                        "• Sua participação em chats e grupos\n\n" +
                        "Essa ação NÃO pode ser desfeita!"
            )
            .setPositiveButton("EXCLUIR TUDO") { _, _ ->
                HapticHelper.destrutiva(requireContext())
                executarExclusaoConta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarExclusaoConta() {
        val uid = auth.currentUser?.uid ?: return
        if (isAdded) Toast.makeText(requireContext(), "Excluindo conta...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val trabalhos = db.collection("trabalhos")
                    .whereEqualTo("criadorId", uid).get().await()
                for (t in trabalhos.documents) {
                    val cands = t.reference.collection("candidaturas").get().await()
                    for (c in cands.documents) c.reference.delete().await()
                    t.reference.delete().await()
                }

                val todosTrabalhos = db.collection("trabalhos").get().await()
                for (t in todosTrabalhos.documents) {
                    try {
                        t.reference.collection("candidaturas").document(uid).delete().await()
                    } catch (_: Exception) { }
                }

                val chats = db.collection("chats")
                    .whereArrayContains("participantes", uid).get().await()
                for (c in chats.documents) {
                    val participantes = (c.get("participantes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val admins = (c.get("admins") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val novosParticipantes = participantes.filter { it != uid }
                    val novosAdmins = admins.filter { it != uid }
                    if (novosParticipantes.isEmpty()) {
                        c.reference.delete().await()
                    } else {
                        c.reference.update(mapOf(
                            "participantes" to novosParticipantes,
                            "admins" to novosAdmins
                        )).await()
                    }
                }

                db.collection("usuarios").document(uid).delete().await()
                try { auth.currentUser?.delete()?.await() } catch (_: Exception) { }
                auth.signOut()

                if (isAdded) Toast.makeText(requireContext(), "Conta excluída", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}