import os
import re

MAPA_STRINGS = {
    # GERAIS
    "OK": "geral_ok", "Cancelar": "geral_cancelar", "Salvar": "geral_salvar",
    "Remover": "geral_remover", "Excluir": "geral_excluir", "Editar": "geral_editar",
    "Voltar": "geral_voltar", "Aguarde...": "geral_aguarde", "Depois": "geral_depois",
    
    # HOME & NAV
    "Empresas": "empresas_titulo", "Perfil": "perfil_titulo", "Mensagens": "chat_titulo",
    "Início": "nav_inicio", "Publicar": "nav_publicar",
    "Trabalhos disponíveis": "home_titulo_trabalhos", "Buscar trabalho...": "home_busca_hint",
    "Todos": "home_filtro_todos", "Meus": "home_filtro_meus", "Insc.": "home_filtro_inscritos",
    "Empr.": "home_filtro_empresas", "Fav.": "home_filtro_favoritos",
    "Olá!": "home_ola", "Nada por aqui ainda": "home_vazio_titulo",
    "Seja o primeiro a publicar um trabalho!": "home_vazio_sub",
    "PUBLICAR PRIMEIRO": "home_btn_publicar_primeiro", "Carregando trabalhos...": "home_carregando",
    
    # CADASTRO
    "Criar conta": "cadastro_titulo", "Preencha os dados abaixo para começar": "cadastro_subtitulo",
    "Nome completo": "cadastro_hint_nome", "Nome de usuário (ex: joao123)": "cadastro_hint_username",
    "E-mail": "cadastro_hint_email", "Senha": "cadastro_hint_senha",
    "Bio (opcional)": "cadastro_hint_bio", "Profissão": "cadastro_hint_profissao",
    "CADASTRAR": "cadastro_btn_salvar", "FINALIZAR CADASTRO": "cadastro_btn_finalizar_google",

    # PUBLICAR
    "Publicar trabalho": "publicar_titulo", "Preencha os dados abaixo": "publicar_subtitulo",
    "SOBRE O TRABALHO": "publicar_secao_sobre", "DETALHES": "publicar_secao_detalhes",
    "VISIBILIDADE": "publicar_secao_visibilidade", "Título do trabalho": "publicar_hint_titulo",
    "Descrição detalhada": "publicar_hint_descricao", "Categoria": "publicar_hint_categoria",
    "Nível de experiência": "publicar_hint_nivel", "Prazo (ex: 30 dias)": "publicar_hint_prazo",
    "Publicar como": "publicar_hint_como", "PUBLICAR TRABALHO": "publicar_btn",
    "Pessoal (sem empresa)": "publicar_pessoal",
    "Trabalho público: qualquer pessoa pode ver e se candidatar.": "publicar_info_publico",

    # EMPRESAS (GERAL E GERENCIAR)
    "+ CRIAR": "empresas_btn_criar", "Descobrir": "empresas_filtro_descobrir",
    "Minhas": "empresas_filtro_minhas", "Pedidos": "empresas_filtro_pedidos",
    "Nenhuma empresa ainda": "empresas_vazio_nenhuma", "Seja o primeiro a criar uma empresa!": "empresas_vazio_nenhuma_sub",
    "CRIAR EMPRESA": "empresas_btn_criar_empresa", "PEDIR": "empresas_btn_pedir",
    "CANCELAR": "empresas_btn_cancelar",
    "Membros": "gerenciar_tab_membros", "Pendentes": "gerenciar_tab_pendentes",
    "EXCLUIR EMPRESA": "gerenciar_btn_excluir_empresa", "DONO": "gerenciar_badge_dono", "ADMIN": "gerenciar_badge_admin",
    "Nome da empresa *": "nova_empresa_hint_nome", "Descrição *": "nova_empresa_hint_descricao",
    "CNPJ (opcional)": "nova_empresa_hint_cnpj", "Área de atuação *": "nova_empresa_hint_area",
    "Endereço (opcional)": "nova_empresa_hint_endereco", "E-mail corporativo (opcional)": "nova_empresa_hint_email",
    "Toque para adicionar logo (opcional)": "nova_empresa_logo",
    "Criar empresa": "nova_empresa_titulo", "PEDIR PRA ENTRAR": "empresa_btn_pedir_entrar",
    "GERENCIAR EMPRESA": "empresa_btn_gerenciar", "ABRIR GRUPO GERAL": "empresa_btn_abrir_grupo",
    "Sobre a empresa": "empresa_sobre", "CNPJ": "empresa_cnpj", "Endereço": "empresa_endereco",
    "Trabalhos da empresa": "empresa_secao_trabalhos", "Nenhum trabalho publicado ainda": "empresa_sem_trabalhos",
    "Detalhes da empresa": "empresa_detalhes_titulo",

    # TRABALHO E DETALHES
    "Detalhes do Trabalho": "trabalho_detalhes_titulo", "Descrição": "trabalho_secao_descricao",
    "Nível de Experiência": "trabalho_secao_nivel", "Prazo": "trabalho_secao_prazo",
    "EDITAR": "trabalho_btn_editar", "CHAT": "trabalho_btn_chat", "CADASTRAR-SE NO TRABALHO": "trabalho_btn_candidatar",
    "ABRIR CHAT COM O CRIADOR": "trabalho_btn_abrir_chat", "Candidatos": "candidatos_titulo",
    "ACEITAR": "candidatos_btn_aceitar", "REJEITAR": "candidatos_btn_rejeitar", "AVALIAR": "candidatos_btn_avaliar",
    "Ninguém se candidatou ainda": "candidatos_vazio", "Quando alguém se candidatar, aparece aqui": "candidatos_vazio_sub",
    "Minhas candidaturas": "historico_titulo", "Sem candidaturas ainda": "historico_vazio_titulo",

    # CHAT
    "Digite uma mensagem...": "chat_input_hint", "Gravando...": "chat_input_gravando",
    "Você bloqueou este usuário": "chat_input_bloqueado", "Nome do Chat": "chat_header_titulo",
    "Sem conversas ainda": "chat_vazio_titulo", "Candidate-se a um trabalho ou crie um grupo para começar": "chat_vazio_sub",
    "CRIAR GRUPO": "chat_btn_criar_grupo", "+ GRUPO": "chat_btn_novo_grupo",
    "Nome do grupo": "novo_grupo_hint_nome", "Buscar por nome ou @username": "novo_grupo_busca",
    "Selecionados: 0": "novo_grupo_selecionados_count", "CRIAR": "novo_grupo_btn_criar",
    "Info do grupo": "info_grupo_titulo", "SAIR": "info_grupo_btn_sair", "APAGAR GRUPO": "info_grupo_btn_apagar",
    "ADICIONAR": "add_membros_btn_confirmar", "Adicionar membros": "add_membros_titulo",

    # PERFIL E CONFIGURAÇÕES
    "MEU PERFIL": "perfil_secao_meu_perfil", "CURRÍCULO": "perfil_secao_curriculo", 
    "LINKS DE CONTATO": "perfil_secao_links", "PREFERÊNCIAS": "perfil_secao_preferencias",
    "SOBRE": "perfil_secao_sobre", "CONTA": "perfil_secao_conta",
    "Editar informações": "perfil_editar_info", "Quem viu meu perfil": "perfil_quem_viu",
    "Anexar currículo (PDF)": "perfil_anexar_curriculo", "Toque para abrir": "perfil_curriculo_toque_abrir",
    "Nenhum link adicionado": "perfil_sem_links", "Adicionar link": "perfil_add_link",
    "Tema do aplicativo": "perfil_tema", "Notificações": "perfil_notificacoes", "Ativado": "perfil_notificacoes_ativado",
    "Idioma": "perfil_idioma", "Acessibilidade": "perfil_acessibilidade", "Sobre o aplicativo": "perfil_sobre",
    "Sair da conta": "perfil_sair", "Excluir conta": "perfil_excluir_conta",
    "A exclusão de conta é permanente e não pode ser desfeita": "perfil_excluir_aviso",
    "Painel administrativo": "perfil_painel_admin", "Gerenciar usuários, empresas, trabalhos": "perfil_painel_admin_sub",
    "Sem bio ainda": "perfil_bio_vazia", "Online": "status_online", "Ausente": "status_ausente",
    "Não perturbe": "status_nao_pertube", "Invisível": "status_invisivel",

    # ACESSIBILIDADE E IDIOMA E SOBRE
    "Ajuste o aplicativo para sua melhor experiência.": "acess_subtitulo",
    "TAMANHO DA FONTE": "acess_secao_fonte", "Normal": "acess_fonte_normal", "Grande": "acess_fonte_grande",
    "Extra grande": "acess_fonte_extra", "OUTRAS OPÇÕES": "acess_secao_outras",
    "Reduzir animações": "acess_reduzir_animacoes", "Remove transições entre telas": "acess_reduzir_animacoes_sub",
    "Alto contraste": "acess_alto_contraste", "Fundo preto + textos mais legíveis": "acess_alto_contraste_sub",
    "Escolha o idioma do aplicativo": "idioma_subtitulo", "Sistema (padrão)": "idioma_sistema",
    "Português": "idioma_pt", "English": "idioma_en", "Español": "idioma_es",
    "Desenvolvido por": "sobre_autores", "Código-fonte": "sobre_github",
    "Termos de uso": "sobre_termos", "Política de privacidade": "sobre_politica",
    "Fechar": "sobre_fechar", "Trabalho remoto, sem limites": "sobre_slogan",

    # ADMIN
    "Usuários": "admin_lista_usuarios", "Trabalhos": "admin_lista_trabalhos",
    "GERENCIAR": "admin_secao_gerenciar", "Gerenciar usuários": "admin_btn_usuarios",
    "Gerenciar empresas": "admin_btn_empresas", "Gerenciar trabalhos": "admin_btn_trabalhos",
    "Ações administrativas afetam o app inteiro. Use com cuidado.": "admin_aviso",
    "Excluir usuário": "admin_excluir_usuario_titulo", "Excluir empresa": "admin_excluir_empresa_titulo",
    "Excluir trabalho": "admin_excluir_trabalho_titulo", "EXCLUIR": "admin_btn_excluir"
}

