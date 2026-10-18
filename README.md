# Pool Aim Overlay — Android MVP 0.1

Protótipo Android de sobreposição para desenhar trajetórias geométricas no 8 Ball Pool. A primeira versão usa marcação manual: não lê nem identifica automaticamente as bolas do jogo.

## O que já está previsto no código

- Permissão de sobreposição do Android (`SYSTEM_ALERT_WINDOW`).
- Painel flutuante para selecionar edição, mesa, bola branca, bola-alvo, caçapa e quantidade de tabelas.
- Calibração manual de quatro cantos da mesa.
- Trajetória direta e candidatos geométricos com uma ou duas tabelas, usando o método de imagens refletidas.
- Linha de mira aproximada para o centro de contato (bola fantasma), com diâmetro estimado pela altura da mesa.
- Serviço com notificação persistente enquanto a sobreposição está ativa.
- Testes unitários do motor geométrico.

## Requisitos para compilar

- Android Studio recente.
- JDK 17 ou compatível com a versão do Android Gradle Plugin.
- Android SDK Platform 35 instalado pelo SDK Manager.
- Acesso à internet na primeira sincronização para baixar o Android Gradle Plugin e JUnit.

## Como compilar o APK

1. Extraia o ZIP.
2. Abra a pasta `pool-aim-overlay` no Android Studio.
3. Aceite a sincronização do Gradle e instale a plataforma Android SDK 35 se for solicitada.
4. No menu **Build**, escolha **Build APK(s)**.
5. O APK de depuração costuma aparecer em `app/build/outputs/apk/debug/app-debug.apk`.
6. Para instalar, copie o APK para o Android e autorize a instalação dessa origem, se o sistema solicitar.

O APK de depuração não é uma versão assinada para publicação na Play Store. Para publicar, será necessário gerar uma versão de release assinada e revisar as políticas de permissões de sobreposição e serviço em primeiro plano.

## Como testar no celular

1. Abra Pool Aim Overlay e toque em **Permitir sobreposição**.
2. Volte ao aplicativo e toque em **Iniciar mira sobre o jogo**.
3. Abra o 8 Ball Pool.
4. No painel flutuante, toque em **MESA** e marque os quatro cantos internos da mesa em sentido horário: superior esquerdo, superior direito, inferior direito e inferior esquerdo.
5. Marque **BRANCA**, **ALVO** e **CAÇAPA** tocando no centro aproximado de cada um.
6. Selecione **DIRETA**, **1 TABELA** ou **2 TABELAS**.
7. Use **EDITAR / JOGAR** para alternar entre marcação e modo de passagem de toques. Em modo de edição, o overlay captura os toques na área da tela; em modo de jogo, a camada de desenho fica não tocável.
8. Use **LIMPAR** para reiniciar a marcação e **FECHAR** para encerrar a sobreposição.

## Limitações conhecidas

- Não há captura nem reconhecimento automático de imagem na versão 0.1.
- O retângulo de cálculo usa os limites mínimo/máximo dos quatro cantos; mesas inclinadas ou com perspectiva podem gerar imprecisão.
- As linhas são geométricas. O jogo pode ter efeitos, colisões, atrito, animações e física que não são reproduzidos pelo modelo.
- A posição de parada da bola não pode ser prevista com precisão sem conhecer força, efeito e parâmetros físicos do jogo.
- A sobreposição pode ser incompatível com algumas configurações de tela cheia, recortes ou políticas do jogo. Teste primeiro em partidas de treino.
- A permissão `specialUse` de serviço em primeiro plano é declarada para manter a sobreposição ativa; a distribuição na Google Play exige revisão das políticas aplicáveis.

## Próxima etapa para reconhecimento automático

Uma versão automática precisará solicitar consentimento do usuário para captura de tela via `MediaProjection`, processar frames (por exemplo, com OpenCV), detectar a mesa e as bolas e mapear coordenadas da captura para as coordenadas da sobreposição. Isso não está implementado nesta versão.

## Linha de comando (opcional)

O projeto inclui scripts `gradlew` e `gradlew.bat` leves, que baixam o Gradle 8.9 oficial na primeira execução e verificam o SHA-256 antes de extrair. No Windows, abra o Prompt de Comando na pasta do projeto e execute:

```bat
gradlew.bat test
```

Para compilar o APK, com o Android SDK configurado:

```bat
gradlew.bat assembleDebug
```

## Compilar sem instalar Android Studio (recomendado para Windows Server 2012 R2)

O projeto inclui um workflow do GitHub Actions em `.github/workflows/android-apk.yml`. Ele compila o APK em um runner Linux na nuvem, então o Android SDK e o Android Studio não precisam estar instalados no seu servidor.

1. Crie ou entre na sua conta do GitHub e crie um repositório novo (pode ser privado).
2. Envie o **conteúdo extraído** desta pasta para o repositório, mantendo a estrutura de diretórios e incluindo `.github/workflows/android-apk.yml`.
3. Na página do repositório, abra **Actions** e selecione **Build Pool Aim Overlay APK**.
4. Se necessário, clique em **Run workflow** e confirme em **Run workflow**.
5. Aguarde o job terminar com status verde. Abra a execução concluída e, na seção **Artifacts**, baixe `pool-aim-overlay-debug-apk`.
6. Extraia o ZIP do artifact; dentro dele estará `app-debug.apk`. Transfira o APK para o Android e instale-o, autorizando a instalação dessa origem quando o sistema pedir.

Se o repositório estiver configurado para bloquear Actions, habilite GitHub Actions nas configurações do repositório. O workflow executa os testes unitários e só publica o APK como artifact se a compilação terminar com sucesso.

**Importante:** o artifact é um APK de depuração para teste, não um pacote de publicação assinado. O workflow ainda precisa ser executado no GitHub para confirmar a compilação real. Não coloque senhas, chaves privadas nem tokens no repositório.
