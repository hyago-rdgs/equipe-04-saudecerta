package br.edu.ifpa.saudecerta.auditoria.aplicacao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.auditoria.api.Auditoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.auditoria.modelo.RegistroDeAuditoria;
import br.edu.ifpa.saudecerta.auditoria.persistencia.RegistroDeAuditoriaRepository;

/**
 * Auditoria SINCRONA, gravada no mesmo banco (ADR 0003).
 *
 * REQUIRES_NEW: o registro e gravado em transacao propria. Se a operacao de
 * negocio falhar e sofrer rollback, a trilha permanece - tentativas tambem
 * interessam. O custo: cada operacao auditada faz uma escrita a mais antes de
 * responder. A alternativa assincrona por eventos e a missao PF-2.
 */
@Service
public class AuditoriaService implements Auditoria {

    private final RegistroDeAuditoriaRepository registros;
    private final Clock relogio;

    public AuditoriaService(RegistroDeAuditoriaRepository registros, Clock relogio) {
        this.registros = registros;
        this.relogio = relogio;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EventoDeAuditoria evento) {
        registros.save(new RegistroDeAuditoria(LocalDateTime.now(relogio), limitar(evento.usuario(), 60),
                evento.perfil(), evento.acao(), evento.recurso(), evento.recursoId(), evento.pacienteId(),
                limitar(evento.detalhe(), 300)));
    }

    @Transactional(readOnly = true)
    public List<RegistroDeAuditoria> recentes(Long pacienteId) {
        return pacienteId == null
                ? registros.findTop200ByOrderByIdDesc()
                : registros.findTop200ByPacienteIdOrderByIdDesc(pacienteId);
    }

    private static String limitar(String texto, int tamanho) {
        return texto == null || texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }
}
