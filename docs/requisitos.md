# Requisitos — SaúdeCerta

## 1. Introdução

Este documento apresenta os requisitos iniciais do sistema SaúdeCerta, considerando o estudo de caso da Clínica SaúdeCerta e a abordagem apresentada no material da disciplina.

Requisitos funcionais descrevem **o que** o sistema deve fazer, enquanto requisitos não funcionais descrevem **como** o sistema deve se comportar, especialmente em relação aos seus atributos de qualidade.

Os requisitos estão organizados em requisitos funcionais (RF), requisitos não funcionais (RNF), regras de negócio (RN) e drivers arquiteturais (DA).

## 2. Requisitos Funcionais — RF

| ID | Requisito funcional |
| --- | --- |
| RF01 | Cadastrar e manter pacientes. |
| RF02 | Cadastrar profissionais e vinculá-los a especialidades. |
| RF03 | Definir a disponibilidade dos profissionais e seus horários de atendimento. |
| RF04 | Marcar consultas em horários disponíveis. |
| RF05 | Remarcar e cancelar consultas. |
| RF06 | Impedir conflitos de horário para um mesmo profissional. |
| RF07 | Registrar o atendimento no prontuário eletrônico. |
| RF08 | Emitir prescrições vinculadas ao atendimento. |
| RF09 | Solicitar exames vinculados ao atendimento. |
| RF10 | Consultar o histórico do paciente. |
| RF11 | Autenticar usuários e aplicar controle de acesso por perfil. |
| RF12 | Registrar auditoria de acesso ao prontuário. |
| RF13 | Gerar relatórios gerenciais básicos. |

Os requisitos acima correspondem às funcionalidades especificadas no estudo de caso, incluindo cadastro, agenda, atendimento, prontuário, prescrição, exames, segurança, auditoria e relatórios.

### 2.1 Detalhamento dos requisitos funcionais

#### RF01 — Cadastrar e manter pacientes

O sistema deve permitir cadastrar e manter os dados dos pacientes, incluindo informações necessárias à sua identificação e atendimento.

#### RF02 — Cadastrar profissionais e especialidades

O sistema deve permitir cadastrar profissionais e vinculá-los a uma ou mais especialidades.

#### RF03 — Definir disponibilidade

O sistema deve permitir definir os dias e faixas de horário de disponibilidade dos profissionais e utilizar essas informações para determinar os horários disponíveis para marcação.

#### RF04 — Marcar consulta

O sistema deve permitir selecionar paciente, profissional, data e horário disponível e criar a consulta com status `AGENDADA`.

#### RF05 — Remarcar ou cancelar consulta

O sistema deve permitir remarcar ou cancelar consultas que estejam com status `AGENDADA`, liberando o horário anterior quando aplicável.

#### RF06 — Impedir conflito de horário

O sistema deve verificar a disponibilidade do profissional antes de criar uma consulta e recusar uma marcação que conflite com outra consulta agendada para o mesmo horário.

#### RF07 — Registrar atendimento

O sistema deve permitir que o profissional registre a queixa, evolução e conduta do atendimento, vinculando essas informações ao paciente e à consulta.

#### RF08 — Emitir prescrição

O sistema deve permitir emitir prescrições vinculadas ao atendimento, contendo um ou mais medicamentos com informações como nome, dose e posologia.

#### RF09 — Solicitar exames

O sistema deve permitir gerar uma ou mais solicitações de exames vinculadas ao atendimento e acompanhar seu status.

#### RF10 — Consultar histórico

O sistema deve permitir que o profissional consulte, em ordem cronológica, os atendimentos, prescrições e exames relacionados ao paciente.

#### RF11 — Autenticar e autorizar usuários

O sistema deve autenticar usuários e controlar o acesso às funcionalidades conforme seus perfis: recepção, profissional, administrador e paciente.

#### RF12 — Auditar acesso ao prontuário

O sistema deve registrar os acessos e alterações relacionados ao prontuário, identificando o usuário, o paciente e o momento da operação.

#### RF13 — Gerar relatórios

O sistema deve gerar relatórios de consultas por período, profissional e especialidade, incluindo totalizadores.

## 3. Requisitos Não Funcionais — RNF

Os requisitos não funcionais são particularmente relevantes para a arquitetura do SaúdeCerta, pois envolvem segurança, privacidade, desempenho, disponibilidade, modificabilidade, usabilidade e auditabilidade.

