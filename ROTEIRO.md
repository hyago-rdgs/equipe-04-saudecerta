# Roteiro do SaúdeCerta

Este roteiro tem três partes. Na **Parte A** você coloca a aplicação para rodar no seu computador. Na **Parte B** você usa o sistema como cada tipo de usuário e confere, um a um, os requisitos funcionais. Na **Parte C** você olha por dentro: camadas, API, testes e regras de arquitetura.

Faça na ordem. Cada etapa da Parte B tem três blocos: **Faça** (os passos), **Observe** (o que deve acontecer) e **Responda** (perguntas para anotar e levar para a aula).

Tempo estimado: 30 minutos para a Parte A, 60 para a Parte B e 45 para a Parte C.

---

## Parte A — Deixar a aplicação funcionando

### A1. O que precisa estar instalado

| Ferramenta | Versão | Como conferir no terminal |
|---|---|---|
| JDK | 17 ou mais nova (recomendado: 21) | `java -version` |
| Maven | 3.9 ou mais nova | `mvn -version` |
| Git | qualquer versão recente | `git --version` |
| Navegador | Chrome, Edge ou Firefox atual | — |
| IDE (opcional) | IntelliJ IDEA Community, VS Code com "Extension Pack for Java" ou Eclipse | — |

Se `java -version` mostrar uma versão menor que 17, instale um JDK novo (por exemplo, o Eclipse Temurin 21) antes de continuar. Se você usa o IntelliJ, não precisa instalar o Maven separadamente: a IDE já traz um.

### A2. Preparar a pasta

1. Descompacte `saudecerta.zip` em uma pasta com caminho curto e **sem espaços ou acentos** (por exemplo `C:\projetos\saudecerta` ou `~/projetos/saudecerta`).
2. Abra um terminal dentro dessa pasta. O arquivo `pom.xml` deve aparecer ao listar o conteúdo (`dir` no Windows, `ls` no Linux/macOS).

### A3. Rodar

Pelo terminal:

```
mvn spring-boot:run
```

Na primeira vez o Maven baixa as dependências. Isso exige internet e pode levar alguns minutos. A aplicação está pronta quando aparecer uma linha parecida com:

```
Started SaudeCertaApplication in 4.2 seconds
```

Pela IDE: abra a pasta como projeto Maven, espere a importação terminar e execute a classe `SaudeCertaApplication` (botão de "play" ao lado do método `main`).

### A4. Conferir

1. Abra <http://localhost:8080>. Deve aparecer a tela de entrada do SaúdeCerta.
2. Abra <http://localhost:8080/swagger-ui.html>. Deve aparecer a documentação da API.
3. Em outro terminal, na pasta do projeto, rode `mvn test`. O resultado esperado é `BUILD SUCCESS`, com alguns testes marcados como ignorados (são os da missão E3-2).

Para parar a aplicação, volte ao terminal onde ela roda e pressione `Ctrl+C`.

> O banco de dados fica **na memória**. Toda vez que você reinicia a aplicação, os dados voltam ao estado inicial e é preciso entrar de novo. Isso é proposital: você pode experimentar sem medo de estragar nada.

### A5. Colocar no Git da equipe

```
git init
git add .
git commit -m "SaudeCerta: versão inicial recebida do professor"
```

Crie o repositório da equipe no GitHub e envie esse primeiro commit antes de alterar qualquer coisa. Assim o histórico mostra exatamente o que a equipe construiu.

