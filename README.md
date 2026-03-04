# app_edmilson

Projeto Android Kotlin (Gradle) para **mobile + Android TV** que abre uma URL em **WebView fullscreen**.

## Requisitos do projeto
- Min SDK: 26 (Android 8)
- Compile SDK: 36
- Target SDK: 36
- 1 Activity principal: `MainActivity`
- Suporte a launcher mobile e `LEANBACK_LAUNCHER` (TV)

## Funcionalidades MVP implementadas
- Abre automaticamente a URL configurada ao iniciar.
- WebView fullscreen com modo imersivo.
- Mantém a tela ligada (`FLAG_KEEP_SCREEN_ON`).
- Navegação touch (mobile) e foco básico para D-pad/controle remoto (TV).
- Tecla Back:
  - volta no histórico do WebView, quando disponível;
  - fecha o app quando não há histórico.
- Links permanecem dentro do próprio WebView.
- Estado offline/erro:
  - mostra overlay `Sem conexão`;
  - botão `Tentar novamente` recarrega a URL.
- Ícone de launcher + banner para Android TV.

## Como alterar a URL
A URL está centralizada em **1 lugar** (preferencial):

Arquivo: `app/build.gradle.kts`

```kotlin
buildConfigField("String", "WEB_URL", "\"https://example.com\"")
```

Troque `https://example.com` pela URL desejada e faça novo build.

## Como rodar (debug)
1. Abra o projeto no Android Studio.
2. Aguarde sync do Gradle.
3. Execute em um dispositivo/emulador Android mobile ou Android TV.

Também via terminal (Windows):

```powershell
.\gradlew.bat assembleDebug
```

APK gerado em:

`app/build/outputs/apk/debug/app-debug.apk`

## Como gerar APK release
Build release:

```powershell
.\gradlew.bat assembleRelease
```

APK gerado em:

`app/build/outputs/apk/release/app-release-unsigned.apk`

Para assinatura final, configure keystore no Android Studio ou no Gradle conforme seu processo de distribuição.

## Estrutura principal
```text
app/
  src/main/AndroidManifest.xml
  src/main/java/com/example/app_edmilson/MainActivity.kt
  src/main/res/layout/activity_main.xml
  src/main/res/drawable/
  src/main/res/drawable-xhdpi/banner_tv.png
  src/main/res/mipmap-anydpi-v26/
```
