package br.edu.ifpa.saudecerta.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A ARQUITETURA COMO TESTE.
 *
 * Um diagrama pode ficar desatualizado sem ninguem perceber; estas regras nao.
 * Se alguem importar a classe errada, "mvn test" falha e mostra a linha.
 *
 * Experimento para a aula: dentro de Consulta (dominio), importe
 * org.springframework.stereotype.Component e rode os testes.
 */
@AnalyzeClasses(packages = "br.edu.ifpa.saudecerta", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String RAIZ = "br.edu.ifpa.saudecerta";

    // ---------- Hexagonal (agenda e prontuario) ----------

    @ArchTest
    static final ArchRule dominio_nao_conhece_framework_nem_camadas_externas = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "..aplicacao..", "..adaptador..")
            .because("o dominio guarda as regras de negocio e deve sobreviver a troca de framework (ADR 0002)");

    @ArchTest
    static final ArchRule aplicacao_nao_conhece_adaptadores = noClasses()
            .that().resideInAPackage("..aplicacao..")
            .should().dependOnClassesThat().resideInAPackage("..adaptador..")
            .because("os casos de uso falam com o mundo externo apenas por portas (interfaces)");

    // ---------- Camadas ----------

    @ArchTest
    static final ArchRule apresentacao_nao_fala_direto_com_persistencia = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..persistencia..")
            .because("controllers passam pela camada de aplicacao; nunca acessam repositorios");

    @ArchTest
    static final ArchRule persistencia_nao_depende_de_camadas_de_cima = noClasses()
            .that().resideInAPackage("..persistencia..")
            .should().dependOnClassesThat().resideInAPackage("..web..")
            .because("as dependencias apontam de cima para baixo");

    @ArchTest
    static final ArchRule controllers_ficam_na_apresentacao = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAnyPackage("..web..", "..raiox..")
            .because("a camada de um componente e identificada pelo pacote");

    // ---------- Monolito modular ----------

    @ArchTest
    static final ArchRule cadastro_so_e_usado_pela_api = modulo("cadastro", "modelo", "persistencia", "aplicacao", "web");

    @ArchTest
    static final ArchRule agenda_so_e_usada_pela_api = modulo("agenda", "dominio", "aplicacao", "adaptador");

    @ArchTest
    static final ArchRule prontuario_e_fechado = modulo("prontuario", "dominio", "aplicacao", "adaptador");

    @ArchTest
    static final ArchRule auditoria_so_e_usada_pela_api = modulo("auditoria", "modelo", "persistencia", "aplicacao", "web");

    @ArchTest
    static final ArchRule seguranca_so_e_usada_pela_api = modulo("seguranca", "modelo", "persistencia", "aplicacao", "web", "config");

    @ArchTest
    static final ArchRule modulos_sem_dependencia_circular = slices()
            .matching(RAIZ + ".(*)..")
            .should().beFreeOfCycles()
            .because("ciclos entre modulos impedem extrair um deles como servico (Projeto Final)");

    /**
     * Quem esta fora do modulo so enxerga o pacote "api" dele.
     * Excecao: o pacote demo, que monta o cenario de demonstracao.
     */
    private static ArchRule modulo(String nome, String... pacotesInternos) {
        String[] internos = new String[pacotesInternos.length];
        for (int i = 0; i < pacotesInternos.length; i++) {
            internos[i] = RAIZ + "." + nome + "." + pacotesInternos[i] + "..";
        }
        return noClasses()
                .that().resideOutsideOfPackages(RAIZ + "." + nome + "..", RAIZ + ".demo..")
                .should().dependOnClassesThat().resideInAnyPackage(internos)
                .because("outros modulos devem usar apenas " + nome + ".api");
    }
}
