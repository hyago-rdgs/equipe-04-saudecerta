# Raio-X das camadas

O Raio-X mostra, para cada requisição, por quais componentes ela passou e quanto tempo ficou em cada um. Ele aparece na barra escura no rodapé da interface.

## Como funciona

1. `RaioXFilter` intercepta toda chamada a `/api/...`, cria um rastro e devolve o identificador no cabeçalho `X-Raio-X`.
2. `RaioXAspect` (AOP) envolve os métodos dos componentes Spring e anota cada chamada no rastro. A camada é deduzida do nome do pacote: `web`, `aplicacao` ou `persistencia`.
3. A interface lê o cabeçalho e busca o rastro em `GET /api/raio-x/{id}`.

Os rastros ficam só em memória (os 60 mais recentes).

## O que observar

- Uma leitura simples (`GET /api/pacientes`) desce Apresentação → Aplicação → Persistência.
- Uma operação de negócio (`POST /api/consultas`) visita vários módulos: agenda, cadastro, auditoria.
- Uma requisição recusada por permissão (403) não mostra nenhum passo: ela foi barrada pelos filtros de segurança antes de chegar ao controller.
- O domínio (`Consulta`, `Atendimento`) nunca aparece. Por quê?

## Para discutir

- O Raio-X não exigiu alterar nenhuma classe de negócio. Que princípio permite isso?
- Ele tem custo: mede e guarda dados a cada chamada. Desligue com `saudecerta.raio-x.ativo=false` e compare os tempos. Você deixaria ligado em produção?
- Qualquer usuário autenticado lê os rastros de todos. Que informação isso expõe? Como você restringiria?
