package br.edu.ifpa.saudecerta.cadastro.modelo;

/** Regras do CPF: limpeza, digitos verificadores e mascara. */
public final class Cpf {

    private Cpf() {
    }

    /** Mantem apenas os digitos. */
    public static String limpar(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String d = limpar(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (d.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String formatar(String cpf) {
        String d = limpar(cpf);
        if (d.length() != 11) {
            return cpf;
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