| ID | Classificação | Requisito não funcional | Fundamentação |
| --- | --- | --- | --- |
| RNF01 | Segurança/Privacidade | O sistema deve impedir o acesso ao prontuário por usuários sem perfil autorizado. | Explícito no estudo de caso. |
| RNF02 | Auditabilidade | O sistema deve registrar os acessos ao prontuário, identificando usuário, paciente, data/hora e ação. | Explícito no estudo de caso. |
| RNF03 | Desempenho | A busca de pacientes deve retornar os resultados em menos de 2 segundos no percentil 95 em condições normais de uso. | Critério de qualidade definido no estudo de caso. |
| RNF04 | Disponibilidade | A agenda deve permanecer disponível durante o horário de funcionamento, considerando disponibilidade mensal mínima de 99% nesse período. | Critério de qualidade definido no estudo de caso. |
| RNF05 | Modificabilidade | A arquitetura deve permitir a inclusão de módulos futuros sem alteração significativa do núcleo de agenda e prontuário. | Explicitamente relacionada à evolução modular do sistema. |
| RNF06 | Usabilidade | Uma marcação simples de consulta deve poder ser concluída pela recepção em até 1 minuto e até 5 cliques a partir da tela inicial. | Critério de qualidade definido no estudo de caso. |
| RNF07 | Segurança de credenciais | As senhas não devem ser armazenadas em texto puro, devendo ser utilizado armazenamento seguro baseado em hash com sal. | Critério de segurança apresentado no estudo de caso. |

Os cenários de qualidade apresentados no estudo de caso fornecem medidas objetivas para desempenho, disponibilidade e usabilidade, além de condições verificáveis para segurança, auditabilidade e modificabilidade.

> **Observação:** os requisitos acima são derivados diretamente dos cenários de qualidade fornecidos no estudo de caso. O uso de uma tecnologia específica para implementação não é tratado como requisito nesta etapa.

## 4. Regras de Negócio — RN

As regras de negócio representam restrições e políticas próprias do domínio da clínica. Elas não descrevem simplesmente funcionalidades do sistema.

| ID | Regra de negócio |
| --- | --- |
| RN01 | Um paciente é identificado unicamente pelo CPF; não pode haver dois cadastros ativos com o mesmo CPF. |
| RN02 | Um profissional não pode possuir duas consultas com status `AGENDADA` no mesmo horário. |
| RN03 | Só é possível registrar atendimento para consultas cuja data já ocorreu ou corresponde ao dia atual. |
| RN04 | Uma prescrição ou solicitação de exame somente pode existir vinculada a um atendimento. |
| RN05 | O prontuário possui comportamento somente-anexação: registros de atendimento não são apagados, apenas complementados. |
| RN06 | O paciente somente pode visualizar os próprios dados e a recepção não pode acessar o conteúdo clínico do prontuário. |
| RN07 | O cancelamento de uma consulta somente pode ocorrer quando ela estiver com status `AGENDADA`. |

As regras acima são as regras de domínio explicitamente apresentadas no estudo de caso.

## 5. Drivers Arquiteturais — DA

Na abordagem apresentada em aula, drivers arquiteturais são os *concerns* mais influentes sobre a estrutura do sistema. Requisitos não funcionais frequentemente exercem esse papel porque determinam características importantes da solução arquitetural.

| ID | Driver | Justificativa |
| --- | --- | --- |
| DA01 | Segurança e privacidade | O sistema manipula dados sensíveis de saúde e precisa restringir o acesso ao prontuário conforme o perfil do usuário. |
| DA02 | Auditabilidade | Todo acesso ao prontuário deve ser rastreável, criando impacto direto na forma de registrar e processar eventos de acesso. |
| DA03 | Desempenho | A busca de pacientes e o acesso ao histórico devem ocorrer rapidamente, especialmente durante o atendimento e na operação da recepção. |
| DA04 | Usabilidade | A recepção opera em situações de alta demanda e precisa realizar agendamentos rapidamente, com baixa quantidade de interações. |
| DA05 | Modificabilidade | A solução deve permitir crescimento por módulos e futuras extensões sem reescrita do núcleo existente. |
| DA06 | Disponibilidade | A agenda é essencial para o funcionamento da clínica e deve permanecer disponível durante o horário de operação. |

### 5.1 Relação entre requisitos e drivers

| Driver | Requisitos relacionados |
| --- | --- |
| DA01 — Segurança e privacidade | RF11, RF12, RNF01, RNF07, RN06 |
| DA02 — Auditabilidade | RF12, RNF02 |
| DA03 — Desempenho | RF01, RF10, RNF03 |
| DA04 — Usabilidade | RF04, RF05, RNF06 |
| DA05 — Modificabilidade | RNF05 |
| DA06 — Disponibilidade | RNF04 |

Os drivers foram priorizados por sua influência direta sobre as decisões arquiteturais do sistema, e não apenas por serem atributos de qualidade genéricos.

## 6. Relação entre Drivers e Conflitos Arquiteturais

O estudo de caso apresenta dois conflitos relevantes:

