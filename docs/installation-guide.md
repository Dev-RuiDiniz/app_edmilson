# Guia de instalação do HotSpotTV

## Objetivo
Orientar a instalação manual do APK release do HotSpotTV em Android TV, TV Box, Fire TV Stick e telefone Android.

## Arquivo esperado
- `HotSpotTV-release.apk`

## Instalação direta no dispositivo
1. Copie o APK para o dispositivo por pendrive, download direto, compartilhamento de rede ou app de arquivos.
2. Abra o APK no gerenciador de arquivos do aparelho.
3. Autorize a instalação por fontes desconhecidas, se o sistema solicitar.
4. Conclua a instalação.
5. Abra o app `HotSpotTV`.

## Instalação com ADB
```powershell
adb connect <ip-ou-host-do-dispositivo>
adb install -r HotSpotTV-release.apk
```

## Verificações após instalar
1. Confirmar se o app aparece no launcher.
2. Abrir o app e informar um código TV válido.
3. Confirmar carregamento da playlist.
4. Testar retorno ao início e troca manual de conteúdo.

## Solução rápida de problemas
- `INSTALL_FAILED_OLDER_SDK`:
  - confirmar se o dispositivo roda Android 5.0 ou superior.
- instalação bloqueada:
  - habilitar fontes desconhecidas.
- app não abre ou fecha:
  - reinstalar o APK e limpar dados do aplicativo.
- mídia não carrega:
  - validar internet e disponibilidade da API.
