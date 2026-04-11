# Sprint 2 - Pareamento QR, API de Playlist e Sessao Persistente

## Periodo
- Data de consolidacao: 04/03/2026
- Sprint: 2

## Objetivo
Evoluir o app de WebView MVP para um player de propaganda autenticado por pareamento, com sessao persistente e retomada automatica de execucao.

## Escopo entregue
- Pareamento por QR Code:
  - gera `deviceId` e `pairingCode` persistidos localmente;
  - monta URL de pareamento (`BuildConfig.PAIRING_URL`) e exibe QR.
- Integracao com API:
  - consulta status de pareamento;
  - consome playlist com envio de resolucao (`WxH`).
- Player de midia multipla:
  - imagem (`ImageView`);
  - video (`Media3 ExoPlayer`);
  - web (`WebView`).
- Sessao persistente:
  - token salvo localmente;
  - app volta conectado ao reiniciar.
- Retomada de execucao:
  - salva indice da playlist atual;
  - salva posicao do video quando aplicavel;
  - reabre no ponto salvo.
- UX operacional:
  - modo imersivo e tela sempre ligada;
  - app fecha apenas com duplo `Back`;
  - overlay offline com tentativa de reconexao.

## Dependencias adicionadas
- `androidx.lifecycle:lifecycle-runtime-ktx`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `com.google.zxing:core`
- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui`

## BuildConfig atualizado
Arquivo: `app/build.gradle.kts`

- `API_BASE_URL`
- `PAIRING_URL`
- `API_POLL_SECONDS`

## Contrato de API esperado (resumo)
1. `GET /tv/devices/{deviceId}/status?code={pairingCode}`
2. `GET /tv/devices/{deviceId}/playlist?resolution={width}x{height}` com `Bearer token`

## Validacao tecnica
- `.\gradlew.bat app:assembleDebug`
- `.\gradlew.bat app:lintDebug`
- Instalacao no BlueStacks via:
  - `HD-Player.exe --cmd installApk`
  - `HD-Player.exe --cmd launchApp`

## Arquivos principais alterados
- `app/src/main/java/com/hotspottv/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values/strings.xml`
- `app/build.gradle.kts`
- `README.md`

## Riscos/limitacoes conhecidos
- API real pode ter esquema de payload diferente do contrato assumido.
- Em caso de expiracao real de token, estrategia ideal e refresh token no backend.
- Ainda nao ha suite de testes instrumentados para o fluxo fim a fim.
