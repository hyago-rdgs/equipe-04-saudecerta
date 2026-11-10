# 3. Cenários de qualidade

Cada cenário segue o formato de seis partes de Bass, Clements e Kazman: **fonte → estímulo → artefato → ambiente → resposta → medida da resposta**. Para cada um indicamos a tática arquitetural, a decisão (ADR) que a materializa e **como verificar**, porque um cenário sem verificação é só uma intenção.

## 3.1 Árvore de utilidade

Prioridade no formato (importância para o negócio, dificuldade técnica), com A = alta, M = média e B = baixa:

```
Utilidade do SaúdeCerta
├── Segurança/Privacidade
│   ├── (A,A) QS-01  Usuário sem perfil clínico tenta abrir prontuário → negado e registrado
│   ├── (A,M) QS-02  Paciente tenta ler prontuário de outro paciente → negado
│   └── (A,B) QS-07  Vazamento da base não expõe senhas
├── Auditabilidade
│   ├── (A,A) QS-03  Todo acesso ao prontuário aparece na trilha em ≤ 1 min, mesmo com falha
│   └── (A,M) QS-04  Adulteração da trilha é detectável
├── Integridade da agenda
│   └── (A,M) QS-05  Duas recepcionistas marcam o mesmo horário ao mesmo tempo → só uma vence
├── Desempenho
│   ├── (M,M) QS-06  Busca de paciente por nome < 2 s (p95)
│   └── (M,M) QS-08  Abrir prontuário com histórico longo < 2 s (p95)
├── Disponibilidade
│   └── (M,B) QS-09  Falha da instância durante o expediente → agenda volta em < 2 min
├── Modificabilidade
│   ├── (A,M) QS-10  Incluir módulo de faturamento sem alterar agenda/prontuário
│   └── (A,M) QS-11  Extrair o módulo Agenda como serviço (PF)
├── Usabilidade
│   └── (M,B) QS-12  Marcar consulta simples em ≤ 1 min e ≤ 5 cliques
└── Testabilidade
    └── (M,B) QS-13  Regras de negócio testadas sem banco nem servidor
```

## 3.2 Cenários detalhados

### QS-01 — Acesso indevido ao prontuário · RNF01 · (A,A)

| Parte | Descrição |
|---|---|
| Fonte | Usuário autenticado com perfil RECEPCAO (ou ADMIN) |
| Estímulo | `GET /api/pacientes/1024/prontuario` |
| Artefato | `ProntuarioService.consultarHistorico` |
| Ambiente | Operação normal |
| Resposta | 403 sem nenhum dado clínico no corpo; evento `TentativaAcessoNegada` gravado na trilha |
| **Medida** | **100% das tentativas bloqueadas e 100% registradas** |
| Táticas | Autorizar atores (RBAC no caso de uso); limitar acesso (menor privilégio do admin); detectar (registrar negação) |
| Decisão | [ADR-003](adr/ADR-003-autenticacao-sessao-rbac.md), [ADR-002](adr/ADR-002-auditoria-assincrona-eventos.md) |
| Verificação | Teste parametrizado `@WithMockUser` para os 4 perfis × todos os endpoints clínicos (matriz da E1, 3.4); verifica o status e a existência do `RegistroAuditoria` com `acao = NEGADO` |

### QS-02 — Paciente acessa dado de outro paciente · RN06 · (A,M)

| Parte | Descrição |
|---|---|
| Fonte | Paciente autenticado (`pacienteId = 10`) |
| Estímulo | `GET /api/pacientes/11/consultas` ou `/prontuario` |
| Artefato | Verificação de titularidade no caso de uso |
| Ambiente | Operação normal |
| Resposta | 403 (não 404, para não confirmar existência por diferença de comportamento — ver nota); negação registrada |
| **Medida** | **0 respostas com dados de outro titular em todos os endpoints de paciente** |
| Táticas | Autorizar com base no recurso (`@PreAuthorize("@titular.ehDono(#pacienteId)")`) |
| Decisão | ADR-003 |
| Verificação | Teste de integração: usuário paciente A tenta acessar todos os endpoints com o ID de B |

> Nota: retornar 403 para qualquer ID que não seja do próprio paciente, existente ou não, faz o comportamento ser idêntico nos dois casos, e não vaza a existência de registros.

### QS-03 — Auditoria completa e no prazo, mesmo com falha · RNF02 · (A,A)

| Parte | Descrição |
|---|---|
| Fonte | Profissional |
| Estímulo | Abre o prontuário de um paciente; **a aplicação cai** logo depois do commit da leitura |
| Artefato | `ProntuarioService`, *Event Publication Registry*, `AuditoriaListener` |
| Ambiente | Operação normal, com falha da instância |
| Resposta | Evento persistido na mesma transação; ao reiniciar, publicações incompletas são reprocessadas e o registro aparece na trilha |
| **Medida** | **Registro consultável em ≤ 1 min em operação normal (p99); 0 acessos sem registro após falha e reinício** |
| Táticas | Outbox transacional; reprocessamento na inicialização; consumidor idempotente (chave = ID do evento) |
| Decisão | [ADR-002](adr/ADR-002-auditoria-assincrona-eventos.md) |
| Verificação | Teste com `@ApplicationModuleTest` + `Scenario.publish(...).andWaitForStateChange(...)`; teste de falha: listener lança exceção, verifica que a publicação segue “incompleta” e é concluída no *resubmit* |

