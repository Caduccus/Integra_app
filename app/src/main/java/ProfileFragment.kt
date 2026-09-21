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

    private var usuarioId: String = ""
    private var nomeUsuario: String = ""
    private var emailUsuario: String = ""
    private var profissaoUsuario: String = ""

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

        usuarioId = arguments?.getString("usuarioId") ?: ""
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
        view.findViewById<View>(R.id.opcaoSobre).setOnClickListener {
            abrirDialogSobre()
        }

        btnLogoutPerfil.setOnClickListener { fazerLogout() }
    }

    // ─────────────────────────────────────────────
    // BUSCAR DADOS DO FIRESTORE
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

                    txtNomePerfil.text = nomeUsuario
                    txtEmailPerfil.text = emailUsuario.ifEmpty { "—" }
                    txtProfissaoPerfil.text = profissaoUsuario.ifEmpty { "—" }
                } else {
                    txtNomePerfil.text = "Usuário"
                    txtEmailPerfil.text = auth.currentUser?.email ?: "—"
                    txtProfissaoPerfil.text = "—"
                }
            } catch (e: Exception) {
                txtNomePerfil.text = nomeUsuario.ifEmpty { "Usuário" }
                txtEmailPerfil.text = auth.currentUser?.email ?: "—"
                txtProfissaoPerfil.text = "—"
                Toast.makeText(requireContext(), "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FOTO_PERFIL, arquivo.absolutePath).apply()

            imgFotoPerfil.setImageURI(uri)
            imgFotoPerfil.imageTintList = null
            imgFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
            imgFotoPerfil.setPadding(0, 0, 0, 0)

            Toast.makeText(requireContext(), "Foto atualizada!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarFotoSalva() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_FOTO_PERFIL, null)

        if (path != null) {
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
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_FOTO_PERFIL, null)

        if (path != null) {
            val arquivo = File(path)
            if (arquivo.exists()) arquivo.delete()
        }

        prefs.edit().remove(KEY_FOTO_PERFIL).apply()

        imgFotoPerfil.setImageResource(R.drawable.ic_person)
        imgFotoPerfil.imageTintList = ColorStateList.valueOf(Color.WHITE)
        imgFotoPerfil.scaleType = ImageView.ScaleType.FIT_CENTER
        val padding = (18 * resources.displayMetrics.density).toInt()
        imgFotoPerfil.setPadding(padding, padding, padding, padding)

        Toast.makeText(requireContext(), "Foto removida", Toast.LENGTH_SHORT).show()
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
                val atualizacao = mapOf(
                    "nome" to nome,
                    "email" to email,
                    "profissao" to profissao
                )

                db.collection("usuarios").document(usuarioId)
                    .update(atualizacao)
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
                Toast.makeText(
                    requireContext(),
                    if (switch.isChecked) "Notificações ativadas" else "Notificações desativadas",
                    Toast.LENGTH_SHORT
                ).show()
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
                "Integra.app\n" +
                        "Versão 1.0\n\n" +
                        "Plataforma de trabalhos remotos.\n\n" +
                        "Conecte-se a oportunidades e publique seus trabalhos de forma simples."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // ─────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────
    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}