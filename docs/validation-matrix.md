# Matriz de validação do HotSpotTV

## Validações locais concluídas
- Build debug: aprovado
- Build release assinado: aprovado
- Testes unitários: aprovados
- Compilação dos testes instrumentados: aprovada

## Homologação manual recomendada
- TV Box do cliente:
  - instalar APK release
  - abrir app
  - inserir código válido
  - validar imagem, HTML, URL e vídeo
- Fire TV Stick:
  - instalar APK release
  - validar launcher e navegação por controle remoto
- Android TV:
  - validar fluxo completo e overlay de controle
- Telefone Android:
  - validar instalação, abertura e renderização

## Erros que devem ser capturados se ocorrerem
- falha de instalação por versão do Android
- falha de instalação por assinatura
- mídia inválida na playlist
- erro de WebView
- erro de vídeo
- ausência de conectividade com a API
