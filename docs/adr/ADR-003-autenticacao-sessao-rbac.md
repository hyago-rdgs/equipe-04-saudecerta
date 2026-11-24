# ADR-003 — Autenticação por sessão no servidor e RBAC no caso de uso

| Campo | Valor |
|---|---|
| Status | **Aceito** |
| Data | 10/11/2026 |
| Decisores | Equipe SaúdeCerta |
| Drivers | DA1 Segurança e privacidade · DA6 Usabilidade da recepção · DA5 Disponibilidade |
| Requisitos | RF11, RF16, RNF01, RNF06, RNF07, RNF08, RN06 |

## Contexto

Quatro perfis (RECEPCAO, PROFISSIONAL, PACIENTE, ADMIN) acessam a mesma API a partir de uma SPA. É preciso decidir **como autenticar** (manter a identidade entre requisições) e **onde autorizar** (aplicar o RBAC e a regra “paciente só vê o próprio”).

Três requisitos pesam:

- **RF16**: um usuário bloqueado não deve mais acessar, de imediato. Se um profissional é desligado, o acesso ao prontuário precisa cessar já.
- **RNF06**: a recepção não pode ser interrompida por reautenticações frequentes.
- **E1, 7.1**: escalar horizontalmente não é driver (≤ 40 usuários simultâneos).

Na E1 ficou proposto que o administrador **não** leia conteúdo clínico.

## Alternativas — autenticação

| # | Alternativa | Revogação imediata (RF16) | Complexidade | Observação |
|---|---|---|---|---|
| A | **JWT *stateless*** (token no header, validado por assinatura) | **Não**: o token vale até expirar. Revogar exige lista de bloqueio, que reintroduz estado | Média (refresh token, rotação, armazenamento seguro no navegador) | Brilha com muitos serviços e escala horizontal, que não é o nosso caso |
| B | **Sessão no servidor** (Spring Session JDBC; cookie `HttpOnly; Secure; SameSite=Strict`) | **Sim**: apagar as sessões do usuário | Baixa (suporte nativo do Spring Security); exige proteção CSRF | Sessão no banco permite 2 instâncias sem *sticky session* |
| C | **Provedor de identidade externo** (Keycloak, OAuth2/OIDC) | Sim (sessão no IdP) | Alta: novo contêiner, configuração de *realm* e clientes | Valeria com SSO entre vários sistemas, que a clínica não tem |

## Alternativas — onde autorizar

| # | Alternativa | Prós | Contras |
|---|---|---|---|
| 1 | Só por URL (`requestMatchers("/api/prontuario/**").hasRole(...)`) | Centralizado | Não expressa “só o próprio paciente”; quebra se a rota mudar |
| 2 | **No caso de uso** (`@PreAuthorize` nos métodos da camada de aplicação), com regras por recurso | Fica junto da regra, vale para qualquer adaptador de entrada (REST hoje, mensageria amanhã) | Exige teste de cada método protegido |
| 3 | Dentro do domínio | — | Mistura segurança com regra de negócio; o domínio passaria a conhecer o usuário logado |

## Análise custo/benefício

| Critério | Peso | A (JWT) | **B (sessão)** | C (IdP) |
|---|---|---|---|---|
| Revogação imediata (RF16, DA1) | 5 | 2 | **5** | 5 |
| Superfície de ataque no navegador | 4 | 2 (token acessível a JS se mal armazenado) | **5** (cookie `HttpOnly`) | 4 |
| Custo de implementação e operação | 4 | 3 | **5** | 1 |
| Escala horizontal | 1 | 5 | **4** (sessão no banco) | 4 |
| Usabilidade (sessão longa sem re-login) | 3 | 4 | **4** | 4 |
| **Total (máx. 85)** | | 47 | **81** | 61 |

O custo de B é proteger contra CSRF, que é o reverso do uso de cookies. O Spring Security resolve com `CookieCsrfTokenRepository`: a SPA lê o token de um cookie legível e o devolve num header.

## Decisão

**Autenticação: B.** Login por `POST /api/auth/login` (formulário JSON); sessão gravada pelo Spring Session JDBC; cookie `SESSION` com `HttpOnly`, `Secure` e `SameSite=Strict`; expiração por inatividade de **30 min** e duração máxima de **8 h** (um turno). Senhas em BCrypt com custo 12 (RNF07). Após 5 tentativas erradas o login fica bloqueado por 15 min. `UsuarioService.bloquear()` apaga todas as sessões do usuário (`FindByIndexNameSessionRepository`).

**Autorização: 2**, em duas camadas:

1. **Grossa, por URL:** tudo em `/api/**` exige autenticação, exceto o login.
2. **Fina, por caso de uso:** `@PreAuthorize` com papéis e regras de titularidade:

```java
@PreAuthorize("hasRole('PROFISSIONAL') or @titular.ehDono(#pacienteId)")
HistoricoDto consultarHistorico(Long pacienteId, Pageable pagina);

@PreAuthorize("hasAnyRole('RECEPCAO','ADMIN') or @titular.ehDono(#cmd.pacienteId())")
ConsultaDto marcar(MarcarConsultaCommand cmd);
```

**Menor privilégio do administrador:** o ADMIN gerencia usuários, especialidades, relatórios e a trilha, mas **não** tem acesso ao conteúdo clínico. A trilha mostra quem acessou qual paciente, sem o conteúdo do atendimento. Se a clínica precisar de acesso emergencial (*break-glass*), ele será um caso de uso próprio, com justificativa obrigatória e auditoria reforçada, a ser decidido em ADR futuro.

Matriz de permissões — a mesma da E1, seção 3.4, agora como especificação de teste:

| Caso de uso | RECEPCAO | PROFISSIONAL | PACIENTE | ADMIN |
|---|---|---|---|---|
| Cadastrar/buscar paciente | ✔ | ler | só o próprio | ✔ |
| Marcar/remarcar/cancelar | ✔ | — | só o próprio | ✔ |
| Ler prontuário | ✘ | ✔ | só o próprio | ✘ |
| Registrar atendimento, prescrição, exame | ✘ | ✔ | ✘ | ✘ |
| Gerenciar usuários, ver auditoria | ✘ | ✘ | ✘ | ✔ |

## Consequências

- (+) Bloqueio tem efeito imediato.
- (+) Nenhum token acessível a JavaScript: um XSS não consegue exfiltrar a credencial.
- (+) Sessão longa e inatividade de 30 min atendem a recepção (RNF06).
- (+) Autorização no caso de uso continua valendo quando a Agenda virar serviço.
- (−) Uma consulta ao banco por requisição para carregar a sessão. Custo baixo na nossa carga; pode ganhar cache se preciso.
- (−) No PF, o serviço extraído precisará validar a sessão. Opções: o gateway propaga a identidade em header assinado, ou adotamos OAuth2 nessa fronteira. A escolha vai para o ADR de extração.

## Verificação

- QS-01, QS-02: teste parametrizado que percorre a matriz acima (perfil × caso de uso → status esperado).
- QS-07: teste do prefixo BCrypt; teste de bloqueio após 5 tentativas.
- Teste: bloquear usuário → a próxima requisição com o cookie antigo recebe 401.
