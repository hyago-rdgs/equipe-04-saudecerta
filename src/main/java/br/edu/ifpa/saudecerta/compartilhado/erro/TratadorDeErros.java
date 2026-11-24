package br.edu.ifpa.saudecerta.compartilhado.erro;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes em respostas HTTP padronizadas (RFC 9457 - Problem Details).
 * Os controllers nao tem try/catch: eles so descrevem o caminho feliz.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regra(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail acessoNegado(AcessoNegadoException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", e.getMessage());
    }

    @ExceptionHandler(MissaoPendenteException.class)
    public ProblemDetail missao(MissaoPendenteException e) {
        ProblemDetail p = problema(HttpStatus.NOT_IMPLEMENTED, "Missão pendente", e.getMessage());
        p.setProperty("missao", e.getMissao());
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Confira os campos destacados e tente novamente.");
        p.setProperty("campos", campos);
        return p;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail requisicaoIlegivel(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Não foi possível interpretar os dados enviados. Confira datas, números e o formato do JSON.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalhe);
        p.setTitle(titulo);
        return p;
    }
}
