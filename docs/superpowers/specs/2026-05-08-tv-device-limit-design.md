# TV Device Limit Design

**Contexto**

O backend passou a exigir `device_id` nos endpoints de TV e aplica bloqueio por `limite_tvs` por estabelecimento. O app Android precisa acompanhar esse contrato sem criar uma nova tela de bloqueio.

**Objetivo**

Enviar `device_id` em todas as chamadas de TV e tratar bloqueios de autorização usando a tela de erro atual.

**Decisões**

- O app usará `Settings.Secure.ANDROID_ID` como `device_id`.
- A leitura do identificador ficará centralizada em um provider dedicado.
- O repositório será responsável por:
  - anexar `device_id` às URLs de conteúdo e registro de exibição;
  - identificar respostas de API com `success=false`;
  - mapear `device_id_obrigatorio=true` e `limite_tvs_atingido=true` para erros tipados.
- A UI continuará usando o estado de erro existente, mas manterá a causa tipada para converter em mensagens amigáveis.

**Fluxo**

1. `RendererActivity` inicia o carregamento do código TV.
2. `TvContentRepository` resolve o `device_id` e monta a URL final com o parâmetro.
3. Se a API retornar bloqueio:
   - `device_id_obrigatorio=true` -> erro `DeviceIdRequired`
   - `limite_tvs_atingido=true` -> erro `TvLimitReached`
4. `RendererActivity` mostra a tela de erro atual com mensagem específica e não renderiza a playlist.

**Mensagens**

- Limite atingido: "Este aparelho não está autorizado para este cliente."
- Device ID ausente: "Não foi possível identificar este aparelho. Verifique a configuração."

**Teste**

- Validar que o repositório adiciona `device_id` nas URLs.
- Validar que o mapeamento de resposta da API cria os erros tipados corretos.
- Validar build e testes unitários do módulo `app`.
