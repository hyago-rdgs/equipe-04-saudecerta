package br.edu.ifpa.saudecerta.auditoria.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpa.saudecerta.auditoria.modelo.RegistroDeAuditoria;

public interface RegistroDeAuditoriaRepository extends JpaRepository<RegistroDeAuditoria, Long> {

    List<RegistroDeAuditoria> findTop200ByOrderByIdDesc();

    List<RegistroDeAuditoria> findTop200ByPacienteIdOrderByIdDesc(Long pacienteId);
}
