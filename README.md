# IPTVEC V4 — correção da lista e player

Correção principal: o JavaScript da paginação tinha uma variável `q` que escondia a função `q()`, causando erro ao renderizar os 50 itens e deixando “Carregando 50 itens...” permanentemente.

Também foi corrigida a leitura dos atributos M3U e mantido o player Media3/ExoPlayer nativo.
