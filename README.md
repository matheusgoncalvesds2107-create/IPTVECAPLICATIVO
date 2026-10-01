# IPTVEC — Lista M3U grande

Projeto Android criado para listas M3U muito grandes.

Características:
- Leitura nativa em streaming, sem carregar os 350 mil itens todos na memória do WebView.
- SQLite local para indexação.
- Contagem por `group-title`.
- Arquivo M3U local pelo seletor do Android.
- URL M3U.
- Player HLS.
- Interface preparada para busca e paginação.

A lista enviada pelo usuário possui cerca de 350.557 entradas EXTINF e 100 grupos. Ela NÃO é embutida no APK, evitando transformar o APK em um arquivo enorme.


## Correção V2.1
Corrigido o erro Kotlin `Unresolved reference: count` no final do processamento da lista. O total agora é obtido diretamente do SQLite após a leitura.