### QS-04 — Adulteração da trilha é detectável · RNF02 · (A,M)

| Parte | Descrição |
|---|---|
| Fonte | Usuário com acesso direto ao banco (DBA, invasor) |
| Estímulo | Altera ou remove um `RegistroAuditoria` |
| Artefato | Tabela `auditoria.registro_auditoria` |
| Ambiente | Pós-incidente |
| Resposta | A verificação da cadeia aponta o primeiro registro inconsistente |
| **Medida** | **100% das alterações detectadas pela rotina de verificação** |
| Táticas | Hash encadeado (SHA-256 do hash anterior + conteúdo); usuário da aplicação sem `UPDATE`/`DELETE` |
| Decisão | ADR-002 |
| Verificação | Teste: gravar 100 registros, alterar o 37º via SQL, `CadeiaDeHash.verificar()` retorna a posição 37 |

### QS-05 — Marcação concorrente do mesmo horário · RN02 / RF06 · (A,M)

| Parte | Descrição |
|---|---|
| Fonte | Duas recepcionistas |
| Estímulo | Marcam o mesmo profissional, mesma data/hora, com diferença de milissegundos |
| Artefato | `AgendamentoService.marcar`, tabela `agenda.consulta` |
| Ambiente | Pico de uso |
| Resposta | Uma recebe 201; a outra recebe 409 com horários sugeridos |
| **Medida** | **0 pares de consultas AGENDADAS no mesmo slot em 1.000 execuções concorrentes** |
| Táticas | Validação no domínio (caminho feliz) + restrição única no banco (garantia final) |
| Decisão | [ADR-004](adr/ADR-004-concorrencia-marcacao.md) |
| Verificação | Teste de integração com `ExecutorService` disparando 20 marcações simultâneas no mesmo slot contra PostgreSQL (Testcontainers); espera exatamente 1 sucesso |

### QS-06 — Busca de paciente por nome · RNF03 · (M,M)

| Parte | Descrição |
|---|---|
| Fonte | Recepcionista |
| Estímulo | `GET /api/pacientes?nome=maria&size=20` |
| Artefato | `PacienteRepository` |
| Ambiente | Base com 50 mil pacientes; 30–40 usuários simultâneos |
| Resposta | Primeira página com 20 resultados |
| **Medida** | **< 2 s no p95 (meta interna: < 300 ms no servidor)** |
| Táticas | Índice `pg_trgm` em `nome` (busca por trecho); paginação obrigatória (`size` ≤ 50); projeção só com colunas da listagem; sem N+1 |
| Decisão | [ADR-006](adr/ADR-006-estrategia-persistencia.md) |
| Verificação | Carga com Gatling/k6 sobre base sintética de 50 mil registros; `EXPLAIN ANALYZE` mostrando uso do índice |

### QS-07 — Credenciais em caso de vazamento · RNF07 · (A,B)

| Parte | Descrição |
|---|---|
| Fonte | Atacante com cópia da tabela `usuario` |
| Estímulo | Tenta recuperar senhas |
| Artefato | `UsuarioService`, coluna `senha_hash` |
| Ambiente | Pós-incidente |
| Resposta | Só há hashes BCrypt com sal individual |
| **Medida** | **0 senhas em texto; custo BCrypt ≥ 12** |
| Táticas | Hash com sal (`BCryptPasswordEncoder(12)`); nunca registrar senha em log |
| Decisão | ADR-003 |
| Verificação | Teste: após cadastrar, a coluna começa com `$2a$12$`; revisão de código com busca por `password` em logs |

### QS-08 — Abrir prontuário com histórico longo · RNF03 / DA4 · (M,M)

| Parte | Descrição |
|---|---|
| Fonte | Profissional |
| Estímulo | Abre o histórico de um paciente com 200 atendimentos |
| Artefato | `ProntuarioService.consultarHistorico` |
| Ambiente | Operação normal |
| Resposta | Linha do tempo paginada (20 mais recentes), com prescrições e exames de cada atendimento |
| **Medida** | **< 2 s no p95; auditoria acrescenta < 20 ms ao tempo de resposta** |
| Táticas | Auditoria fora do caminho crítico (assíncrona); `Atendimento` como agregado próprio; consulta com `JOIN FETCH` ou *batch fetching* para evitar N+1 |
| Decisão | ADR-002, ADR-006 |
| Verificação | Medição com e sem o listener de auditoria; log de SQL no teste mostra número fixo de consultas, independente do tamanho do histórico |

### QS-09 — Falha da instância no expediente · RNF04 · (M,B)

