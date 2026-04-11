# Compatibilidade de dispositivos

## Objetivo
Registrar o ajuste de compatibilidade de instalação do app em Android TV, TV Box e Fire TV Stick.

## Mudança aplicada
- `minSdk` reduzido de `26` para `21`.
- Nova faixa mínima suportada: `Android 5.0` ou superior.

## Motivação
- Houve falha de instalação reportada em dispositivo do cliente com:
  - Android `13.1`
  - patch de segurança `05/04/2022`
  - kernel `3.10.104`
- Em paralelo, foi identificado que o projeto exigia Android `8.0+`, o que reduz a compatibilidade com boxes e sticks baseados em Android antigo, Fire OS customizado ou builds de fabricante com APIs limitadas.

## Impacto esperado
- Aumentar a chance de instalação em TV Box e Fire TV Stick fora do ecossistema de Smart TVs homologadas.
- Manter compatibilidade com celulares e Android TV já suportados.

## Limites conhecidos
- Reduzir `minSdk` melhora a compatibilidade, mas não garante instalação em todos os aparelhos.
- O processo ainda pode falhar por:
  - APK não assinado para distribuição
  - bloqueio de fontes desconhecidas
  - incompatibilidade de arquitetura do dispositivo
  - restrições do Fire OS
  - falta de espaço ou conflito com versão anterior instalada

## Validação recomendada
1. Testar instalação manual em pelo menos um TV Box e um Fire TV Stick.
2. Capturar o retorno de `adb install -r <apk>` nos casos de falha.
3. Confirmar abertura do app, digitação do código e renderização de `url`, `html`, `image` e `video`.
