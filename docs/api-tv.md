# Documentacao da API TV do HotSpotTV

## Visao geral

A API TV serve para o app de TV obter a lista de propagandas a exibir e registrar quantas vezes cada propaganda passou na TV.

O estabelecimento e identificado pelo `codigo TV`. Cada aparelho tambem e identificado por um `device_id`, usado para controlar o limite de TVs autorizadas por cliente.

Cada propaganda tambem pode informar sua propria duracao de exibicao no app.

## Autenticacao

A autenticacao e obrigatoria via `api_key`, enviada de uma destas formas:

- Query string: `api_key=SUA_API_KEY`
- Header: `X-API-Key: SUA_API_KEY`

O valor da chave e configurado em `config/app.php` (`TV_API_KEY`) ou pela variavel de ambiente `TV_API_KEY`.

## Base URL

A base URL e a mesma origem do sistema.

Exemplo:

```text
https://seusite.com
```

Todas as rotas abaixo utilizam o metodo `GET`.

## 1. Listar propagandas

Uso: o app de TV chama este endpoint ao iniciar e periodicamente para obter as propagandas que devem ser exibidas no rotativo.

### Endpoint

```http
GET /api/tv/propagandas
```

### Parametros

| Parametro | Obrigatorio | Descricao |
| --- | --- | --- |
| `codigo` | Sim | Codigo TV do Mikrotik |
| `device_id` | Sim | Identificador estavel do aparelho |
| `api_key` | Sim | Chave da API |

Alternativa: enviar `X-API-Key: SUA_API_KEY` no header e manter apenas `codigo` e `device_id` na query string.

### Regras de autorizacao por dispositivo

- Se `device_id` ja estiver autorizado para o cliente, a API libera.
- Se for um `device_id` novo e ainda couber no `limite_tvs`, a API autoriza e grava o vinculo.
- Se for um `device_id` novo e o limite estiver esgotado, a API bloqueia.
- Se `device_id` nao for enviado, a API rejeita a chamada.

### Campos aceitos por item de propaganda

| Campo | Obrigatorio | Descricao |
| --- | --- | --- |
| `imagem_url` / `url` / `video_url` / `html` | Sim | Conteudo a renderizar |
| `duracao` / `duration` / `tempo_exibicao_segundos` | Nao | Tempo de exibicao em segundos enviado pela API |

Regras:
- O app usa `duracao`/`duration`/`tempo_exibicao_segundos` para `imagem`, `url` e `html` quando o valor for numerico e maior que zero.
- Se esse campo nao vier, for invalido ou `<= 0`, o app usa o fallback local configurado em `TV_DEFAULT_DISPLAY_DURATION_SECONDS`.
- Videos continuam reproduzindo ate o fim do arquivo ou stream e so entao avancam para o proximo item.

### Resposta 200

```json
{
  "success": true,
  "codigo": "TV1A2B3C4D",
  "propagandas": [
    {
      "id": 1,
      "imagem_url": "https://.../uploads/tv/tv_123_...jpg",
      "tempo_exibicao_segundos": 20,
      "titulo": "Promocao Especial",
      "descricao": "...",
      "ordem": 0
    }
  ]
}
```

### Resposta de bloqueio por limite

```json
{
  "success": false,
  "limite_tvs_atingido": true,
  "message": "Limite de TVs atingido"
}
```

### Resposta de `device_id` ausente

```json
{
  "success": false,
  "device_id_obrigatorio": true,
  "error": "device_id obrigatorio"
}
```

### Exemplo cURL

```bash
curl -G "https://hotspot1.edmilsonti.com.br/api/tv/propagandas" \
  --data-urlencode "codigo=TV1A2B3C4D" \
  --data-urlencode "device_id=ANDROID-ID-DA-TV" \
  --data-urlencode "api_key=SUA_API_KEY"
```

## 2. Registrar exibicao

Uso: quando o app de TV exibir uma propaganda na tela, ele chama este endpoint para incrementar o contador no painel do cliente.

### Endpoint

```http
GET /api/tv/registrar-exibicao
```

### Parametros

| Parametro | Obrigatorio | Descricao |
| --- | --- | --- |
| `id` | Sim | ID da propaganda retornado em `/api/tv/propagandas` |
| `codigo` | Sim | Codigo TV do Mikrotik |
| `device_id` | Sim | Identificador estavel do aparelho |
| `api_key` | Sim | Chave da API |

### Resposta 200

```json
{"success": true, "message": "Exibicao registrada"}
```

### Resposta de bloqueio

```json
{
  "success": false,
  "limite_tvs_atingido": true,
  "message": "Limite de TVs atingido"
}
```

Observacao: o `id` precisa pertencer a uma propaganda vinculada ao Mikrotik identificado pelo `codigo`.

Regra de uso no app:
- `imagem`: envia o registro apos o carregamento bem-sucedido.
- `url` e `html`: envia o registro apos o `WebView` concluir o carregamento.
- `video`: envia o registro quando o player entra em estado pronto para reproducao.
- O envio ocorre uma vez por exibicao do item na playlist.

### Exemplo cURL

```bash
curl -G "https://hotspot1.edmilsonti.com.br/api/tv/registrar-exibicao" \
  --data-urlencode "id=1" \
  --data-urlencode "codigo=TV1A2B3C4D" \
  --data-urlencode "device_id=ANDROID-ID-DA-TV" \
  --data-urlencode "api_key=SUA_API_KEY"
```

## Fluxo sugerido no app de TV

1. Na configuracao do app, o usuario informa o `codigo TV`.
2. O app obtem automaticamente um `device_id` estavel do aparelho.
3. Ao iniciar o app, e enquanto a tela de exibicao estiver aberta, chamar `GET /api/tv/propagandas`.
4. Se a API responder com `limite_tvs_atingido=true` ou `device_id_obrigatorio=true`, o app nao deve abrir a playlist.
5. Cada vez que uma propaganda for exibida, chamar `GET /api/tv/registrar-exibicao`.

## Seguranca

- Nao exponha a `api_key` em codigo publico.
- O `device_id` deve ser enviado em todas as chamadas de TV.
- O app deve tratar respostas de bloqueio sem usar cache para contornar a restricao.