| Parte | Descrição |
|---|---|
| Fonte | Falha interna (estouro de memória, erro na JVM) |
| Estímulo | Processo da API termina |
| Artefato | Contêiner da API |
| Ambiente | Horário de funcionamento |
| Resposta | Healthcheck detecta; Docker reinicia; sessões continuam válidas porque estão no banco |
| **Medida** | **Indisponibilidade < 2 min por incidente; ≥ 99% mensal em 12 h/dia (≈ 3,6 h de tolerância)** |
| Táticas | Detectar (healthcheck Actuator); recuperar (restart automático); estado externo (sessão no banco); opcional: redundância ativa com 2 instâncias |
| Decisão | ADR-001, ADR-003 |
| Verificação | `docker kill` da API durante teste manual; cronometrar a volta; usuário logado não precisa entrar de novo |

### QS-10 — Incluir módulo de faturamento · RNF05 · (A,M)

| Parte | Descrição |
|---|---|
| Fonte | Equipe de desenvolvimento |
| Estímulo | Novo requisito: faturar consultas realizadas |
| Artefato | Código-fonte |
| Ambiente | Tempo de desenvolvimento |
| Resposta | Novo pacote `faturamento` que escuta `AtendimentoRegistrado` e usa `AgendaApi` |
| **Medida** | **0 arquivos alterados em `agenda` e `prontuario`; ≤ 5 dias/equipe** |
| Táticas | Encapsular (portas públicas); usar intermediário (eventos de domínio); restringir dependências (Modulith) |
| Decisão | ADR-001, ADR-002 |
| Verificação | Exercício na E3: criar um módulo `notificacao` de exemplo e conferir `git diff --stat` nos pacotes núcleo |

### QS-11 — Extrair o módulo Agenda como serviço · RT05 · (A,M)

| Parte | Descrição |
|---|---|
| Fonte | Equipe (Projeto Final) |
| Estímulo | Mover Agenda para um processo separado com API REST |
| Artefato | Módulo `agenda`, `CadastroApiAdapter`, consumidores de `AgendaApi` |
| Ambiente | Tempo de desenvolvimento |
| Resposta | Domínio e aplicação da Agenda copiados sem mudança; só os adaptadores trocam (JPA local → próprio banco; `CadastroApiAdapter` → cliente HTTP; `AgendaApi` no monólito → cliente HTTP) |
| **Medida** | **0 linhas alteradas em `agenda.domain`; mudanças restritas a `infra` e configuração** |
| Táticas | Portas e adaptadores; referência por ID; schema próprio do módulo |
| Decisão | ADR-001, ADR-006 |
| Verificação | No PF: `git diff --stat` do pacote `agenda.domain` entre a E3 e o PF |

### QS-12 — Marcação rápida na recepção · RNF06 · (M,B)

| Parte | Descrição |
|---|---|
| Fonte | Recepcionista (persona Márcia) |
| Estímulo | Paciente cadastrado pede consulta com clínico geral |
| Artefato | SPA + `GET horarios-livres` + `POST consultas` |
| Ambiente | Horário de pico, ao telefone |
| Resposta | Busca do paciente → especialidade → horário → confirmar; conflito avisado com sugestões |
| **Medida** | **≤ 1 min e ≤ 5 cliques a partir da tela inicial** |
| Táticas | Endpoint específico de horários livres (evita a UI calcular); 409 com `horariosSugeridos`; sessão de 8 h com expiração por inatividade de 30 min, sem reautenticação a cada ação |
| Decisão | ADR-003, ADR-004 |
| Verificação | Teste de usabilidade cronometrado com 3 pessoas fora da equipe (E3/PF) |

### QS-13 — Testar regras sem infraestrutura · RNF09 · (M,B)

| Parte | Descrição |
|---|---|
| Fonte | Desenvolvedor |
| Estímulo | Altera uma regra de negócio |
| Artefato | Pacotes `domain` |
| Ambiente | Build local e CI |
| Resposta | Testes de domínio rodam com JUnit puro, sem subir Spring nem banco |
| **Medida** | **100% das RN com teste; suíte de domínio < 10 s** |
| Táticas | Domínio isolado; dependências por porta; `Clock` injetável |
| Decisão | ADR-001 |
| Verificação | Relatório do JaCoCo restrito a `..domain..`; tempo do `mvn test -Dgroups=dominio` |

## 3.3 Pontos de sensibilidade e de tradeoff

| Ponto | Tipo | Atributos em tensão | Como foi resolvido |
|---|---|---|---|
| Momento da gravação da auditoria | Tradeoff | Auditabilidade × desempenho | Gravação mínima (outbox) síncrona, processamento assíncrono — ADR-002 |
| Mecanismo de sessão | Tradeoff | Segurança (revogação) × escalabilidade | Sessão no banco: revogação imediata; escala horizontal não é driver — ADR-003 |
| Duração da sessão | Sensibilidade | Usabilidade × segurança | 30 min de inatividade, 8 h no máximo; recepção não reautentica a cada ação |
| Validação de conflito | Tradeoff | Integridade × usabilidade × simplicidade | Domínio para mensagem clara + banco para garantia — ADR-004 |
| Granularidade de implantação | Tradeoff | Modificabilidade × simplicidade operacional | Um deploy com módulos fiscalizados — ADR-001 |
| Separação domínio/JPA | Tradeoff | Modificabilidade × custo de desenvolvimento | Separado só onde há regra rica — ADR-006 |
