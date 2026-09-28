package com.example.plataformaremota

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var cardAvatarPerfil: MaterialCardView
    private lateinit var txtNomePerfil: TextView
    private lateinit var txtEmailPerfil: TextView
    private lateinit var txtProfissaoPerfil: TextView
    private lateinit var txtStatusNotificacoes: TextView
    private lateinit var btnLogoutPerfil: MaterialButton
    private lateinit var btnExcluirConta: MaterialButton

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""
    private var emailUsuario: String = ""
    private var profissaoUsuario: String = ""
    private var fotoUrlAtual: String = ""

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val PREFS_NAME = "integra_prefs"
    private val KEY_NOTIFICACOES = "notificacoes_ativas"
    private val KEY_FOTO_PERFIL = "foto_perfil"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            salvarImagem(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        txtStatusNotificacoes = view.findViewById(R.id.txtStatusNotificacoes)
        btnLogoutPerfil = view.findViewById(R.id.btnLogoutPerfil)
        btnExcluirConta = view.findViewById(R.id.btnExcluirConta)

        usuarioId = arguments?.getString("usuarioId") ?: auth.currentUser?.uid ?: ""
        nomeUsuario = arguments?.getString("nomeUsuario") ?: ""

        txtNomePerfil.text = nomeUsuario.ifEmpty { "Carregando..." }

        carregarDadosUsuario()
        carregarFotoSalva()
        atualizarStatusNotificacoes()

        cardAvatarPerfil.setOnClickListener { abrirBottomSheetFoto() }

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

        btnLogoutPerfil.setOnClickListener { fazerLogout() }
        btnExcluirConta.setOnClickListener { confirmarExcluirConta() }

        // Aplica cor do tema
        ThemeManager.aplicarCores(requireContext(), view)
    }

    // ─────────────────────────────────────────────
    // CARREGAR DADOS DO FIRESTORE
    // ─────────────────────────────────────────────
    private fun carregarDadosUsuario() {
        val uid = auth.currentUser?.uid ?: usuarioId
        if (uid.isEmpty()) {
            txtNomePerfil.text = "Usuário não identificado"
            return
        }
        usuarioId = uid

        lifecycleScope.launch {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()

                if (doc.exists()) {
                    nomeUsuario = doc.getString("nome") ?: "Usuário"
                    emailUsuario = doc.getString("email") ?: ""
                    profissaoUsuario = doc.getString("profissao") ?: ""
                    fotoUrlAtual = doc.getString("fotoUrl") ?: ""

                    txtNomePerfil.text = nomeUsuario
                    txtEmailPerfil.text = emailUsuario.ifEmpty { "—" }
                    txtProfissaoPerfil.text = profissaoUsuario.ifEmpty { "—" }

                    if (fotoUrlAtual.isNotEmpty()) {
                        carregarFotoRemota(fotoUrlAtual)
                    }
                } else {
                    txtNomePerfil.text = nomeUsuario.ifEmpty { "Usuário" }
                    txtEmailPerfil.text = auth.currentUser?.email ?: "—"
                    txtProfissaoPerfil.text = "—"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarFotoRemota(url: String) {
        if (!isAdded) return

        Glide.with(this)
            .load(url)
            .circleCrop()
            .into(imgFotoPerfil)

        imgFotoPerfil.imageTintList = null
        imgFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
        imgFotoPerfil.setPadding(0, 0, 0, 0)
    }

    // ─────────────────────────────────────────────
    // FOTO DE PERFIL
    // ─────────────────────────────────────────────
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
            Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadParaCloudinary(uri: Uri) {
        Toast.makeText(requireContext(), "Enviando foto...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "perfis")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        lifecycleScope.launch {
                            try {
                                db.collection("usuarios").document(usuarioId)
                                    .update("fotoUrl", secureUrl)
                                    .await()

                                fotoUrlAtual = secureUrl
                                Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        Toast.makeText(requireContext(), "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun carregarFotoSalva() {
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
        lifecycleScope.launch {
            try {
                db.collection("usuarios").document(usuarioId)
                    .update("fotoUrl", "")
                    .await()

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
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // DIALOG: TEMA
    // ─────────────────────────────────────────────
    private fun abrirDialogTemas() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_temas, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.temaPadrao).setOnClickListener {
            ThemeManager.setTema(requireContext(), ThemeManager.TEMA_PADRAO)
            dialog.dismiss()
            requireActivity().recreate()
        }
        dialogView.findViewById<View>(R.id.temaVermelho).setOnClickListener {
            ThemeManager.setTema(requireContext(), ThemeManager.TEMA_VERMELHO)
            dialog.dismiss()
            requireActivity().recreate()
        }
        dialogView.findViewById<View>(R.id.temaAzul).setOnClickListener {
            ThemeManager.setTema(requireContext(), ThemeManager.TEMA_AZUL)
            dialog.dismiss()
            requireActivity().recreate()
        }
        dialogView.findViewById<View>(R.id.temaVerde).setOnClickListener {
            ThemeManager.setTema(requireContext(), ThemeManager.TEMA_VERDE)
            dialog.dismiss()
            requireActivity().recreate()
        }

        dialog.show()
    }

    // ─────────────────────────────────────────────
    // DIALOG: EDITAR PERFIL
    // ─────────────────────────────────────────────
    private fun abrirDialogEditarPerfil() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_perfil, null)

        val edtNome = dialogView.findViewById<EditText>(R.id.edtDialogNome)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtDialogEmail)
        val edtProfissao = dialogView.findViewById<EditText>(R.id.edtDialogProfissao)

        edtNome.setText(nomeUsuario)
        edtEmail.setText(emailUsuario)
        edtProfissao.setText(profissaoUsuario)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar perfil")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                salvarEdicao(
                    edtNome.text.toString().trim(),
                    edtEmail.text.toString().trim(),
                    edtProfissao.text.toString().trim()
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarEdicao(nome: String, email: String, profissao: String) {
        if (nome.isEmpty() || email.isEmpty() || profissao.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                db.collection("usuarios").document(usuarioId)
                    .update(mapOf(
                        "nome" to nome,
                        "email" to email,
                        "profissao" to profissao
                    ))
                    .await()

                nomeUsuario = nome
                emailUsuario = email
                profissaoUsuario = profissao

                txtNomePerfil.text = nome
                txtEmailPerfil.text = email
                txtProfissaoPerfil.text = profissao

                Toast.makeText(requireContext(), "Perfil atualizado!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // DIALOG: NOTIFICAÇÕES
    // ─────────────────────────────────────────────
    private fun abrirDialogNotificacoes() {
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
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ativo = prefs.getBoolean(KEY_NOTIFICACOES, true)
        txtStatusNotificacoes.text = if (ativo) "Ativado" else "Desativado"
    }

    // ─────────────────────────────────────────────
    // DIALOG: SOBRE
    // ─────────────────────────────────────────────
    private fun abrirDialogSobre() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sobre o Integra.app")
            .setMessage(
                "Integra.app\nVersão 1.0\n\n" +
                        "Plataforma de trabalhos remotos.\n\n" +
                        "Conecte-se a oportunidades e publique seus trabalhos."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // ─────────────────────────────────────────────
    // SAIR DA CONTA
    // ─────────────────────────────────────────────
    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    // ─────────────────────────────────────────────
    // EXCLUIR CONTA
    // ─────────────────────────────────────────────
    private fun confirmarExcluirConta() {
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
                executarExclusaoConta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarExclusaoConta() {
        val uid = auth.currentUser?.uid ?: return

        Toast.makeText(requireContext(), "Excluindo conta...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // 1. Apaga trabalhos do usuário (com as candidaturas dentro)
                val trabalhos = db.collection("trabalhos")
                    .whereEqualTo("criadorId", uid)
                    .get().await()

                for (t in trabalhos.documents) {
                    val cands = t.reference.collection("candidaturas").get().await()
                    for (c in cands.documents) {
                        c.reference.delete().await()
                    }
                    t.reference.delete().await()
                }

                // 2. Apaga candidaturas em trabalhos de OUTROS
                val todosTrabalhos = db.collection("trabalhos").get().await()
                for (t in todosTrabalhos.documents) {
                    try {
                        t.reference.collection("candidaturas").document(uid).delete().await()
                    } catch (_: Exception) { }
                }

                // 3. Remove dos chats/grupos
                val chats = db.collection("chats")
                    .whereArrayContains("participantes", uid)
                    .get().await()

                for (c in chats.documents) {
                    val participantes = (c.get("participantes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val admins = (c.get("admins") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    val novosParticipantes = participantes.filter { it != uid }
                    val novosAdmins = admins.filter { it != uid }

                    if (novosParticipantes.isEmpty()) {
                        c.reference.delete().await()
                    } else {
                        c.reference.update(
                            mapOf(
                                "participantes" to novosParticipantes,
                                "admins" to novosAdmins
                            )
                        ).await()
                    }
                }

                // 4. Apaga o documento do usuário
                db.collection("usuarios").document(uid).delete().await()

                // 5. Tenta apagar do Firebase Auth
                try {
                    auth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    // Não conseguiu apagar do Auth — dados já foram
                }

                // 6. Logout e volta pro login
                auth.signOut()
                Toast.makeText(requireContext(), "Conta excluída", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erro ao excluir: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}