def processar_xml(caminho_arquivo):
    with open(caminho_arquivo, 'r', encoding='utf-8') as f:
        conteudo = f.read()
    
    modificado = False
    for texto_pt, chave_string in MAPA_STRINGS.items():
        # Regex para achar android:text="Texto" ou android:hint="Texto" ou android:contentDescription="Texto"
        padrao_text = r'android:text="' + re.escape(texto_pt) + r'"'
        padrao_hint = r'android:hint="' + re.escape(texto_pt) + r'"'
        padrao_desc = r'android:contentDescription="' + re.escape(texto_pt) + r'"'
        
        if re.search(padrao_text, conteudo):
            conteudo = re.sub(padrao_text, f'android:text="@string/{chave_string}"', conteudo)
            modificado = True
        if re.search(padrao_hint, conteudo):
            conteudo = re.sub(padrao_hint, f'android:hint="@string/{chave_string}"', conteudo)
            modificado = True
        if re.search(padrao_desc, conteudo):
            conteudo = re.sub(padrao_desc, f'android:contentDescription="@string/{chave_string}"', conteudo)
            modificado = True

    if modificado:
        with open(caminho_arquivo, 'w', encoding='utf-8') as f:
            f.write(conteudo)
        print(f"✅ Traduzido: {os.path.basename(caminho_arquivo)}")

# Varre as pastas de layout do projeto
caminho_res = os.path.join('app', 'src', 'main', 'res')
for root, dirs, files in os.walk(caminho_res):
    for file in files:
        if file.endswith('.xml'):
            processar_xml(os.path.join(root, file))

print("🎉 Todos os arquivos XML foram processados com sucesso!")