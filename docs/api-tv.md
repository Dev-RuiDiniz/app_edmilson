# Documentação da API TV

## Visão geral

A API TV serve para o app de TV, como o aplicativo na TV do estabelecimento, obter a lista de propagandas a exibir e, opcionalmente, registrar quantas vezes cada propaganda passou na TV.

O estabelecimento é identificado pelo código TV. Cada Mikrotik possui um código, configurado no painel do cliente em `TV -> Código TV por Mikrotik`.

Cada propaganda também pode informar sua própria duração de exibição no app.

## Autenticação

A autenticação é obrigatória via `api_key`, enviada de uma destas formas:

- Query string: `api_key=SUA_API_KEY`
- Header: `X-API-Key: SUA_API_KEY`

O valor da chave é configurado em `config/app.php` (`TV_API_KEY`) ou pela variável de ambiente `TV_API_KEY`.

## Base URL

A base URL é a mesma origem do sistema.

Exemplo:

```text
https://seusite.com
```

Todas as rotas abaixo utilizam o método `GET`.

## 1. Listar propagandas

Uso: o app de TV chama este endpoint periodicamente, ou ao iniciar, para obter as propagandas que devem ser exibidas no rotativo.

### Endpoint

```http
GET /api/tv/propagandas
```

### Parâmetros

| Parâmetro | Obrigatório | Descrição |
| --- | --- | --- |
| `codigo` | Sim | Código TV do Mikrotik |
| `api_key` | Sim | Chave da API |

Alternativa: enviar `X-API-Key: SUA_API_KEY` no header e manter apenas `?codigo=XXX` na query string.

### Campos aceitos por item de propaganda

| Campo | Obrigatório | Descrição |
| --- | --- | --- |
| `imagem_url` / `url` / `video_url` / `html` | Sim | Conteúdo a renderizar |
| `duracao` ou `duration` | Não | Tempo de exibição em segundos para `imagem`, `url` e `html` |

Regras:
- O app prioriza `duracao`/`duration` por item quando o campo vier preenchido com valor numérico maior que zero.
- Se o campo não vier, for inválido ou `<= 0`, o app usa o fallback local configurado em `TV_DEFAULT_DISPLAY_DURATION_SECONDS`.
- Vídeos continuam reproduzindo até o fim do arquivo ou stream e só então avançam para o próximo item.

### Resposta 200

```json
{
  "success": true,
  "codigo": "TV1A2B3C4D",
  "propagandas": [
    {
      "id": 1,
      "imagem_url": "https://.../uploads/tv/tv_123_...jpg",
      "duracao": 20,
      "titulo": "Promocao Especial",
      "descricao": "...",
      "ordem": 0
    }
  ]
}
```

### Resposta 400

```json
{"success": false, "error": "Parâmetro codigo é obrigatório"}
```

### Resposta 403

```json
{"success": false, "error": "Acesso negado. API key inválida."}
```

### Exemplo cURL

```bash
curl -G "https://hotspot1.edmilsonti.com.br/api/tv/propagandas" \
  --data-urlencode "codigo=TV1A2B3C4D" \
  --data-urlencode "api_key=SUA_API_KEY"
```

## 2. Registrar exibição

Uso: quando o app de TV exibir uma propaganda na tela, pode chamar este endpoint para incrementar o contador `Quantas vezes passou na TV` no painel do cliente.

Se o app não enviar esta chamada, o contador permanece em `0`.

### Endpoint

```http
GET /api/tv/registrar-exibicao
```

### Parâmetros

| Parâmetro | Obrigatório | Descrição |
| --- | --- | --- |
| `id` | Sim | ID da propaganda retornado em `/api/tv/propagandas` |
| `codigo` | Sim | Código TV do Mikrotik |
| `api_key` | Sim | Chave da API |

### Resposta 200

```json
{"success": true, "message": "Exibição registrada"}
```

### Resposta 404

```json
{"success": false, "error": "Propaganda não encontrada ou código não confere"}
```

Observação: o `id` precisa pertencer a uma propaganda vinculada ao Mikrotik identificado pelo `codigo`.

### Exemplo cURL

```bash
curl -G "https://hotspot1.edmilsonti.com.br/api/tv/registrar-exibicao" \
  --data-urlencode "id=1" \
  --data-urlencode "codigo=TV1A2B3C4D" \
  --data-urlencode "api_key=SUA_API_KEY"
```

## Fluxo sugerido no app de TV

1. Na configuração do app, o usuário informa o código TV e, se necessário, a API key.
2. Ao iniciar o app, ou em intervalo periódico, como a cada 5 minutos, chamar `GET /api/tv/propagandas?codigo=XXX&api_key=YYY`.
3. Exibir as propagandas em rotativo, ordenando pelo campo `ordem`.
4. Utilizar `imagem_url`, `url`, `video_url` ou `html`, conforme o tipo retornado.
5. Quando disponível, aplicar `duracao`/`duration` em segundos por item para `imagem`, `url` e `html`.
6. Opcionalmente, cada vez que uma propaganda for exibida, chamar `GET /api/tv/registrar-exibicao?id=ID&codigo=XXX&api_key=YYY` para atualizar o contador no painel.

## Segurança

- Não exponha a `api_key` em código público.
- No app de TV, prefira obter a chave por configuração segura do estabelecimento.
- Quando possível, utilize um proxy no backend para adicionar a chave às requisições.
