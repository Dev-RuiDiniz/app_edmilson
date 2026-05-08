# Resumo de entrega do HotSpotTV

## Conteudo do pacote final
- `HotSpotTV-release.apk`
- `manual-hotspottv-cliente.pdf`
- `guia-instalacao.pdf`

## Conteudo disponivel nesta maquina hoje
- `app-debug.apk` para homologacao
- documentacao atualizada com fluxo de `device_id`
- manual do cliente em PDF

## Escopo desta entrega
- package final `com.hotspottv`
- compatibilidade ampliada para Android 5.0+
- tratamento reforcado para falhas de midia e WebView
- envio obrigatorio de `device_id` nas chamadas TV
- bloqueio no app quando a API retornar `limite_tvs_atingido`
- bloqueio no app quando a API retornar `device_id_obrigatorio`
- cobertura ampliada de testes unitarios

## Observacao sobre APK release
- O APK `release` depende de assinatura configurada via `keystore.properties` ou variaveis `RELEASE_*`.
- Sem essa assinatura, o build `assembleRelease` falha por protecao do projeto.
- Ate a configuracao da assinatura, o artefato disponivel para validacao e o APK `debug`.

## Validacoes executadas localmente
- `assembleDebug`
- `testDebugUnitTest`
- `testDebugUnitTest assembleDebug`

## Homologacoes pendentes em campo
- instalacao do pacote final assinado no dispositivo do cliente
- validacao do fluxo de limite de TVs em hardware real
- validacao do bloqueio para aparelho nao autorizado
