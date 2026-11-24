# ADR 0005 — Autenticação sem estado com token JWT

- **Situação:** aceita
- **Data:** 2026-11-10

## Contexto

A interface é uma página que consome a API REST. A ementa prevê evolução para serviços, que precisarão reconhecer o mesmo usuário.

## Opções consideradas

1. Sessão no servidor com cookie.
2. HTTP Basic a cada requisição.
3. Token JWT assinado, enviado no cabeçalho `Authorization`.

## Decisão

Opção 3, com assinatura simétrica HS256. O token carrega login, nome, perfil e o vínculo com profissional ou paciente, e vale 8 horas. Senhas são guardadas com BCrypt.

## Custo e benefício

| | Sessão | Basic | JWT |
|---|---|---|---|
| Benefício principal | revogação imediata | trivial | sem estado; outros serviços validam sozinhos |
| Custo | estado no servidor; CSRF | senha trafega sempre | não dá para revogar antes de expirar |
| Risco | escala horizontal exige sessão compartilhada | exposição da senha | token roubado vale até expirar |

## Consequências

- Sem segredo configurado, a chave é aleatória a cada inicialização: reiniciar derruba as sessões. Para fixar, defina `saudecerta.jwt.segredo` (32+ caracteres) fora do repositório.
- A interface guarda o token em `sessionStorage`: some ao fechar a aba, mas fica ao alcance de scripts da página; por isso toda saída de dados na tela é escapada (`util.js`).
- Com serviços separados (PF-1), chave simétrica obriga a compartilhar o segredo; par de chaves (RS256) passa a ser melhor.
