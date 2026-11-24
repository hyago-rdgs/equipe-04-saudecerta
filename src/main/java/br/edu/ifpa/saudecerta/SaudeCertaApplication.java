package br.edu.ifpa.saudecerta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SaudeCerta - gestao de clinica.
 *
 * Monolito modular: cada pacote de primeiro nivel e um modulo
 * (cadastro, agenda, prontuario, auditoria, seguranca, painel).
 * Veja docs/arquitetura.md antes de mexer no codigo.
 */
@SpringBootApplication
public class SaudeCertaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaudeCertaApplication.class, args);
    }
}
