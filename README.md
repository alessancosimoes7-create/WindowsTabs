# WindowsTabs

Programa em **Java (Swing)** para Windows que abre **3 janelas reais e independentes**, sem navegador e sem HTML:

| Janela | Conteúdo |
|--------|----------|
| Janela 1 | Imagem 1 |
| Janela 2 | Imagem 2 |
| Janela 3 | Imagens 3 e 4 |

Cada janela:

- aparece **separada na barra de tarefas** do Windows;
- pode ser **movida e redimensionada**;
- tem seus próprios botões de **minimizar, maximizar e fechar**;
- **não depende de nenhuma janela principal**: fechar uma não fecha as outras. O programa só termina quando a última janela é fechada (ou com Ctrl + C, veja abaixo);
- ajusta a imagem ao tamanho da janela **mantendo a proporção** (a Janela 3 coloca as duas imagens lado a lado; se ficar mais alta que larga, uma em cima da outra).

**Ctrl + C** com qualquer janela do programa ativa fecha as três janelas e finaliza o processo Java.

> Vídeo, áudio e YouTube **não** estão implementados. Ficam para uma atualização futura.

---

## Requisitos

- **Para compilar:** nada além de uma conta no GitHub. O GitHub Actions usa Java 17 automaticamente.
- **Para executar no Windows:** Java 17 ou superior instalado (JRE ou JDK). Não precisa de JavaFX. Download gratuito em https://adoptium.net
- **Para as imagens aparecerem:** conexão com a internet (elas são baixadas das URLs ao abrir).

---

## Estrutura do projeto

```
WindowsTabs/
├── .github/workflows/build.yml          # Workflow do GitHub Actions
├── .gitignore
├── README.md
└── src/main/java/com/windowstabs/
    ├── Main.java            # Ponto de entrada, URLs das imagens, abre as 3 janelas
    ├── AppController.java   # Ciclo de vida das janelas e atalho Ctrl + C
    ├── ImageWindow.java     # Janela (JFrame) independente
    ├── ImagePanel.java      # Baixa e desenha uma imagem ajustada à janela
    ├── DualImagePanel.java  # Mostra duas imagens na mesma janela (Janela 3)
    └── ImageLoader.java     # Download da imagem (com novas tentativas)
```

---

## Como enviar o projeto para o GitHub

### 1. Criar o repositório

1. Entre em https://github.com e faça login.
2. Clique em **New** (Novo repositório).
3. Nome: `WindowsTabs`. Deixe vazio (**não** marque README, .gitignore nem licença).
4. Clique em **Create repository**.

### 2. Enviar os arquivos (pelo terminal / Git)

Instale o Git em https://git-scm.com se ainda não tiver. Abra o terminal **dentro da pasta `WindowsTabs`** e rode (troque `SEU_USUARIO`):

```bash
git init
git add .
git commit -m "Primeira versão"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/WindowsTabs.git
git push -u origin main
```

> Pelo site do GitHub (botão *Add file → Upload files*), pastas que começam com ponto, como `.github`, às vezes não são enviadas no arrastar e soltar. Por isso o método com Git (ou o app **GitHub Desktop**) é mais seguro. Confirme depois que o arquivo `.github/workflows/build.yml` aparece no repositório.

---

## Como baixar o JAR gerado

O workflow roda **sozinho** a cada `push` na branch `main`. Para baixar:

1. No repositório, abra a aba **Actions**.
2. Clique na execução mais recente de **Build WindowsTabs** (deve estar com o ✅ verde).
3. Role até a seção **Artifacts** no fim da página.
4. Clique em **WindowsTabs** para baixar um `.zip`.
5. Extraia o zip: dentro está o **`WindowsTabs.jar`**.

Para rodar a compilação manualmente: **Actions → Build WindowsTabs → Run workflow**.

---

## Como executar no Windows

**Opção 1:** dê dois cliques no `WindowsTabs.jar` (se o Java estiver associado a arquivos `.jar`).

**Opção 2:** pelo terminal (Prompt de Comando ou PowerShell), na pasta do JAR:

```bash
javaw -jar WindowsTabs.jar
```

`javaw` não deixa janela de console aberta. Se quiser ver mensagens de erro, use `java -jar WindowsTabs.jar`.

As 3 janelas abrem lado a lado e aparecem como 3 botões na barra de tarefas.

---

## Atalhos

| Ação | Resultado |
|------|-----------|
| **Ctrl + C** (com uma janela do programa ativa) | Fecha as 3 janelas e encerra o processo Java |
| Botão **X** de uma janela | Fecha só aquela janela |
| Fechar a última janela | Encerra o programa |

---

## Problemas comuns

- **Janela preta com "Falha ao carregar a imagem":** verifique a internet. Clique na janela para tentar novamente. Links do `encrypted-tbn0.gstatic.com` podem expirar com o tempo; se isso acontecer, troque a URL correspondente no topo do arquivo `Main.java`.
- **Imagem não suportada:** o Java puro lê JPEG, PNG e GIF. Imagens em WebP ou AVIF não são decodificadas.
- **Nada acontece ao dar dois cliques no JAR:** instale o Java 17+ e tente `javaw -jar WindowsTabs.jar` pelo terminal.
- **O workflow falhou:** abra a execução na aba **Actions**, clique no passo em vermelho e veja o erro de compilação.

---

## Compilar manualmente (opcional)

Com o JDK 17+ instalado, na raiz do projeto:

```bash
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes $(find src/main/java -name "*.java")
jar --create --file WindowsTabs.jar --main-class com.windowstabs.Main -C build/classes .
```