### A6. Se algo der errado

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `mvn` não é reconhecido como comando | Maven não instalado ou fora do PATH | Instale o Maven ou rode pela IDE |
| `release version 17 not supported` ou erro de versão de classe | JDK mais antigo que 17 | Instale um JDK 17+ e confira `java -version` e `mvn -version` (o Maven mostra qual Java está usando) |
| `Port 8080 was already in use` | Outro programa usa a porta | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` e acesse a porta 8081 |
| Erros de download (`Could not transfer artifact`, `Connection timed out`) | Sem internet ou rede com proxy | Tente outra rede; no laboratório, peça ao professor a configuração de proxy do Maven |
| A tela pede login de novo depois de reiniciar a aplicação | A chave dos tokens muda a cada inicialização | Entre novamente. O motivo está em `docs/adr/0005-autenticacao-jwt.md` |
| A página abre mas fica em branco | Navegador muito antigo ou erro de JavaScript | Abra as ferramentas do desenvolvedor (F12), aba Console, e leia a mensagem |

Se o erro for outro, copie a mensagem **completa** do terminal antes de pedir ajuda. A linha que importa costuma ser a primeira que começa com `Caused by` ou `ERROR`.

---

## Parte B — O que a aplicação faz

### Usuários de demonstração

A senha de todos é `saude123`. Na tela de entrada, clicar no nome do usuário já entra direto.

| Usuário | Perfil | Usado nas etapas |
|---|---|---|
| `recepcao` | Recepção | 1, 2, 3 |
| `ana` | Profissional de saúde (Dra. Ana Beatriz, Clínica Geral) | 4 |
| `rafael` | Profissional de saúde (Dr. Rafael, Cardiologia) | 4 |
| `admin` | Administração | 5, 6 |
| `joana` | Paciente | 7 |

### Requisitos funcionais

Marque cada requisito quando você o tiver visto funcionar.

| ID | Requisito | Etapa | Situação |
|---|---|---|---|
| RF01 | Entrar no sistema com usuário e senha | 0 | pronto |
| RF02 | Cada perfil enxerga e executa apenas o que lhe cabe | 0, 5 | pronto |
| RF03 | Buscar, cadastrar, alterar e inativar pacientes; CPF válido e único | 2 | pronto |
| RF04 | Manter profissionais e especialidades | 6 | pronto |
| RF05 | Ver a agenda de um dia, por profissional | 3 | pronto |
| RF06 | Agendar consulta respeitando horário da clínica e sem conflito | 3 | pronto |
| RF07 | Confirmar a chegada do paciente | 1 | pronto |
| RF08 | Cancelar consulta informando o motivo | 3 | pronto |
| RF09 | Ver o resumo do dia (sala de espera, a chegar, encerradas) | 1, 4 | pronto |
| RF10 | Iniciar o atendimento: só o profissional da consulta, depois da chegada | 4 | pronto |
| RF11 | Registrar queixa, evolução e conduta; salvar rascunho | 4 | pronto |
| RF12 | Finalizar o atendimento, que não pode mais ser alterado | 4 | pronto |
| RF13 | Consultar o histórico clínico (prontuário) do paciente | 4 | pronto |
| RF14 | Consultar a trilha de auditoria: quem fez o quê e quando | 5 | pronto |
| RF15 | Remarcar consulta | 3 | **missão E3-1** |
| RF16 | Prescrever medicamentos no atendimento | 4 | **missão E3-2** |
| RF17 | Relatórios de consultas por período | 5 | **missão E3-3** |
| RF18 | Portal do paciente: ver e cancelar as próprias consultas | 7 | **missão E3-4** |

Antes de começar, abra a barra escura **Raio-X** no rodapé (clique em "Abrir") e deixe-a aberta. Ela mostra o caminho de cada requisição pelas camadas do servidor. Você vai usá-la em todas as etapas.

---

### Etapa 0 — Entrar (RF01, RF02)

**Faça**

1. Na tela de entrada, digite o usuário `recepcao` com uma senha errada.
2. Entre com a senha certa. Anote os itens do menu.
3. Clique em "Sair" e entre como `ana`. Anote o menu. Repita com `admin`.

**Observe**

- A senha errada mostra "Usuário ou senha incorretos", sem dizer qual dos dois está errado.
- O menu muda conforme o perfil.

**Responda**

1. Por que a mensagem não diz se o usuário existe?
2. Esconder um item do menu protege a funcionalidade? Onde, no código, o acesso é realmente negado? (Dica: `SecurityConfig`.)

---

### Etapa 1 — O dia da clínica (RF07, RF09) — usuário `recepcao`

**Faça**

1. Abra "Painel do dia". Leia os indicadores e os quadros "Sala de espera", "A chegar" e "Encerradas".
2. Em "A chegar", clique em "Confirmar chegada" de um paciente.

**Observe**

- O paciente sai de "A chegar" e entra em "Sala de espera" com a etiqueta "Aguardando". Os indicadores mudam.
- No Raio-X aparecem duas requisições: o `POST .../chegada` e o novo `GET /api/painel`.

**Responda**

1. Clique no `POST .../chegada` dentro do Raio-X. Quais componentes foram chamados e em que camada está cada um?
2. A situação "Aguardando" na tela corresponde a qual valor de `StatusConsulta` no código?

---

### Etapa 2 — Pacientes (RF03) — usuário `recepcao`

**Faça**

1. Abra "Pacientes". Busque por `maria`. Depois busque por `158` (parte de um CPF).
2. Clique em "Cadastrar paciente" e clique em cadastrar **sem preencher nada**.
3. Preencha nome e data de nascimento e use o CPF `111.111.111-11`.
4. Troque o CPF para `158.813.998-03` (que já pertence a outra pessoa).
5. Troque o CPF para `766.344.002-10` e conclua o cadastro.
6. Edite o paciente que você criou e escreva algo em "Alertas para a equipe". Salve.
7. Edite de novo e clique em "Inativar cadastro".

**Observe**

- Passo 2: cada campo obrigatório ganha sua própria mensagem.
- Passos 3 e 4: o cadastro é recusado, com mensagens diferentes.
- Passo 7: o paciente some da lista, mas não é apagado do banco.

**Responda**

1. Olhe os três erros no Raio-X. Qual código HTTP voltou em cada um (campos vazios, CPF inválido, CPF duplicado)? Por que são códigos diferentes?
2. No caso dos campos vazios, o Raio-X mostra que a requisição chegou até qual camada? E no CPF duplicado? O que isso diz sobre onde cada validação acontece?
3. Por que o sistema inativa em vez de apagar um paciente?

---

### Etapa 3 — Agenda (RF05, RF06, RF08, RF15) — usuário `recepcao`

As regras da clínica: consultas de 30 minutos, das 07:00 às 19:00, de segunda a sábado, e nunca em horário que já passou. **Se você está fazendo o roteiro à noite, os horários de hoje já passaram: use o botão "›" para ir ao próximo dia.**

**Faça**

1. Abra "Agenda". Identifique as cores de cada situação pela legenda abaixo da grade.
2. Vá para amanhã. Passe o mouse sobre um horário vazio e clique em "Agendar". Busque um paciente, escolha-o e conclua.
3. Ainda em amanhã: a paciente Larissa Couto Maués já tem consulta às 09:00 com a Dra. Ana. Tente agendar a **mesma Larissa**, às **09:00**, com o **Dr. Caio**.
4. Avance até o próximo domingo e tente agendar qualquer consulta.
5. Clique em uma consulta com situação "Agendada", depois em "Cancelar…", e tente cancelar sem motivo. Depois informe um motivo.
6. Clique em outra consulta "Agendada" e depois em "Remarcar…".

**Observe**

- Passo 3: o agendamento é recusado porque a paciente não pode estar em dois lugares. (Se amanhã for domingo, a mensagem será a do domingo; faça o teste em outro dia agendando duas vezes a mesma pessoa.)
- Passo 5: a consulta cancelada fica riscada e o horário volta a aceitar agendamento.
- Passo 6: a API responde que a remarcação é uma missão pendente.

**Responda**

1. A tela nem oferece o botão "Agendar" em um horário ocupado. Então por que o servidor também verifica o conflito? (Tente responder antes de olhar `docs/api.http`.)
2. Abra a classe `Consulta`. Liste as regras de agendamento que estão nela. Há alguma anotação do Spring ou do JPA nessa classe? Por quê?
3. No Raio-X do `POST /api/consultas`, quais **módulos** diferentes aparecem (olhe o nome dos componentes)?
4. Qual código HTTP voltou em "Remarcar…"? O que ele significa?

---

### Etapa 4 — Atendimento e prontuário (RF10 a RF13, RF16) — usuário `ana`

**Faça**

1. Entre como `ana`. Em "Painel do dia", veja o quadro "Pacientes aguardando você".
2. Clique em "Atender" na paciente Joana Batista Ferreira. Leia o aviso de atenção ao lado do nome e o histórico à direita.
3. Preencha só a queixa principal e clique em "Salvar rascunho".
4. Clique em "Finalizar atendimento" **sem** preencher a evolução.
5. Preencha evolução e conduta e finalize.
6. Abra "Agenda" e localize a consulta que você acabou de atender.
7. Abra "Pacientes" e clique em "Prontuário" de Miguel Antônio Sarmento (paciente do Dr. Rafael).
8. Na barra de endereço, troque o final da URL para `#/atendimento/12` (uma consulta do Dr. Rafael) e pressione Enter.
9. Volte ao atendimento de outra paciente que esteja aguardando e role até "Prescrição".

