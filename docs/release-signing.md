# Release signing e publicação

## Objetivo
Padronizar a geração de artefatos de produção assinados para distribuição no Google Play.

## Pré-requisitos
- Java/JDK com `keytool`
- JDK 21 para executar o build com Android Gradle Plugin 9
- Android Studio (opcional) ou Gradle Wrapper
- Keystore de upload gerada

## Configuração suportada pelo projeto
`app/build.gradle.kts` aceita assinatura de release por:
- arquivo `keystore.properties` na raiz do projeto
- variáveis de ambiente `RELEASE_*`

Campos obrigatórios:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Sem esses campos, tarefas `Release` e `bundle` falham propositalmente.

## Artefatos
- AAB (recomendado para Play): `app/build/outputs/bundle/release/app-release.aab`
- APK assinado: `app/build/outputs/apk/release/app-release.apk`

## Comandos
```powershell
.\gradlew.bat clean bundleRelease
.\gradlew.bat assembleRelease
```

## Checklist de publicação
1. Confirmar endpoints de produção em:
   - `BuildConfig.API_BASE_URL`
   - `BuildConfig.API_TV_CONTENT_PATH_TEMPLATE`
   - `BuildConfig.TV_DEFAULT_DISPLAY_DURATION_SECONDS`
2. Incrementar `versionCode` e atualizar `versionName`
3. Gerar `bundleRelease`
4. Validar app em dispositivo real
5. Subir AAB no Google Play Console
6. Preencher Data Safety, conteúdo e classificação etária
7. Publicar em trilha interna antes da produção
