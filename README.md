# app_edmilson

App Android (celular + Android TV) em Kotlin para fluxo:

1. inserir/selecionar **Código TV**
2. consultar API por código
3. renderizar conteúdo (URL/HTML/Imagem/Vídeo)

## Requisitos
- Min SDK: 26
- Compile SDK: 36
- Target SDK: 36
- UI em XML + Activities

## Fluxo
1. `MainActivity` exibe input + lista de códigos recentes.
2. Código válido abre `RendererActivity`.
3. `RendererActivity` chama endpoint configurável por código.
4. O retorno é renderizado:
   - `type = "url"` -> `WebView.loadUrl`
   - `type = "html"` -> `WebView.loadDataWithBaseURL`
   - `type = "image"` -> `ImageView` com Coil
   - `type = "video"` (ou URL com extensão de vídeo) -> `PlayerView` + ExoPlayer
   - sem `type` + `url` válida -> assume URL

## Lógica do sistema
- Entrada do código:
  - normaliza para maiúsculo (`TV...`)
  - valida prefixo `TV` e tamanho mínimo de 6 caracteres
  - salva histórico local dos últimos códigos usados
- Consulta API:
  - usa `Retrofit + OkHttp`
  - endpoint montado por `API_TV_CONTENT_PATH_TEMPLATE` com substituição `%s` ou `{code}`
  - código é URL-encoded antes da chamada
- Parse de resposta:
  - suporta resposta completa (`code`, `type`, `url/html/imageUrl`) e simples (`url`)
  - se `type` vier inconsistente (ex.: `type=url` sem `url`), aplica fallback automático para outros campos válidos
- Cache:
  - salva o último conteúdo renderizável por código (memória + `SharedPreferences`)
  - usa cache quando API falha por status HTTP, exceção de rede, resposta vazia ou payload sem conteúdo renderizável
- Renderização:
  - `url` e `html` em `WebView`
  - `image` em `ImageView` (Coil)
  - `video` em `PlayerView` (ExoPlayer), com autoplay e loop
  - estado de `Loading`, `Success`, `Error`, com ação de recarregar e trocar código

## API e BuildConfig
Arquivo: `app/build.gradle.kts`

Variáveis suportadas (Gradle property ou env var):
- `API_BASE_URL` (ex.: `https://hotspot1.edmilsonti.com.br`)
- `API_TV_CONTENT_PATH_TEMPLATE` (default: `api/tv/propagandas?codigo={code}&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a`)

Exemplos de template aceitos:
- `api/tv/propagandas?codigo={code}`
- `api/tv/propagandas?codigo=%s`
- `api/tv/propagandas?codigo={code}&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a`
- `https://hotspot1.edmilsonti.com.br/api/tv/propagandas?codigo={code}`

Regra de montagem da URL:
- URL da API = Base + endpoint.
- Ex.: `https://hotspot1.edmilsonti.com.br` + `api/tv/propagandas?codigo=TV2665487D&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a`.

## Build
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK debug:
- `app/build/outputs/apk/debug/app-debug.apk`

## Teste no BlueStacks
Com BlueStacks aberto:

```powershell
adb connect 127.0.0.1:5555
adb -s 127.0.0.1:5555 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s 127.0.0.1:5555 shell monkey -p com.example.app_edmilson -c android.intent.category.LAUNCHER 1
```

## Testes
- Testes unitários atuais:
  - parse da API (resposta padrão e resposta simples)
  - fallback de parse quando `type` não casa com o payload
  - validação de código TV
- Executar:
```powershell
.\gradlew.bat testDebugUnitTest
```