**Observe**

- Passo 2: a consulta passa de "Aguardando" para "Em atendimento".
- Passo 4: o sistema recusa a finalização e explica o motivo.
- Passo 5: depois de finalizado, o atendimento vira parte do histórico e a consulta aparece como "Realizada".
- Passo 7: a Dra. Ana consegue **ler** o prontuário de um paciente de outro profissional, e a tela avisa que o acesso foi registrado.
- Passo 8: ela **não** consegue abrir o atendimento de uma consulta que não é dela.

**Responda**

1. Onde está a regra "não finaliza sem evolução"? E a regra "só o profissional responsável altera"? (Procure em `Atendimento`.)
2. Qualquer profissional lê qualquer prontuário. Isso é uma falha ou uma decisão? Leia `docs/adr/0004-controle-de-acesso-rbac.md` e diga se você concorda.
3. Tente, como `ana`, atender um paciente que ainda está "Agendado" (sem chegada confirmada), trocando o número na URL. O que acontece e qual classe decide isso?
4. O que precisaria existir, em cada camada, para a prescrição funcionar? Liste as classes que você criaria.

---

### Etapa 5 — Auditoria (RF02, RF14, RF17) — usuário `admin`

**Faça**

1. Saia e, na tela de entrada, tente entrar como `ana` com senha errada. Depois entre como `admin`.
2. Abra "Auditoria". Localize: a tentativa de acesso recusada; as leituras de prontuário que você fez como `ana`; o cadastro do paciente da etapa 2.
3. Use o filtro "Mostrar apenas o que envolve o paciente" e escolha Joana Batista Ferreira.
4. Abra <http://localhost:8080/swagger-ui.html>, faça login pelo endpoint `POST /api/auth/login` com o usuário `admin`, copie o `token`, clique em "Authorize" (se disponível) ou use o arquivo `docs/api.http`, e chame `GET /api/pacientes/1/prontuario`.
5. Abra "Relatórios".

