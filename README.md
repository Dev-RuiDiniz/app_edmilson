# app_edmilson

App Android (celular + Android TV) em Kotlin para fluxo:

1. inserir/selecionar **Código TV**
2. consultar API por código
3. renderizar conteúdo em playlist (URL/HTML/Imagem/Vídeo)

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
  - endpoint de registro montado por `API_TV_REGISTER_DISPLAY_PATH_TEMPLATE` com substituição `{id}` e `{code}`
  - código é URL-encoded antes da chamada
- Parse de resposta:
  - suporta resposta completa (`id`, `code`, `type`, `url/html/imageUrl`) e simples (`url`)
  - suporta item único (`data`, `propaganda`) e lista (`propagandas`)
  - se `type` vier inconsistente (ex.: `type=url` sem `url`), aplica fallback automático para outros campos válidos
  - remove duplicados de mídia na lista final mantendo ordem de chegada da API
- Cache:
  - salva a última playlist renderizável por código (memória + `SharedPreferences`)
  - usa cache quando API falha por status HTTP, exceção de rede, resposta vazia ou payload sem conteúdo renderizável
- Renderização:
  - `url` e `html` em `WebView`
  - `image` em `ImageView` (Coil)
  - `video` em `PlayerView` (ExoPlayer), com autoplay
- `image`, `url` e `html` usam `duracao`/`duration`/`tempo_exibicao_segundos` da API quando disponível
  - fallback para `image`, `url` e `html`: `TV_DEFAULT_DISPLAY_DURATION_SECONDS`
  - `video` toca até o fim e então avança para o próximo item
  - o overlay de controle aparece ao clicar/tocar na tela
  - o overlay mostra `Tempo`, contador `Restante`, botão `Trocar` e botão `Início`
  - em vídeo, o contador e o tempo exibido usam a duração real do arquivo quando disponível
  - o app chama `/api/tv/registrar-exibicao` a cada vez que uma propaganda entra em exibição
  - quando há somente 1 vídeo, o player reinicia automaticamente em loop de playlist
  - estado de `Loading`, `Success`, `Error`, com ação de voltar ao início no overlay de erro

## Launcher Icon (Android/Android TV)
- Adaptive icon ativo em:
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Arte-base do launcher:
  - `icone.jpeg`
- Foreground com `inset` seguro para manter a arte inteira visível:
  - `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Ícones legados (`mipmap-mdpi` até `mipmap-xxxhdpi`) regenerados com conteúdo centralizado e margem de segurança, evitando corte de texto em launchers com máscaras diferentes.

## API e BuildConfig
Arquivo: `app/build.gradle.kts`

Variáveis suportadas (Gradle property ou env var):
- `API_BASE_URL` (ex.: `https://hotspot1.edmilsonti.com.br`)
- `API_TV_CONTENT_PATH_TEMPLATE` (default: `api/tv/propagandas?codigo={code}&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a`)
- `API_TV_REGISTER_DISPLAY_PATH_TEMPLATE` (default: `api/tv/registrar-exibicao?id={id}&codigo={code}&api_key=...`)
- `TV_DEFAULT_DISPLAY_DURATION_SECONDS` (default: `30`)

Exemplos de template aceitos:
- `api/tv/propagandas?codigo={code}`
- `api/tv/propagandas?codigo=%s`
- `api/tv/propagandas?codigo={code}&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a`
- `https://hotspot1.edmilsonti.com.br/api/tv/propagandas?codigo={code}`

Regra de montagem da URL:
- URL da API = Base + endpoint.
- Ex.: `https://hotspot1.edmilsonti.com.br` + `api/tv/propagandas?codigo=TV2665487D&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a`.

Contrato de duração por item:
- A API pode enviar `duracao`, `duration` ou `tempo_exibicao_segundos` em segundos para cada item de `propagandas`.
- O app converte esse valor para milissegundos internamente.
- Para `image`, `url` e `html`, o app usa o valor enviado pela API quando ele for maior que zero.
- Quando esse valor não vier, for inválido ou `<= 0`, o app usa `TV_DEFAULT_DISPLAY_DURATION_SECONDS`.
- Para `video`, o app usa a duração real do player; se indisponível, segue até o fim da reprodução.

Contrato de registro de exibição:
- A API deve retornar `id` para cada propaganda que precise ser contabilizada.
- O app chama `/api/tv/registrar-exibicao` uma vez por renderização de item exibido.
- Para `image`, o envio acontece após a imagem carregar.
- Para `url`/`html`, o envio acontece após o `WebView` concluir o carregamento.
- Para `video`, o envio acontece quando o player entra em `STATE_READY`.

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
  - montagem de playlist com múltiplas propagandas em ordem
  - validação de código TV
- Executar:
```powershell
.\gradlew.bat testDebugUnitTest
```

## Documentação
- `docs/sprint-1.md`
- `docs/sprint-2.md`
- `docs/sprint-3.md`
- `docs/release-signing.md`
- `docs/manual-app.md`
- `docs/api-tv.md`
