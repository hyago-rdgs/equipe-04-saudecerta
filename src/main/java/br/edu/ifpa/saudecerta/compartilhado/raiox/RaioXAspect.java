package br.edu.ifpa.saudecerta.compartilhado.raiox;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Observa as chamadas entre camadas usando AOP, sem tocar no codigo de negocio.
 *
 * A camada de cada componente e deduzida do NOME DO PACOTE:
 *   ..web..           -> Apresentacao
 *   ..aplicacao..     -> Aplicacao
 *   ..persistencia..  -> Persistencia (adaptadores e repositorios Spring Data)
 *
 * O dominio nao aparece no rastro: sao objetos Java puros, que o Spring nao
 * gerencia e portanto nao intercepta. Pergunta para a equipe: isso e um
 * defeito do Raio-X ou um bom sinal sobre o dominio?
 */
@Aspect
@Component
@ConditionalOnProperty(name = "saudecerta.raio-x.ativo", havingValue = "true")
public class RaioXAspect {

    private static final String BASE = "br.edu.ifpa.saudecerta";

    @Around("within(br.edu.ifpa.saudecerta..web..*)"
            + " || within(br.edu.ifpa.saudecerta..aplicacao..*)"
            + " || within(br.edu.ifpa.saudecerta..persistencia..*)"
            + " || execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object observar(ProceedingJoinPoint ponto) throws Throwable {
        Rastro rastro = RaioXContexto.atual();
        if (rastro == null) {
            return ponto.proceed();
        }
        Class<?> tipo = tipoDoComponente(ponto);
        Passo passo = rastro.entrar(camadaDe(tipo), tipo.getSimpleName(), ponto.getSignature().getName());
        long inicio = System.nanoTime();
        try {
            return ponto.proceed();
        } finally {
            rastro.sair(passo, System.nanoTime() - inicio);
        }
    }

    /** Repositorios Spring Data sao proxies: procuramos a interface declarada no projeto. */
    private static Class<?> tipoDoComponente(ProceedingJoinPoint ponto) {
        Class<?> tipo = ClassUtils.getUserClass(ponto.getTarget());
        if (tipo.getName().startsWith(BASE)) {
            return tipo;
        }
        for (Class<?> contrato : ponto.getThis().getClass().getInterfaces()) {
            if (contrato.getName().startsWith(BASE)) {
                return contrato;
            }
        }
        return tipo;
    }

    private static String camadaDe(Class<?> tipo) {
        String pacote = tipo.getPackageName();
        if (pacote.contains(".web")) {
            return "Apresentação";
        }
        if (pacote.contains(".aplicacao")) {
            return "Aplicação";
        }
        return "Persistência";
    }
}
