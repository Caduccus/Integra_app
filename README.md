<div align="center">
  <img width="250" height="250" alt="logo_integra" src="https://github.com/user-attachments/assets/b91d7685-3b1e-463a-9a34-99f5f2939e62" />

  
  # 🚀 Integra.app
  
  **Trabalho remoto, sem limites.**

  [![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)]()
  [![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)]()
  [![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)]()
  [![Material Design 3](https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=material-design&logoColor=white)]()

  *Uma plataforma completa que conecta profissionais a oportunidades de trabalho remoto, com gestão de empresas, candidaturas e comunicação em tempo real.*
</div>

---

## 📱 Sobre o Projeto
O **Integra.app** é um aplicativo Android nativo desenvolvido em **Kotlin** que visa revolucionar a forma como profissionais encontram vagas remotas e como empresas gerenciam seus processos seletivos. Tudo em um só lugar: desde a publicação de uma vaga até o chat direto com o candidato aprovado.

---

## ✨ Funcionalidades Principais

### 💼 Área de Trabalho & Empresas
*   **Publicação de Vagas:** Crie vagas públicas ou privadas (exclusivas para membros da empresa), com upload de banners personalizados.
*   **Gestão de Candidaturas:** Aceite, rejeite ou visualize candidatos com um sistema de timeline (Enviada, Vista, Resultado).
*   **Empresas:** Crie sua própria empresa, adicione membros, promova administradores e gerencie pedidos de entrada.
*   **Curtidas & Favoritos:** Salve as vagas que mais gostou para se candidatar depois.

### 💬 Chat Avançado (Estilo WhatsApp)
*   **Mensagens em Tempo Real:** Envie textos, imagens, documentos (PDF, ZIP) e áudios.
*   **Gravação de Áudio:** Interface com ondas (waveform) animadas em tempo real durante a gravação e reprodução.
*   **Recursos Extras:** Responder mensagens (Reply), editar textos, deletar para todos, fixar conversas e bloquear usuários.
*   **Temas Customizáveis:** Chat adaptado com identidade visual do app (Dark Mode com toques de azul).

### 🤝 Rede Social & Perfil
*   **Sistema de Amizades:** Envie e receba solicitações de amizade, converse diretamente com amigos e visualize a lista completa.
*   **Perfil Profissional:** Adicione bio, currículo (PDF), links para redes sociais (LinkedIn, GitHub, etc.) e status de disponibilidade.
*   **Avaliações:** Deixe e receba avaliações em estrelas sobre outros usuários e empresas.

### 🔔 Notificações & Engajamento
*   **Push Notifications (OneSignal + FCM):** Receba notificações no celular mesmo com o app fechado sobre novas mensagens, amizades e candidaturas.
*   **Notificações In-App:** Central de notificações integrada na Home com atualização em tempo real.

### 🎨 Design & Acessibilidade
*   **Material Design 3:** Interface moderna e fluida.
*   **Acessibilidade:** Opções de alto contraste, redução de animações e ajuste no tamanho da fonte.
*   **Internacionalização (i18n):** Suporte completo para Português, Inglês e Espanhol.
*   **Múltiplos Temas:** Escolha entre 7 cores de tema diferentes.

---

## 🛠️ Tecnologias e Bibliotecas Utilizadas

*   **Linguagem:** [Kotlin](https://kotlinlang.org/)
*   **Backend as a Service:** [Firebase](https://firebase.google.com/) (Authentication, Cloud Firestore, Storage)
*   **Notificações:** [OneSignal](https://onesignal.com/) + Firebase Cloud Messaging (FCM)
*   **Mídia & Uploads:** [Cloudinary](https://cloudinary.com/) (Áudios, Imagens, PDFs)
*   **Banco de Dados Local:** [Room Database](https://developer.android.com/training/data-storage/room)
*   **Carregamento de Imagens:** [Glide](https://github.com/bumptech/glide)
*   **Rede:** [OkHttp](https://square.github.io/okhttp/)
*   **UI/UX:** XML tradicional com Componentes do Material Design 3
*   **Arquitetura:** MVVM (Model-View-ViewModel) com Repositórios

---

## 🚀 Como Rodar o Projeto

### Pré-requisitos
*   Android Studio (Versão mais recente recomendada).
*   Conta no Firebase com um projeto configurado (Auth, Firestore, Storage).
*   Conta no Cloudinary.
*   Conta no OneSignal.

### Passo a Passo

1. Clone este repositório:
```bash
git clone https://github.com/Caduccus/Integra_app.git
