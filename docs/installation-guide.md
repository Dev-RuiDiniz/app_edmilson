# Guia de instalacao do HotSpotTV

## Objetivo
Orientar a instalacao manual do APK do HotSpotTV em Android TV, TV Box, Fire TV Stick e telefone Android.

## Arquivos possiveis
- `HotSpotTV-release.apk`
- `HotSpotTV-debug-homologacao.apk`

## Qual APK usar
- `release`: usar para entrega final ao cliente quando a assinatura estiver configurada.
- `debug-homologacao`: usar para teste e validacao quando o release assinado ainda nao estiver disponivel.

## Instalacao direta no dispositivo
1. Copie o APK para o dispositivo por pendrive, download direto, compartilhamento de rede ou app de arquivos.
2. Abra o APK no gerenciador de arquivos do aparelho.
3. Autorize a instalacao por fontes desconhecidas, se o sistema solicitar.
4. Conclua a instalacao.
5. Abra o app `HotSpotTV`.

## Instalacao com ADB
```powershell
adb connect <ip-ou-host-do-dispositivo>
adb install -r HotSpotTV-release.apk
```

Para homologacao com o pacote debug:

```powershell
adb install -r HotSpotTV-debug-homologacao.apk
```

## Verificacoes apos instalar
1. Confirmar se o app aparece no launcher.
2. Abrir o app e informar um `Codigo TV` valido.
3. Confirmar carregamento da playlist.
4. Testar retorno ao inicio e troca manual de conteudo.
5. Validar se um aparelho nao autorizado mostra mensagem de bloqueio.

## Solucao rapida de problemas
- `INSTALL_FAILED_OLDER_SDK`:
  - confirmar se o dispositivo roda Android 5.0 ou superior.
- instalacao bloqueada:
  - habilitar fontes desconhecidas.
- app nao abre ou fecha:
  - reinstalar o APK e limpar dados do aplicativo.
- aparelho bloqueado:
  - verificar no painel se o `limite_tvs` do cliente foi atingido.
- midia nao carrega:
  - validar internet e disponibilidade da API.
