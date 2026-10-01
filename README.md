# IPTVEC V7

Correções visuais e de reprodução:
- Player nativo fica oculto até o usuário escolher um conteúdo, evitando a grande área preta inicial.
- Ao selecionar um canal, o player aparece e usa o logo do M3U como artwork enquanto o stream é preparado.
- Cards dos canais mostram logo maior (52x38), nome e qualidade (4K/FHD/HD/SD).
- Logos são carregados por URL e também por carregamento nativo Android como fallback.
- Mantém SQLite, paginação de 50 itens e leitura da lista M3U grande.

Exemplo verificado na lista enviada: SPORTV 2 FHD possui `tvg-logo` e pertence ao grupo `CANAIS | SPORTV`.
