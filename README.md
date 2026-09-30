# IPTVEC — M3U corrigido

Esta versão corrige o carregamento da lista M3U no Android:
- URL M3U é baixada pelo Android nativo, evitando o bloqueio de CORS do WebView.
- Também permite selecionar um arquivo .m3u/.m3u8 no celular.
- A lista é analisada e agrupada por `group-title`.
- Os canais são enviados para o player HLS/M3U8 existente.
- Android 7.0+ (minSdk 24).
- Tela cheia.

## Como gerar
No GitHub, envie estes arquivos para o repositório e execute:
Actions > Gerar APK IPTVEC > Run workflow.

O APK estará no Artifact `IPTVEC-debug`.


## Lista integrada
A URL M3U fornecida pelo usuário foi integrada ao aplicativo. Ao abrir o IPTVEC, o Android tenta carregar automaticamente a lista pelo método nativo, evitando o fetch/CORS do WebView.
Também permanece disponível o carregamento manual por URL e por arquivo `.m3u`.
