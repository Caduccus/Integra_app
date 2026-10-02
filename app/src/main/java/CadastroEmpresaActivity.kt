package com.example.plataformaremota

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.data.entity.Empresa
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CadastroEmpresaActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var cardLogo: MaterialCardView
    private lateinit var imgLogo: ImageView

    private lateinit var edtNome: TextInputEditText
    private lateinit var edtDescricao: TextInputEditText
    private lateinit var edtCnpj: TextInputEditText
    private lateinit var edtArea: AutoCompleteTextView
    private lateinit var edtEndereco: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var btnCriar: MaterialButton

    private lateinit var repository: EmpresaRepository

    private var logoUrl: String = ""
    private var uploadingLogo: Boolean = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) uploadLogo(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_empresa)

        repository = EmpresaRepository()

        btnVoltar = findViewById(R.id.btnVoltarCadastroEmpresa)
        cardLogo = findViewById(R.id.cardLogoEmpresa)
        imgLogo = findViewById(R.id.imgLogoEmpresa)
        edtNome = findViewById(R.id.edtNomeEmpresa)
        edtDescricao = findViewById(R.id.edtDescricaoEmpresa)
        edtCnpj = findViewById(R.id.edtCnpjEmpresa)
        edtArea = findViewById(R.id.edtAreaEmpresa)
        edtEndereco = findViewById(R.id.edtEnderecoEmpresa)
        edtEmail = findViewById(R.id.edtEmailEmpresa)
        btnCriar = findViewById(R.id.btnCriarEmpresa)

        configurarDropdownArea()

        btnVoltar.setOnClickListener { finish() }
        cardLogo.setOnClickListener {
            if (uploadingLogo) return@setOnClickListener
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        btnCriar.setOnClickListener { criarEmpresa() }

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun configurarDropdownArea() {
        val opcoes = arrayOf(
            "Tecnologia", "Design", "Marketing", "Vendas",
            "Educação", "Saúde", "Finanças", "Recursos Humanos",
            "Engenharia", "Administrativo", "Jurídico", "Outros"
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            opcoes
        )
        edtArea.setAdapter(adapter)
        // ⭐ NÃO coloca setOnClickListener — o ExposedDropdownMenu já abre sozinho
    }

    // ─────────────────────────────────────────────
    // UPLOAD DE LOGO
    // ─────────────────────────────────────────────
    private fun uploadLogo(uri: Uri) {
        uploadingLogo = true
        Toast.makeText(this, "Enviando logo...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "empresas/logos")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        logoUrl = url.replace("http://", "https://")
                        uploadingLogo = false

                        runOnUiThread {
                            Glide.with(this@CadastroEmpresaActivity)
                                .load(logoUrl).circleCrop().into(imgLogo)
                            imgLogo.imageTintList = null
                            imgLogo.setPadding(0, 0, 0, 0)
                            Toast.makeText(
                                this@CadastroEmpresaActivity,
                                "Logo carregada!", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        uploadingLogo = false
                        Toast.makeText(
                            this@CadastroEmpresaActivity,
                            "Erro: ${error?.description}", Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            uploadingLogo = false
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ─────────────────────────────────────────────
    // CRIAR EMPRESA
    // ─────────────────────────────────────────────
    private fun criarEmpresa() {
        val nome = edtNome.text.toString().trim()
        val descricao = edtDescricao.text.toString().trim()
        val cnpj = edtCnpj.text.toString().trim()
        val area = edtArea.text.toString().trim()
        val endereco = edtEndereco.text.toString().trim()
        val email = edtEmail.text.toString().trim()

        if (nome.length < 2) {
            mostrarSnackbar("Nome precisa ter pelo menos 2 caracteres")
            return
        }
        if (descricao.length < 10) {
            mostrarSnackbar("Descrição precisa ter pelo menos 10 caracteres")
            return
        }
        if (area.isEmpty()) {
            mostrarSnackbar("Selecione uma área de atuação")
            edtArea.requestFocus()
            return
        }
        if (email.isNotEmpty() && !email.contains("@")) {
            mostrarSnackbar("E-mail inválido")
            return
        }
        if (uploadingLogo) {
            mostrarSnackbar("Aguarde o upload da logo terminar")
            return
        }

        btnCriar.isEnabled = false
        btnCriar.text = "CRIANDO..."

        lifecycleScope.launch {
            val empresa = Empresa(
                nome = nome,
                descricao = descricao,
                cnpj = cnpj,
                areaAtuacao = area,
                endereco = endereco,
                emailCorporativo = email,
                logoUrl = logoUrl
            )

            val id = repository.criarEmpresa(empresa)

            if (id != null) {
                Toast.makeText(
                    this@CadastroEmpresaActivity,
                    "Empresa criada!",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            } else {
                btnCriar.isEnabled = true
                btnCriar.text = "CRIAR EMPRESA"
                mostrarSnackbar("Erro ao criar empresa. Verifique sua conexão.")
            }
        }
    }

    private fun mostrarSnackbar(mensagem: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mensagem,
            Snackbar.LENGTH_LONG
        ).show()
    }
}