### Segurança/Privacidade × Usabilidade

O controle de acesso deve ser suficientemente rigoroso para proteger os dados clínicos, mas não pode criar uma experiência excessivamente lenta ou complexa para os usuários, especialmente para a recepção.

### Auditabilidade × Desempenho

O sistema precisa registrar os acessos ao prontuário, mas o registro de auditoria não deve comprometer o tempo de resposta das operações clínicas.

Esses conflitos são explicitamente apontados no estudo de caso como situações que exigem decisões arquiteturais fundamentadas.

## 7. Estilo Arquitetural Inicial

### 7.1 Escolha

O estilo arquitetural inicial adotado para o SaúdeCerta é uma **arquitetura multicamadas com organização modular e aplicação dos princípios de Ports & Adapters (Arquitetura Hexagonal)**.

A implementação inicial deve permanecer como um **monólito modular**, com os módulos de negócio organizados e separados internamente.

### 7.2 Justificativa

Essa escolha é coerente com o contexto do SaúdeCerta porque o sistema possui complexidade suficiente para exigir separação de responsabilidades, mas não apresenta, nesta etapa, uma necessidade que justifique a complexidade operacional de uma arquitetura distribuída por microsserviços.

A arquitetura em camadas permite separar responsabilidades entre apresentação, aplicação/serviço, domínio e persistência. A arquitetura hexagonal complementa essa organização ao manter o domínio isolado das tecnologias de infraestrutura, permitindo que componentes externos sejam substituídos com menor impacto.

A organização modular também atende ao requisito de modificabilidade, pois os módulos de cadastro, agenda, atendimento, segurança e relatórios podem evoluir sem que todo o sistema precise ser reescrito.

A opção por um monólito modular também é compatível com o porte da clínica e com a necessidade de evitar complexidade operacional desnecessária no início. O próprio estudo de caso propõe começar dessa forma, mantendo a possibilidade de extração futura de serviços.

### 7.3 Estrutura arquitetural inicial

A estrutura conceitual proposta é:

| Camada | Responsabilidade |
| --- | --- |
| Apresentação | Expor a interface/API e traduzir as requisições e respostas. |
| Aplicação/Serviço | Orquestrar casos de uso, transações e regras de aplicação. |
| Domínio | Concentrar entidades, regras de negócio e invariantes. |
| Persistência | Implementar o acesso e armazenamento dos dados. |
| Transversal | Abranger segurança, auditoria e tratamento de erros. |

Essa organização corresponde à arquitetura multicamadas apresentada no estudo de caso.

## 8. Rastreabilidade Inicial

A rastreabilidade relaciona requisitos às decisões que deverão ser aprofundadas nas próximas etapas.

| Requisito/Regra | Driver | Implicação arquitetural |
| --- | --- | --- |
| RF06 / RN02 | DA03 | A validação de conflito deve ser realizada de forma consistente no domínio e na operação de agendamento. |
| RF11 / RN06 | DA01 | Necessidade de controle de acesso baseado em perfis. |
| RF12 / RNF02 | DA02 | Necessidade de mecanismo de auditoria dos acessos ao prontuário. |
| RNF03 | DA03 | Busca e persistência devem ser estruturadas para atender ao tempo de resposta definido. |
| RNF04 | DA06 | A infraestrutura deve suportar a disponibilidade esperada da agenda. |
| RNF05 | DA05 | Módulos devem possuir baixo acoplamento e responsabilidades bem definidas. |
| RNF06 | DA04 | Fluxos da recepção devem ser simples e eficientes. |
| RN05 | DA01 / DA02 | Histórico clínico deve preservar registros e impedir remoções indevidas. |

## 9. Escopo Arquitetural Futuro

A arquitetura inicial deve permitir evolução para uma arquitetura baseada em serviços no Projeto Final.

O estudo de caso prevê a possibilidade de extração de pelo menos um serviço do monólito, como o serviço de Agendamento ou Prontuário, mantendo o restante do sistema inicialmente integrado.

Essa possibilidade reforça a importância de manter fronteiras claras entre os módulos desde a primeira versão, sem transformar a aplicação inicial em uma arquitetura distribuída antes que exista justificativa para isso.

## 10. Conclusão

Os requisitos do SaúdeCerta mostram que a principal preocupação arquitetural não está apenas nas funcionalidades clínicas, mas na necessidade de conciliá-las com segurança, privacidade, auditabilidade, desempenho, disponibilidade, usabilidade e capacidade de evolução.

A partir desses drivers, a escolha de uma arquitetura multicamadas, modular e orientada pelos princípios de Ports & Adapters fornece uma base adequada para a primeira etapa do projeto, mantendo o sistema suficientemente simples para implementação inicial e preparado para evolução posterior.