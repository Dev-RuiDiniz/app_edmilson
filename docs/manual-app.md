# Manual do HotSpotTV

## Objetivo
Este manual explica como instalar, configurar e operar o app de TV do cliente.

## O que o app faz
- Recebe um `Código TV`.
- Consulta a API do cliente.
- Exibe propagandas em playlist.
- Suporta `imagem`, `url`, `html` e `video`.
- Registra na API cada vez que uma propaganda entra em exibição, quando o item possui `id`.

## Requisitos para uso
- Android ou Android TV compatível com `minSdk 21` (`Android 5.0` ou superior).
- Conexão com internet.
- Código TV válido.
- API do cliente respondendo nas rotas:
  - `/api/tv/propagandas`
  - `/api/tv/registrar-exibicao`

## Instalação do APK
1. Envie o APK release do HotSpotTV para o dispositivo.
2. Abra o arquivo APK.
3. Permita a instalação de fontes externas, se o Android solicitar.
4. Conclua a instalação.

## Compatibilidade com TV Box e Fire TV Stick
- Esta versão do app foi ajustada para ampliar a compatibilidade de instalação em dispositivos com Android antigo.
- O piso passou de `minSdk 26` para `minSdk 21`, cobrindo aparelhos a partir do Android 5.0.
- Isso melhora a chance de instalação em TV Box e Fire TV Stick com sistema antigo ou firmware customizado.
- Ainda assim, alguns fabricantes podem bloquear instalação por política própria, arquitetura da CPU, armazenamento insuficiente, assinatura do APK ou restrições do Fire OS.
- Para diagnóstico em campo, prefira validar a mensagem do sistema ou usar `adb install` para capturar o erro exato.

## Primeiro acesso
1. Abra o app.
2. Aguarde a tela inicial de seleção de código.
3. Digite o `Código TV` ou escolha um código recente.
4. Toque em `Conectar`.

## Funcionamento da tela de exibição
- O conteúdo roda em playlist contínua.
- Enquanto a tela estiver aberta, o app faz refresh periódico da playlist para captar uploads e exclusões sem reinício.
- Cada propaganda usa seu próprio tempo de exibição.
- O tempo de `imagem`, `url` e `html` vem da API pelos campos `duracao`, `duration` ou `tempo_exibicao_segundos`.
- Se a API não informar tempo válido, o app usa o valor padrão configurado no projeto.
- `Video` roda até o final do arquivo.
- Se a API enviar uma URL de vídeo em qualquer campo de mídia, o app detecta a extensão e envia o conteúdo para o player.
- O player está preparado para reproduzir formatos como `mp4`, `m3u8`, `mpd` e `webm`, além de outras extensões de vídeo detectadas pela URL.

## Controles na tela
- Clique ou toque na tela para abrir o painel de controle.
- `Trocar`: avança para a próxima propaganda.
- `Início`: volta para a tela inicial de seleção de código.
- `Tempo`: mostra o tempo configurado do item atual.
- `Restante`: mostra o contador regressivo do item atual.

## Regras do contador
- Em `imagem`, `url` e `html`, o contador usa o tempo da API ou o fallback do app.
- Em `video`, o contador usa o tempo real retornado pelo player.
- Se a duração do vídeo não estiver disponível, o app mostra que o vídeo está em reprodução.

## Registro de exibição
- O app chama a rota de registro quando a propaganda começa a ser exibida.
- Regras por tipo:
  - `imagem`: após carregar a imagem.
  - `url` e `html`: após o carregamento do `WebView`.
  - `video`: quando o player fica pronto para reprodução.
- Para esse envio funcionar, a API deve retornar `id` em cada propaganda.

## Ícone do aplicativo
- O APK atual já inclui o ícone configurado para o cliente no launcher do Android e Android TV.

## Solução de problemas
- Tela não carrega:
  - verifique internet e disponibilidade da API.
- Código TV inválido:
  - confirme se começa com `TV` e tem o formato esperado.
- Propaganda sem trocar:
  - verifique se a API retornou `duracao`, `duration` ou `tempo_exibicao_segundos`.
- Vídeo não abre:
  - confirme se a URL enviada pela API aponta para um arquivo ou stream de vídeo válido e acessível pela TV.
- Contador do painel não aparece correto:
  - confirme se o item é `video` com duração detectável ou se a API está enviando a duração para conteúdos não-vídeo.

## Artefato para envio ao cliente
- APK release assinado:
  - `app/build/outputs/apk/release/app-release.apk`
