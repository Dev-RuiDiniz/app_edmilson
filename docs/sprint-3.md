# Sprint 3 - Playlist de Midia + Ajuste de Launcher Icon

## Periodo
- Data de consolidacao: 06/03/2026
- Sprint: 3

## Objetivo
Garantir exibicao continua de conteudo da API em formato playlist (imagem/video/url/html), consolidar o controle visual do player e corrigir o dimensionamento do launcher icon para Android Phone e Android TV.

## Escopo entregue
- Player orientado a playlist:
  - suporte a lista de midias retornada em `propagandas`;
  - ciclo continuo entre itens;
  - deduplicacao por tipo+URL, preservando ordem da API.
- Regras de reproducao:
  - imagem: usa `duracao`/`duration` da API quando disponivel;
  - url/html: usam `duracao`/`duration` da API quando disponivel;
  - sem duracao valida para imagem/url/html, usa fallback global configurado;
  - video: reproduz ate o fim e avanca;
  - se houver somente um video, reinicia automaticamente (loop de playlist).
- Controles na renderizacao:
  - overlay exibido sob clique/toque;
  - botao `Trocar` para avancar para a proxima propaganda;
  - botao `Inicio` para voltar para a tela de selecao de codigo;
  - contador `Restante` para o item atual;
  - em video, o tempo exibido acompanha a duracao real do player quando disponivel.
- Registro de exibicao:
  - cada propaganda com `id` pode ser enviada para `/api/tv/registrar-exibicao`;
  - o envio ocorre uma vez por exibicao de item.
- Cache atualizado:
  - persistencia de playlist completa em `SharedPreferences`;
  - persistencia da duracao por item para compatibilidade com a resposta da API;
  - retrocompatibilidade com cache antigo de item unico.
- Launcher icon:
  - adaptive icon mantido em `mipmap-anydpi-v26`;
  - arte-base atualizada para `icone.jpeg`;
  - foreground com `inset` de seguranca;
  - icones legacy (`mipmap-*`) regenerados com margem central para evitar corte/compressao.

## Arquivos principais alterados
- `app/src/main/java/com/example/app_edmilson/RendererActivity.kt`
- `app/src/main/java/com/example/app_edmilson/data/model/TvContentParser.kt`
- `app/src/main/java/com/example/app_edmilson/data/model/TvContentResponseDto.kt`
- `app/src/main/java/com/example/app_edmilson/data/repository/TvContentRepository.kt`
- `app/src/test/java/com/example/app_edmilson/data/model/TvContentParserTest.kt`
- `app/src/test/java/com/example/app_edmilson/data/repository/CachedTvContentTest.kt`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-*/ic_launcher*.png`
- `README.md`

## Validacao tecnica
Comandos executados:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Resultado:
- `BUILD SUCCESSFUL` em testes unitarios.
- `BUILD SUCCESSFUL` em `assembleDebug`.

## Resultado funcional esperado
- API com multiplos itens: app percorre todos em loop.
- Sequencia imagem + video:
  - imagem pelo tempo enviado em `duracao`/`duration`, ou fallback global;
  - video ate terminar;
  - proximo item automaticamente.
- Um unico video:
  - toca inteiro;
  - reinicia automaticamente sem travar em tela de erro.
- URL/HTML:
  - usam a duracao por item quando a API informar;
  - sem duracao valida, usam o fallback global configurado.
