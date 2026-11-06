# Visão — SaúdeCerta

## 1. Contexto

A Clínica SaúdeCerta é uma unidade de saúde de médio porte, com cerca de vinte profissionais de diferentes especialidades. Atualmente, o controle da agenda é realizado por meio de planilhas compartilhadas e os prontuários são mantidos em papel e armazenados em pastas físicas.

Esse cenário provoca conflitos de horários, demora na localização do histórico dos pacientes, retrabalho em remarcações e riscos relacionados ao acesso e manuseio de dados sensíveis de saúde. Também não existe um registro adequado de quem acessou ou alterou informações do prontuário.

A direção da clínica decidiu desenvolver um sistema web multicamadas para digitalizar o fluxo de atendimento, desde a marcação da consulta até o registro do atendimento, prontuário e prescrição, preservando a privacidade das informações e permitindo a evolução do sistema por módulos.

O estudo de caso destaca uma tensão arquitetural entre segurança e privacidade, usabilidade da recepção, desempenho na consulta de prontuários e auditabilidade dos acessos.

## 2. Problema

O processo atual da SaúdeCerta depende de mecanismos manuais e pouco integrados para controlar a agenda e os prontuários.

Os principais problemas identificados são:

- marcações conflitantes para o mesmo profissional e horário;
- dificuldade e demora para localizar o histórico de pacientes;
- retrabalho durante remarcações de consultas;
- armazenamento físico dos prontuários;
- risco no manuseio de dados sensíveis de saúde;
- ausência de uma trilha adequada de auditoria dos acessos ao prontuário.

Esses problemas afetam diretamente a eficiência da recepção, o trabalho dos profissionais de saúde, a experiência dos pacientes e a segurança das informações clínicas.

## 3. Objetivo do Produto

Desenvolver um sistema web multicamadas para digitalizar e organizar o fluxo de atendimento da Clínica SaúdeCerta, abrangendo o cadastro de pacientes e profissionais, agendamento de consultas, atendimento, prontuário eletrônico, prescrições, solicitações de exames, controle de acesso e auditoria.

O sistema deverá preservar a privacidade dos dados de saúde e possuir uma estrutura que permita sua evolução por módulos sem necessidade de reescrever o núcleo existente.

## 4. Visão Geral da Solução

A solução proposta será um sistema web que centralizará as principais informações e operações da clínica.

O produto deverá:

- substituir o controle da agenda por um mecanismo digital de disponibilidade e agendamento;
- evitar conflitos de horários;
- manter o histórico clínico dos pacientes em prontuário eletrônico;
- permitir o registro dos atendimentos;
- permitir emissão de prescrições e solicitação de exames;
- controlar o acesso às informações de acordo com o perfil do usuário;
- registrar os acessos ao prontuário para fins de auditoria;
- fornecer relatórios gerenciais básicos;
- manter módulos de negócio organizados para permitir evolução futura.

## 5. Escopo Inicial

### Dentro do escopo

- Cadastro de pacientes.
- Cadastro de profissionais.
- Cadastro de especialidades.
- Definição da disponibilidade dos profissionais.
- Marcação de consultas.
- Remarcação e cancelamento de consultas.
- Verificação de conflitos de horário.
- Atendimento e registro no prontuário eletrônico.
- Consulta ao histórico do paciente.
- Emissão de prescrições.
- Solicitação de exames.
- Autenticação de usuários.
- Autorização por perfil.
- Auditoria de acesso ao prontuário.
- Relatórios gerenciais básicos.

Essas funcionalidades correspondem aos módulos previstos de Cadastros, Agenda, Atendimento, Prescrição/Exames, Segurança e Relatórios.

### Fora do escopo inicial

Os seguintes itens estão explicitamente fora do escopo:

- faturamento;
- integração com convênios;
- integração com laboratórios externos para laudos;
- telemedicina ou teleconsulta;
- assinatura digital com certificado ICP-Brasil.

Esses elementos são considerados possíveis pontos de extensão futura.

## 6. Stakeholders

Stakeholders são os interessados no sistema e possuem diferentes preocupações em relação ao produto. A aula destaca que usuários, gestão, equipe de desenvolvimento e operação podem possuir interesses distintos, como rapidez, custo, manutenibilidade e disponibilidade.

| Stakeholder | Interesse principal |
| --- | --- |
| Direção/Gestão da clínica | Melhorar a gestão da clínica e acompanhar informações por meio de relatórios. |
| Recepcionistas | Realizar cadastros e operações de agenda de forma rápida e evitar conflitos. |
| Profissionais de saúde | Acessar o histórico dos pacientes e registrar atendimentos, prescrições e exames. |
| Pacientes | Consultar seus agendamentos e seus próprios dados. |
| Administrador | Gerenciar usuários, perfis, especialidades, relatórios e auditoria. |
| Equipe de desenvolvimento | Manter e evoluir o sistema de forma organizada. |
| Operação/Infraestrutura | Garantir disponibilidade e funcionamento adequado do sistema. |

## 7. Atores

| Ator | Objetivos principais |
| --- | --- |
| Recepcionista | Cadastrar pacientes, marcar/remarcar consultas e evitar conflitos de agenda. |
| Profissional de saúde | Consultar histórico, registrar atendimento, emitir prescrições e solicitar exames. |
| Paciente | Consultar seus agendamentos e seus dados; solicitar ou confirmar consultas. |
| Administrador | Gerenciar usuários e especialidades, acompanhar relatórios e auditoria. |

Os quatro atores são definidos diretamente no estudo de caso.

## 8. Benefícios Esperados

Com a implantação do SaúdeCerta, espera-se:

- redução de conflitos de horários;
- maior agilidade no processo de agendamento;
- redução do retrabalho relacionado a remarcações;
- acesso mais rápido ao histórico dos pacientes;
- substituição dos prontuários físicos por prontuário eletrônico;
- maior controle sobre o acesso aos dados clínicos;
- rastreabilidade das operações relacionadas ao prontuário;
- melhoria da gestão por meio de relatórios;
- maior facilidade para evolução e inclusão de novos módulos.

## 9. Direcionadores da Solução

A arquitetura deverá considerar principalmente os seguintes aspectos:

- **Segurança e privacidade:** os dados de saúde são sensíveis e devem ser acessados apenas por perfis autorizados.
- **Auditabilidade:** os acessos ao prontuário precisam ser registrados.
- **Desempenho:** operações como busca de pacientes e abertura de prontuários devem ocorrer rapidamente.
- **Usabilidade:** a recepção precisa executar operações de agenda com agilidade.
- **Modificabilidade:** a estrutura deve permitir evolução por módulos e futuras extensões.

Essas preocupações correspondem aos atributos de qualidade e drivers destacados no estudo de caso.

## 10. Limitações e Premissas

Nesta primeira etapa, o sistema está delimitado ao fluxo de atendimento e gestão clínica descrito no estudo de caso. Não fazem parte da solução inicial os recursos explicitamente definidos como fora do escopo.

A solução deverá ser projetada considerando a possibilidade de evolução futura, mas essa possibilidade não implica implementar os módulos ou integrações que foram excluídos da primeira versão.

## 11. Síntese da Visão

O SaúdeCerta tem como propósito substituir processos manuais de agenda e prontuário por uma solução web integrada, segura e modular. O sistema deverá atender às necessidades de recepcionistas, profissionais de saúde, pacientes e administradores, conciliando agilidade operacional com segurança, privacidade e auditabilidade.

A visão está alinhada à orientação da disciplina de utilizar os requisitos e os interesses dos stakeholders como base para as decisões arquiteturais posteriores.