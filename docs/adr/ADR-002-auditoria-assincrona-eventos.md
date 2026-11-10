# ADR-002 — Auditoria por eventos de domínio com outbox transacional e hash encadeado

| Campo | Valor |
|---|---|
| Status | **Aceito** |
| Data | 10/11/2026 |
| Decisores | Equipe SaúdeCerta |
| Drivers | DA2 Auditabilidade · DA4 Desempenho · DA3 Modificabilidade |
| Requisitos | RF12, RF17, RNF01, RNF02, RNF03, RN05 |

## Contexto

O RNF02 exige que **todo** acesso ou alteração de prontuário seja registrado (usuário, paciente, data/hora, ação), com o registro disponível ao administrador em até 1 minuto. O RNF01 exige registrar também as tentativas negadas. Ao mesmo tempo, abrir o prontuário tem de ser rápido (RNF03/QS-08).

A E1 deixou uma questão em aberto: se a auditoria for assíncrona, um evento pode se perder se a aplicação cair entre o acesso e o registro. Isso violaria a medida “0 acessos sem registro”.

Há ainda um requisito implícito: uma trilha de auditoria que um DBA pode editar sem deixar rastro tem pouco valor como prova (LGPD, RNF08).

## Alternativas consideradas

| # | Alternativa | Garante registro? | Impacto na leitura | Acoplamento | Complexidade |
|---|---|---|---|---|---|
| A | **Síncrona** — `ProntuarioService` chama `AuditoriaService.registrar()` na mesma transação | Sim | Médio: grava o registro completo, calcula o hash e disputa o bloqueio da cadeia a cada leitura | Alto: Prontuário conhece Auditoria | Baixa |
| B | **`@Async` simples** — evento disparado e esquecido | **Não**: queda entre o commit e o processamento perde o evento | Baixo | Baixo | Baixa |
| C | **Evento + outbox transacional** (Spring Modulith *Event Publication Registry*) + listener assíncrono | **Sim**: o evento é gravado na mesma transação e reprocessado se o listener falhar | Baixo: um `INSERT` pequeno | Baixo | Média |
| D | **Broker externo** (RabbitMQ/Kafka) + consumidor | Só com outbox; publicar direto no broker tem o problema de *dual write* | Baixo | Baixo | Alta: novo contêiner para operar |
| E | **AOP + arquivo de log** | Depende do flush do log | Baixo | Baixo | Baixa, mas consultar e proteger a trilha fica difícil |

## Análise custo/benefício

| Critério | Peso | A | B | **C** | D | E |
|---|---|---|---|---|---|---|
| Garantia de registro (RNF02) | 5 | 5 | 1 | **5** | 5 | 2 |
| Desempenho da leitura (RNF03) | 3 | 3 | 5 | **4** | 4 | 5 |
| Desacoplamento (RNF05) | 3 | 1 | 5 | **5** | 5 | 4 |
| Integridade/consulta da trilha | 4 | 4 | 4 | **4** | 4 | 1 |
| Custo de implementação e operação (inverso) | 4 | 5 | 5 | **4** | 1 | 4 |
| **Total (máx. 95)** | | 73 | 71 | **84** | 72 | 57 |

O custo de C sobre A é pequeno: uma dependência (`spring-modulith-starter-jpa`), uma tabela (`event_publication`) e um listener. O benefício é desacoplar a auditoria sem abrir mão da garantia. D entrega o mesmo resultado com um contêiner a mais, o que não se justifica dentro de um processo único (ver ADR-001).

## Decisão

Adotamos **C**:

1. O `ProntuarioService` publica `ProntuarioAcessado(usuarioId, pacienteId, acao, instante, origemIp)` e `AtendimentoRegistrado(...)`. O `AccessDeniedHandler` publica `TentativaAcessoNegada`. Os eventos são `record`s públicos do módulo emissor.
2. O Spring Modulith grava cada publicação na tabela `event_publication` **na mesma transação** do caso de uso. Por isso, `consultarHistorico` usa `@Transactional` comum, e não `readOnly = true`: no PostgreSQL uma transação somente-leitura rejeitaria o `INSERT` do outbox.
3. O `AuditoriaListener` (`@ApplicationModuleListener`, que é assíncrono, roda após o commit e abre transação própria) grava o `RegistroAuditoria` e marca a publicação como concluída.
4. Publicações incompletas são reenviadas na inicialização (`spring.modulith.events.republish-outstanding-events-on-restart=true`) e por um agendamento a cada 30 s. O listener é **idempotente**: o ID do evento é chave única em `registro_auditoria`.
5. **Imutabilidade:** cada registro guarda `hash = SHA-256(hashAnterior ‖ conteúdo canônico)`. O listener roda num executor dedicado de **uma thread**, o que serializa a cadeia sem disputa de bloqueio. O papel de banco da aplicação não tem `UPDATE`/`DELETE` em `auditoria.*`.
6. A trilha registra **quem, qual paciente, quando, qual ação e o resultado**, nunca o conteúdo clínico (minimização, RNF08).

```java
@Service
class ProntuarioService implements ConsultarProntuarioUseCase {
    @Transactional
    @PreAuthorize("hasRole('PROFISSIONAL') or @titular.ehDono(#pacienteId)")
    public HistoricoDto consultarHistorico(Long pacienteId, Pageable pagina, Usuario usuario) {
        var historico = atendimentos.historicoDe(pacienteId, pagina);
        eventos.publishEvent(new ProntuarioAcessado(usuario.id(), pacienteId, Acao.LEITURA, Instant.now(clock)));
        return HistoricoDto.de(historico);
    }
}

@Component
class AuditoriaListener {
    @ApplicationModuleListener
    void on(ProntuarioAcessado e) { trilha.anexar(RegistroAuditoria.de(e)); }
}
```

## Consequências

**Positivas**

- (+) Nenhum acesso fica sem registro, mesmo com queda (QS-03).
- (+) A leitura paga só um `INSERT` pequeno; hash e gravação final ficam fora do caminho crítico (QS-08).
- (+) Alteração na trilha é detectável (QS-04).
- (+) O Prontuário não conhece a Auditoria. O futuro faturamento pode escutar `AtendimentoRegistrado` do mesmo jeito (QS-10).

**Negativas e mitigação**

- (−) Defasagem de segundos entre acesso e registro. É aceita, porque a medida do RNF02 é ≤ 1 min. Monitoramos a idade da publicação incompleta mais antiga pelo Actuator.
- (−) A tabela `event_publication` cresce → publicações concluídas são removidas após 7 dias.
- (−) Uma thread única limita a vazão a algumas centenas de registros por segundo. A carga estimada é de ~2 mil por **dia**, uma folga de várias ordens de grandeza.

## Verificação

- QS-03: teste com `Scenario` do Spring Modulith e teste de falha do listener com reenvio.
- QS-04: teste de adulteração da cadeia.
- Teste de integração: `GET /prontuario` → em até 5 s existe `RegistroAuditoria` com o usuário e o paciente corretos.

## Quando revisar

No PF, se o Prontuário for extraído para um serviço, os eventos passam a sair do processo. Aí a tabela `event_publication` passa a alimentar um broker (Spring Modulith *externalization*), sem mudar os emissores.
