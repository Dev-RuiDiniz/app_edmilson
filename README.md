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
buildConfigField("String", "WEB_URL", "\"https://hotspot1.edmilsonti.com.br/\"")
```

Troque pela URL desejada e faça novo build.

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

## Assinatura final (Google Play)
O projeto está preparado para assinatura de release via:
- `keystore.properties` (recomendado para uso local)
- variáveis de ambiente `RELEASE_*` (recomendado para CI/CD)

Se você executar tarefa release sem configurar assinatura, o Gradle interrompe com erro para evitar artefato inválido.

### 1. Gerar keystore (uma única vez)
Exemplo:

```powershell
keytool -genkeypair -v `
  -keystore C:\keys\edmilson-release.jks `
  -alias edmilson_upload `
  -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Configurar `keystore.properties`
1. Copie `keystore.properties.example` para `keystore.properties`.
2. Preencha os valores reais:

```properties
RELEASE_STORE_FILE=C:/keys/edmilson-release.jks
RELEASE_STORE_PASSWORD=*****
RELEASE_KEY_ALIAS=edmilson_upload
RELEASE_KEY_PASSWORD=*****
```

`keystore.properties` e arquivos de chave já estão no `.gitignore`.

### 3. Alternativa via variáveis de ambiente (CI/CD)
```powershell
$env:RELEASE_STORE_FILE="C:/keys/edmilson-release.jks"
$env:RELEASE_STORE_PASSWORD="*****"
$env:RELEASE_KEY_ALIAS="edmilson_upload"
$env:RELEASE_KEY_PASSWORD="*****"
```

### 4. Gerar release assinado
Para Google Play, prefira AAB:

```powershell
.\gradlew.bat bundleRelease
```

Saída:
- `app/build/outputs/bundle/release/app-release.aab`

Se precisar APK assinado:

```powershell
.\gradlew.bat assembleRelease
```

Saída:
- `app/build/outputs/apk/release/app-release.apk`

### 5. Fluxo pelo Android Studio
1. `Build > Generate Signed Bundle / APK`
2. Escolha `Android App Bundle`
3. Selecione o mesmo `keystore`/alias
4. Build Variant: `release`
5. Finalize a geração e valide o artefato

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

## Licenças
- Licença do projeto: `LICENSE` (Apache-2.0)
- Avisos de terceiros: `THIRD_PARTY_NOTICES.md`
