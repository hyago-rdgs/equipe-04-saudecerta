package br.edu.ifpa.saudecerta.seguranca.persistencia;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpa.saudecerta.seguranca.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);
}
