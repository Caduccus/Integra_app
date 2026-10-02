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
    private lateinit var txtBioPerfil: TextView
    private lateinit var txtStatusNotificacoes: TextView
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
        if (uri != null && isAdded) {
            salvarImagem(uri)
        }
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

                    txtNomePerfil.text = nomeUsuario
                    txtEmailPerfil.text = emailUsuario.ifEmpty { "—" }
                    txtProfissaoPerfil.text = profissaoUsuario.ifEmpty { "—" }
                    txtBioPerfil.text = if (bioUsuario.isEmpty()) "Sem bio ainda" else bioUsuario

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
            if (isAdded) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        if (!isAdded) return

                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                db.collection("usuarios").document(usuarioId)
                                    .update("fotoUrl", secureUrl)
                                    .await()

                                if (!isAdded) return@launch
                                fotoUrlAtual = secureUrl
                                Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                if (isAdded) {
                                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                db.collection("usuarios").document(usuarioId)
                    .update("fotoUrl", "")
                    .await()

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
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirDialogTemas() {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_temas, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

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
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_perfil, null)

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
                    ))
                    .await()

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
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
            .setTitle("Sobre o Integra.app")
            .setMessage(
                "Integra.app\nVersão 1.0\n\n" +
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

    private fun trocarDeConta() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Trocar de conta")
            .setMessage("Você vai sair da conta atual e voltar pra tela de login. Continuar?")
            .setPositiveButton("Trocar") { _, _ ->
                auth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                executarExclusaoConta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarExclusaoConta() {
        val uid = auth.currentUser?.uid ?: return

        if (isAdded) {
            Toast.makeText(requireContext(), "Excluindo conta...", Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
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

                val todosTrabalhos = db.collection("trabalhos").get().await()
                for (t in todosTrabalhos.documents) {
                    try {
                        t.reference.collection("candidaturas").document(uid).delete().await()
                    } catch (_: Exception) { }
                }

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

                db.collection("usuarios").document(uid).delete().await()

                try {
                    auth.currentUser?.delete()?.await()
                } catch (e: Exception) { }

                auth.signOut()

                if (isAdded) {
                    Toast.makeText(requireContext(), "Conta excluída", Toast.LENGTH_SHORT).show()
                }

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao excluir: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}