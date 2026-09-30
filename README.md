# EC IPTV Android

Projeto Android baseado no layout HTML enviado.

## Estrutura
- Início
- Ao vivo
- Jogos
- Perfil
- Player HLS/M3U8
- Carregamento de lista M3U por URL
- Navegação inferior

## Gerar APK
No GitHub Actions, execute o workflow **Gerar APK**.
Também é possível executar `gradle :app:assembleDebug` em um ambiente com Android/Gradle configurado.

Observação: o HTML original usa Tailwind, Lucide, HLS.js e imagens externas por CDN/URL. O aplicativo precisa de internet para esses recursos.
