package com.example.plataformaremota

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class SobreActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre)

        findViewById<ImageView>(R.id.btnVoltarSobre).setOnClickListener { finish() }

        val txtVersao = findViewById<TextView>(R.id.txtVersaoSobre)
        txtVersao.text = "Versão ${BuildConfig.VERSION_NAME}"

        findViewById<android.view.View>(R.id.btnGitHub).setOnClickListener {
            HapticHelper.leve(this)
            abrirLink("https://github.com/Caduccus/Integra_app")
        }

        findViewById<android.view.View>(R.id.btnTermos).setOnClickListener {
            HapticHelper.leve(this)
            abrirDialogTexto("Termos de uso", TERMOS_USO)
        }

        findViewById<android.view.View>(R.id.btnPolitica).setOnClickListener {
            HapticHelper.leve(this)
            abrirDialogTexto("Política de privacidade", POLITICA_PRIVACIDADE)
        }

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun abrirLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show()
        }
    }

    // ⭐ Dialog com fundo branco fixo — sempre legível em qualquer tema
    private fun abrirDialogTexto(titulo: String, texto: String) {
        val view = layoutInflater.inflate(R.layout.dialog_sobre_texto, null)
        val corpo = view.findViewById<TextView>(R.id.txtDialogSobreCorpo)
        corpo.text = texto

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(view)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private val TERMOS_USO = """
        Última atualização: 09 de outubro de 2026

        1. ACEITAÇÃO DOS TERMOS
        Ao criar uma conta e utilizar o aplicativo Integra, você concorda integralmente com estes Termos de Uso. Caso não concorde, não utilize o aplicativo.

        2. DESCRIÇÃO DO SERVIÇO
        O Integra é uma plataforma que conecta profissionais a oportunidades de trabalho remoto e permite a criação e gestão de empresas, equipes e processos seletivos.

        3. CADASTRO
        Você se compromete a fornecer informações verdadeiras, completas e atualizadas no momento do cadastro. É de sua responsabilidade manter a confidencialidade da sua senha.

        4. CONDUTA DO USUÁRIO
        Você concorda em não:
        • Publicar conteúdo falso, ofensivo ou ilegal
        • Assediar, ameaçar ou prejudicar outros usuários
        • Utilizar a plataforma para fins fraudulentos
        • Violar direitos autorais ou de propriedade intelectual

        5. TRABALHOS E CANDIDATURAS
        O Integra é apenas uma plataforma de conexão. Não somos responsáveis pelas negociações, contratos ou relações de trabalho firmadas entre usuários e empresas.

        6. CONTEÚDO DO USUÁRIO
        Você mantém os direitos sobre o conteúdo que publica, mas concede ao Integra licença para exibi-lo dentro da plataforma.

        7. SUSPENSÃO DE CONTAS
        Reservamo-nos o direito de suspender ou encerrar contas que violem estes Termos.

        8. ALTERAÇÕES
        Podemos atualizar estes Termos a qualquer momento. Recomendamos revisão periódica.

        9. CONTATO
        Dúvidas: contato@integra.app
    """.trimIndent()

    private val POLITICA_PRIVACIDADE = """
        Última atualização: 09 de outubro de 2026

        1. INFORMAÇÕES QUE COLETAMOS
        • Dados de cadastro: nome, e-mail, username, profissão, bio
        • Foto de perfil e currículo (opcional)
        • Mensagens trocadas na plataforma
        • Informações sobre trabalhos, candidaturas e empresas
        • Dados de uso (telas visitadas, ações realizadas)

        2. COMO USAMOS OS DADOS
        Utilizamos suas informações para:
        • Fornecer e melhorar o serviço
        • Permitir conexões entre candidatos e empresas
        • Enviar notificações relevantes
        • Garantir segurança da plataforma

        3. COMPARTILHAMENTO
        Não vendemos seus dados. Compartilhamos apenas:
        • Com outros usuários, quando você torna informações públicas (perfil, trabalhos publicados)
        • Com prestadores de serviço técnicos (Firebase, Cloudinary) para operar o app
        • Quando exigido por lei

        4. ARMAZENAMENTO
        Seus dados são armazenados em servidores seguros do Google Firebase. Fotos e documentos são hospedados no Cloudinary.

        5. SEUS DIREITOS
        Você pode a qualquer momento:
        • Acessar seus dados pelo próprio app
        • Editar ou excluir suas informações no perfil
        • Excluir sua conta permanentemente

        6. COOKIES E RASTREAMENTO
        Não utilizamos cookies de terceiros para publicidade. Usamos apenas dados anônimos de uso interno.

        7. SEGURANÇA
        Adotamos medidas técnicas para proteger seus dados. Nenhum sistema é 100% seguro, mas fazemos nosso melhor.

        8. MENORES DE IDADE
        O Integra é destinado a maiores de 16 anos.

        9. CONTATO
        Encarregado de dados: privacidade@integra.app
    """.trimIndent()
}