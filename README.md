# HotSpotTV

App Android (celular + Android TV) em Kotlin para fluxo:

1. inserir/selecionar `Codigo TV`
2. identificar o aparelho com `device_id`
3. consultar API por codigo
4. renderizar conteudo em playlist

## Requisitos
- Min SDK: 21
- Compile SDK: 36
- Target SDK: 36
- JDK: 21 para compilar localmente
- UI em XML + Activities

## Fluxo
1. `MainActivity` exibe input + lista de codigos recentes.
2. Codigo valido abre `RendererActivity`.
3. `RendererActivity` chama endpoint configuravel por codigo.
4. O retorno e renderizado:
   - `type = "url"` -> `WebView.loadUrl`
   - `type = "html"` -> `WebView.loadDataWithBaseURL`
   - `type = "image"` -> `ImageView` com Coil
   - `type = "video"` -> `PlayerView` + ExoPlayer

## Logica do sistema
- Entrada do codigo:
  - normaliza para maiusculo (`TV...`)
  - valida prefixo `TV` e tamanho minimo de 6 caracteres
  - salva historico local dos ultimos codigos usados
  - exibe no maximo os 3 codigos recentes na tela inicial
- Identificacao do aparelho:
  - usa `Settings.Secure.ANDROID_ID` como `device_id`
  - envia `device_id` em `/api/tv/propagandas`
  - envia `device_id` em `/api/tv/registrar-exibicao`
- Consulta API:
  - usa `Retrofit + OkHttp`
  - endpoint montado por `API_TV_CONTENT_PATH_TEMPLATE` com substituicao `%s` ou `{code}`
  - endpoint de registro montado por `API_TV_REGISTER_DISPLAY_PATH_TEMPLATE` com substituicao `{id}` e `{code}`
- Bloqueios de autorizacao:
  - se a API responder `limite_tvs_atingido=true`, o app nao abre a playlist
  - se a API responder `device_id_obrigatorio=true`, o app nao abre a playlist
  - nesses casos o app usa a tela de erro atual com mensagem especifica
  - erros de autorizacao nao usam fallback de cache
- Cache:
  - salva a ultima playlist renderizavel por codigo
  - usa cache apenas em falhas genericas de rede, HTTP ou resposta invalida
- Atualizacao:
  - enquanto a tela de `RendererActivity` estiver aberta, o app refaz a consulta periodicamente
  - a reconciliacao atualiza imagens, videos e demais itens sem exigir reinicio do app

## API e BuildConfig
Arquivo: `app/build.gradle.kts`

Variaveis suportadas:
- `API_BASE_URL`
- `API_TV_CONTENT_PATH_TEMPLATE`
- `API_TV_REGISTER_DISPLAY_PATH_TEMPLATE`
- `TV_DEFAULT_DISPLAY_DURATION_SECONDS`
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

### Contrato atual da API TV

Chamadas esperadas:

```text
/api/tv/propagandas?codigo=...&device_id=...&api_key=...
/api/tv/registrar-exibicao?id=...&codigo=...&device_id=...&api_key=...
```

Flags especiais de bloqueio:
- `limite_tvs_atingido=true`
- `device_id_obrigatorio=true`

## Build
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat assembleRelease
```

Observacao:
- `assembleRelease` exige assinatura configurada.
- Sem `keystore.properties` ou `RELEASE_*`, o build release falha por regra do projeto.

## Artefatos

APK debug:
- `app/build/outputs/apk/debug/app-debug.apk`

APK release:
- `app/build/outputs/apk/release/app-release.apk`
- requer assinatura configurada

## Testes
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat testDebugUnitTest assembleDebug`

## Documentacao
- `docs/manual-app.md`
- `docs/api-tv.md`
- `docs/installation-guide.md`
- `docs/client-delivery.md`
- `docs/device-compatibility.md`
- `docs/release-signing.md`
