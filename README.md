# app_edmilson

Projeto Android Kotlin (Gradle) para **mobile + Android TV** com foco em **digital signage**:
- pareamento por QR Code;
- sessão persistente no dispositivo;
- consumo de playlist de propaganda por API;
- exibição fullscreen de imagem, vídeo e web;
- retomada do ponto ao reabrir o app.

## Requisitos
- Min SDK: 26 (Android 8)
- Compile SDK: 36
- Target SDK: 36
- 1 Activity principal: `MainActivity`
- Suporte a launcher mobile e `LEANBACK_LAUNCHER` (TV)

## Fluxo funcional atual
1. No primeiro uso, o app gera `deviceId` + `pairingCode` e mostra QR Code.
2. O cliente escaneia o QR e autentica no sistema.
3. O app faz polling da API de status até receber token de acesso.
4. Após pareado, o app busca a playlist de propaganda e inicia a reprodução.
5. Em reinício do app/dispositivo:
  - mantém token/sessão;
  - volta no item em exibição (e posição do vídeo quando aplicável).
6. O app só fecha com ação intencional (duplo Back).

## Configurações de integração (BuildConfig)
Arquivo: `app/build.gradle.kts`

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://hotspot1.edmilsonti.com.br/api/\"")
buildConfigField("String", "PAIRING_URL", "\"https://hotspot1.edmilsonti.com.br/tv-pair\"")
buildConfigField("int", "API_POLL_SECONDS", "15")
```

## Contrato de API esperado no app
### 1. Status de pareamento
`GET {API_BASE_URL}tv/devices/{deviceId}/status?code={pairingCode}`

Resposta esperada:
```json
{
  "paired": true,
  "accessToken": "jwt-ou-token"
}
```

### 2. Playlist
`GET {API_BASE_URL}tv/devices/{deviceId}/playlist?resolution={largura}x{altura}`
`Authorization: Bearer {token}`

Resposta esperada:
```json
{
  "refreshAfterSeconds": 15,
  "items": [
    { "type": "image", "url": "https://...", "durationSeconds": 10 },
    { "type": "video", "url": "https://..." },
    { "type": "web", "url": "https://...", "durationSeconds": 15 }
  ]
}
```

## Como rodar (debug)
```powershell
.\gradlew.bat assembleDebug
```

APK debug:
- `app/build/outputs/apk/debug/app-debug.apk`

## Instalação no BlueStacks
```powershell
$apk = (Resolve-Path "app\build\outputs\apk\debug\app-debug.apk").Path
& "C:\Program Files\BlueStacks_nxt\HD-Player.exe" --instance Pie64 --cmd installApk --filepath "$apk"
& "C:\Program Files\BlueStacks_nxt\HD-Player.exe" --instance Pie64 --cmd launchApp --package com.example.app_edmilson
```

## Assinatura release (Google Play)
O projeto suporta assinatura com `keystore.properties` ou variáveis `RELEASE_*`.
Sem assinatura configurada, tarefas `Release`/`bundle` falham propositalmente.

Consulte: `docs/release-signing.md`

## Licenças
- Licença do projeto: `LICENSE` (Apache-2.0)
- Avisos de terceiros: `THIRD_PARTY_NOTICES.md`
