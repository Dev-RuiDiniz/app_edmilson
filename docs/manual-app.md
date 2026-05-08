# Manual do HotSpotTV

## Objetivo
Este manual explica como instalar, configurar e operar o app de TV do cliente.

## O que o app faz
- Recebe um `Codigo TV`.
- Identifica o aparelho automaticamente por `device_id`.
- Consulta a API do cliente.
- Exibe propagandas em playlist.
- Suporta `imagem`, `url`, `html` e `video`.
- Registra na API cada vez que uma propaganda entra em exibicao, quando o item possui `id`.

## Requisitos para uso
- Android ou Android TV compativel com `minSdk 21` (`Android 5.0` ou superior).
- Conexao com internet.
- `Codigo TV` valido.
- API do cliente respondendo nas rotas:
  - `/api/tv/propagandas`
  - `/api/tv/registrar-exibicao`
- O aparelho precisa estar autorizado no limite de TVs definido no painel do cliente.

## Instalacao do APK
1. Envie o APK para o dispositivo.
2. Abra o arquivo APK.
3. Permita a instalacao de fontes externas, se o Android solicitar.
4. Conclua a instalacao.

Observacao:
- Para uso final com cliente, o ideal e enviar um APK `release` assinado.
- Nesta maquina, a assinatura de release depende de `keystore.properties` ou variaveis `RELEASE_*`.
- Enquanto essa assinatura nao estiver configurada, o artefato disponivel para homologacao e o APK `debug`.

## Compatibilidade com TV Box e Fire TV Stick
- Esta versao do app foi ajustada para ampliar a compatibilidade de instalacao em dispositivos com Android antigo.
- O piso e `minSdk 21`, cobrindo aparelhos a partir do Android 5.0.
- Isso melhora a chance de instalacao em TV Box e Fire TV Stick com sistema antigo ou firmware customizado.
- Alguns fabricantes ainda podem bloquear instalacao por politica propria, arquitetura da CPU, armazenamento insuficiente, assinatura do APK ou restricoes do Fire OS.

## Primeiro acesso
1. Abra o app.
2. Aguarde a tela inicial de selecao de codigo.
3. Digite o `Codigo TV` ou escolha um codigo recente.
4. Toque em `Conectar`.

## Codigos recentes
- A tela inicial guarda historico local dos ultimos codigos usados.
- O app exibe no maximo os 3 codigos mais recentes para facilitar reconexao rapida.

## Identificacao do aparelho
- O app envia automaticamente um `device_id` estavel do aparelho em todas as chamadas de TV.
- Esse identificador e usado pelo backend para controlar quantas TVs cada cliente pode autorizar.
- O usuario nao precisa preencher esse identificador manualmente.

## Regras de autorizacao por TV
- Se o aparelho ja estiver autorizado para o cliente, o app carrega normalmente.
- Se for um aparelho novo e ainda houver vagas dentro do `limite_tvs`, o backend autoriza e libera o uso.
- Se for um aparelho novo e o limite estiver esgotado, o app nao abre a playlist.
- Se a API exigir `device_id` e ele nao puder ser obtido, o app tambem nao abre a playlist.

## Mensagens de bloqueio
Quando houver bloqueio por autorizacao, a tela de erro atual do app pode mostrar:

- `Este aparelho nao esta autorizado para este cliente.`
- `Nao foi possivel identificar este aparelho. Verifique a configuracao.`

Nesses casos:
- o conteudo nao sera exibido;
- o usuario pode tentar novamente;
- o usuario pode voltar para a tela inicial.

## Funcionamento da tela de exibicao
- O conteudo roda em playlist continua.
- Enquanto a tela estiver aberta, o app faz refresh periodico da playlist para captar uploads e exclusoes sem reinicio.
- Esse refresh em tempo real vale para imagens, videos e demais itens da playlist.
- Cada propaganda usa seu proprio tempo de exibicao.
- O tempo de `imagem`, `url` e `html` vem da API pelos campos `duracao`, `duration` ou `tempo_exibicao_segundos`.
- Se a API nao informar tempo valido, o app usa o valor padrao configurado no projeto.
- `Video` roda ate o final do arquivo.
- Se a API enviar uma URL de video em qualquer campo de midia, o app detecta a extensao e envia o conteudo para o player.
- O player esta preparado para reproduzir formatos como `mp4`, `m3u8`, `mpd` e `webm`, alem de outras extensoes de video detectadas pela URL.

## Controles na tela
- Clique ou toque na tela para abrir o painel de controle.
- `Trocar`: avanca para a proxima propaganda.
- `Inicio`: volta para a tela inicial de selecao de codigo.
- `Tempo`: mostra o tempo configurado do item atual.
- `Restante`: mostra o contador regressivo do item atual.

## Regras do contador
- Em `imagem`, `url` e `html`, o contador usa o tempo da API ou o fallback do app.
- Em `video`, o contador usa o tempo real retornado pelo player.
- Se a duracao do video nao estiver disponivel, o app mostra que o video esta em reproducao.

## Registro de exibicao
- O app chama a rota de registro quando a propaganda comeca a ser exibida.
- O `device_id` tambem e enviado nessa chamada.
- Regras por tipo:
  - `imagem`: apos carregar a imagem.
  - `url` e `html`: apos o carregamento do `WebView`.
  - `video`: quando o player fica pronto para reproducao.
- Para esse envio funcionar, a API deve retornar `id` em cada propaganda.

## Icone do aplicativo
- O APK atual ja inclui o icone configurado para o cliente no launcher do Android e Android TV.

## Solucao de problemas
- Tela nao carrega:
  - verifique internet e disponibilidade da API.
- Codigo TV invalido:
  - confirme se comeca com `TV` e tem o formato esperado.
- Aparelho bloqueado:
  - confirme no painel se a TV ja foi autorizada ou se o `limite_tvs` do cliente foi atingido.
- Device ID obrigatorio:
  - reinicie o aparelho e teste novamente; se persistir, validar configuracao do Android/TV Box.
- Propaganda sem trocar:
  - verifique se a API retornou `duracao`, `duration` ou `tempo_exibicao_segundos`.
- Video nao abre:
  - confirme se a URL enviada pela API aponta para um arquivo ou stream de video valido e acessivel pela TV.

## Artefatos
- APK debug para homologacao:
  - `app/build/outputs/apk/debug/app-debug.apk`
- APK release assinado:
  - `app/build/outputs/apk/release/app-release.apk`
  - requer assinatura configurada antes da geracao
