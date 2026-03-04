# Sprint 1 - MVP Android WebView (Mobile + TV)

## Período
- Data de consolidação: 04/03/2026
- Sprint: 1

## Objetivo
Entregar um aplicativo Android em Kotlin, formato Gradle padrão, com suporte a celular/tablet e Android TV, que abre automaticamente um site em tela cheia via WebView.

## Escopo entregue
- Projeto Android completo com:
  - Kotlin
  - Gradle Wrapper
  - `MainActivity` única
  - Min SDK 26
  - Compile/Target SDK 36
- Suporte a launcher mobile e Android TV (`LEANBACK_LAUNCHER`).
- WebView fullscreen com:
  - carregamento automático da URL
  - links abertos dentro do próprio app
  - JavaScript + DOM Storage habilitados
  - suporte básico de foco para D-pad/controle remoto
- Comportamento de navegação:
  - touch padrão em mobile
  - `Back` volta histórico do WebView; sem histórico fecha app
  - foco garantido para interações em TV
- Tratamento de rede:
  - overlay “Sem conexão”
  - botão “Tentar novamente” para recarregar
- Modo imersivo e tela sempre ligada.
- Recursos visuais mínimos:
  - ícone launcher adaptativo
  - banner Android TV
- README com:
  - execução
  - configuração da URL
  - geração de APK debug/release

## Configuração de URL
A URL base está centralizada em:

- Arquivo: `app/build.gradle.kts`
- Campo: `BuildConfig.WEB_URL`

Exemplo atual:

```kotlin
buildConfigField("String", "WEB_URL", "\"https://hotspot1.edmilsonti.com.br/\"")
```

## Arquivos principais
- `app/src/main/java/com/example/app_edmilson/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `README.md`

## Critérios de aceite atendidos
- Compila sem erros (`assembleDebug`).
- Abre URL automaticamente ao iniciar.
- Roda em mobile e Android TV.
- App aparece no launcher da TV (banner + intent `LEANBACK_LAUNCHER`).
- Exibe tela de erro offline com tentativa de recarga.
- Back navega histórico ou finaliza app.

## Evidência de build
Comando executado:

```powershell
.\gradlew.bat assembleDebug
```

Resultado:
- `BUILD SUCCESSFUL`
- APK debug gerado em:
  - `app/build/outputs/apk/debug/app-debug.apk`

## Riscos/limitações conhecidos no MVP
- Sem monitoramento automático de reconexão em background (reload é manual pelo botão).
- Sem otimizações específicas de performance para páginas muito pesadas.
- Sem suíte de testes instrumentados nesta sprint.

## Próximos passos sugeridos (Sprint 2)
- Automatizar publicação (CI/CD) com variáveis de assinatura `RELEASE_*`.
- Adicionar tela de loading e timeout configurável.
- Implementar testes instrumentados básicos de navegação e estado offline.