**Observe**

- Passo 2: a trilha registra inclusive **leituras** e **tentativas recusadas**. As linhas de prontuário têm uma marca lateral.
- Passo 4: o administrador recebe `403`. Ele administra o sistema, mas não vê dado clínico.
- No Raio-X, a requisição recusada com 403 não mostra nenhum componente.

**Responda**

1. Por que a requisição com 403 não passou por nenhuma camada da aplicação? Quem respondeu?
2. A auditoria é gravada antes de a resposta voltar ao usuário (síncrona). Qual é o custo disso? Qual seria o risco de fazer depois (assíncrona)? Compare com `docs/adr/0003-auditoria-sincrona.md`.
3. A trilha mostra *que* a Dra. Ana leu o prontuário, mas não *por quê*. Isso é suficiente para a LGPD? O que você acrescentaria?

---

### Etapa 6 — Profissionais (RF04) — usuário `admin`

**Faça**

1. Abra "Profissionais". Crie a especialidade `Dermatologia`.
2. Cadastre um profissional nessa especialidade.
3. Abra "Agenda" e confira a nova coluna.
4. Volte a "Profissionais", edite o profissional e desmarque "Ativo". Confira a agenda de novo.
5. Tente cadastrar outro profissional com o mesmo número de registro.

**Observe**

- A agenda ganha e perde a coluna conforme o profissional está ativo.
- Registro duplicado é recusado.

**Responda**

1. O módulo `agenda` não acessa a tabela de profissionais. Como ele descobre os nomes e as especialidades? (Procure `CadastroFachada`.)
2. Compare o pacote `cadastro` com o pacote `agenda`. Que pastas existem em um e não no outro? Qual é a diferença de estilo arquitetural? (Confira em `docs/adr/0002-hexagonal-onde-ha-regra.md`.)

---

### Etapa 7 — Paciente (RF18) — usuário `joana`

**Faça**

1. Entre como `joana`.
2. Leia o cartão da missão.

**Responda**

1. O token de `joana` já carrega o `pacienteId`. Como garantir que ela veja **apenas** as próprias consultas, mesmo que troque números na URL?
2. Que dados do atendimento **não** devem aparecer no portal?

---

## Parte C — Por dentro

### C1. Ler o código na ordem certa

Siga uma única funcionalidade, de fora para dentro. Sugestão: **agendar consulta**.

1. `static/js/telas/agenda.js` — a tela chama `api.agendar(...)`.
2. `static/js/api.js` — a chamada vira `POST /api/consultas` com o token.
3. `agenda/adaptador/web/AgendaController` — recebe o HTTP e repassa.
4. `agenda/aplicacao/AgendaService.agendar` — orquestra: cadastro, domínio, repositório, auditoria.
5. `agenda/dominio/Consulta.agendar` e `conflitaCom` — as regras.
6. `agenda/aplicacao/ConsultaRepositorio` — a porta (interface).
7. `agenda/adaptador/persistencia/ConsultaRepositorioJpa` e `ConsultaJpa` — o adaptador.

Depois compare com o caminho de **cadastrar paciente** no módulo `cadastro`, que usa camadas simples.

**Responda:** em qual dos dois caminhos há mais classes? O que se ganhou com as classes a mais?

### C2. Usar a API sem a interface

Abra `docs/api.http` no IntelliJ ou no VS Code (extensão REST Client) e execute as requisições de cima para baixo. Ou use o Swagger em `/swagger-ui.html`.

**Responda:** para cada requisição do arquivo, anote o verbo, o código de status e o motivo de ser esse código. Há alguma rota cujo nome ou verbo você mudaria?

### C3. Rodar os testes

```
mvn test
```

Abra os três tipos de teste e compare:

| Teste | O que verifica | Sobe o Spring? |
|---|---|---|
| `ConsultaTest`, `AtendimentoTest`, `CpfTest` | regras de domínio | não |
| `ArquiteturaTest` | regras de dependência entre camadas e módulos | não |
| `FluxoDaClinicaTest` | a API de ponta a ponta | sim |

**Responda:** qual tipo demora mais? Por quê? Qual você rodaria a cada alteração e qual só antes de enviar o código?

### C4. Quebrar a arquitetura de propósito

1. Abra `agenda/dominio/Consulta.java`.
2. Acrescente `import org.springframework.stereotype.Component;` e anote a classe com `@Component`.
3. Rode `mvn test`.
4. Leia a mensagem de erro do `ArquiteturaTest` e **desfaça a alteração**.

Repita com outra violação: dentro de `PainelService`, tente usar `ConsultaRepositorio` (que é interno do módulo agenda) em vez da `AgendaFachada`.

**Responda:** que regra cada alteração violou? Por que essa regra existe? Em um projeto sem esse teste, quando o problema seria percebido?

### C5. Medir com o Raio-X

Leia `docs/raio-x.md`. Depois:

1. Compare o rastro de `GET /api/pacientes` com o de `GET /api/agenda`. Qual visita mais componentes? Por quê?
2. No rastro de `POST /api/consultas`, some os tempos da auditoria. Que fração do tempo total ela representa?
3. Pare a aplicação e suba de novo com o Raio-X desligado: `mvn spring-boot:run "-Dspring-boot.run.arguments=--saudecerta.raio-x.ativo=false"`. O que muda na interface?

### C6. Ler as decisões

Leia as seis ADRs em `docs/adr/`. Para cada uma, escreva em uma frase: qual era a alternativa e o que foi sacrificado ao escolher.

---

## O que entregar deste roteiro

1. A tabela de requisitos da Parte B com a marcação do que você viu funcionar.
2. As respostas das etapas 0 a 7 e dos itens C1 a C6.
3. Uma lista do que a equipe encontrou de estranho, incompleto ou discutível. Não é pegadinha: o sistema tem limitações reais, algumas descritas em `docs/rastreabilidade.md` e outras não.

Com isso feito, a equipe está pronta para escolher e planejar as missões em `MISSOES.md`.
