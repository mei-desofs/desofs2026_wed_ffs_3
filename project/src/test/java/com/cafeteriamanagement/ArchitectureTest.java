package com.cafeteriamanagement;

    import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.cafeteriamanagement")
public class ArchitectureTest {

    /*// 1️⃣ Serviços não devem acessar outros serviços diretamente
    @ArchTest
    public static final ArchRule service_nao_acessa_outros_services =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..service..")
                    .because("Serviços não devem ter dependências entre si para evitar acoplamento");

    // 2️⃣ Serviços podem acessar apenas Repository, Entity, DTO, Util e bibliotecas padrão
    @ArchTest
    public static final ArchRule service_dependencias_permitidas =
            classes().that().resideInAPackage("..service..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "..service..",          // Próprio pacote (classes internas)
                            "..repository..",       // Repositórios
                            "..entity..",           // Entidades
                            "..model..",            // Modelos/DTOs
                            "..dto..",              // DTOs
                            "..exception..",        // Exceções customizadas
                            "..util..",             // Utilitários
                            "java..",               // Classes Java padrão
                            "javax..",              // Java EE
                            "jakarta..",            // Jakarta EE
                            "org.springframework..", // Spring Framework
                            "org.slf4j..",          // Logging
                            ""                      // Pacote raiz (permite primitivos)
                    );

    // 3️⃣ Repositories não devem acessar Services
    @ArchTest
    public static final ArchRule repository_nao_acessa_service =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..service..")
                    .because("Repositories não devem depender de Services (inversão de dependência)");

    // 4️⃣ Repositories não devem acessar outros Repositories
    @ArchTest
    public static final ArchRule repository_nao_acessa_outro_repository =
            noClasses().that().resideInAPackage("..repository..")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .andShould().haveSimpleNameEndingWith("Repository")
                    .because("Repositories não devem ter dependências entre si");*/

    // DIOGO -- Classes de Service devem ser públicas e ter sufixo "Service" -- Diogo
    @ArchTest
    public static final ArchRule services_must_be_public_and_name =
            classes().that().resideInAPackage("..service..")
                    .and().haveSimpleNameEndingWith("Service")
                    .should().bePublic()
                    .because("Services devem ser públicos para serem injetados");

    // DIOGO -- Controllers não devem acessar Repositories diretamente
    @ArchTest
    public static final ArchRule controller_dont_access_repos =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .because("Controllers devem acessar apenas Services");

    // GONDAR -- Repositories devem ser públicos
    @ArchTest
    public static final ArchRule repositories_devem_ser_publicos =
            classes().that().resideInAPackage("..repository..")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().bePublic()
                    .because("Repositories devem ser públicos para serem injetados e usados pelos Services");

    // GONDAR -- Controllers devem ser públicos
    @ArchTest
    public static final ArchRule controllers_devem_ser_publicos =
            classes().that().resideInAPackage("..controller..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().bePublic()
                    .because("Controllers devem ser públicos para serem expostos via Spring MVC");

}

    /*// 6️⃣ Classes de Repository devem ter sufixo "Repository"
    @ArchTest
    public static final ArchRule repository_nome_sufixo =
            classes().that().resideInAPackage("..repository..")
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Repositories devem seguir convenção de nomenclatura");

    *///

    /*// 8️⃣ Entities não devem depender de Services ou Repositories
    @ArchTest
    public static final ArchRule entity_sem_dependencias_camadas =
            noClasses().that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..service..", "..repository..", "..controller..")
                    .because("Entities devem ser POJOs sem dependências de camadas superiores");

    // 9️⃣ Nenhuma classe de produção deve acessar classes de teste
    /*@ArchTest
    public static final ArchRule producao_nao_acessa_test =
            noClasses().that().resideOutsideOfPackage("..test..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..test..")
                    .because("Código de produção não deve depender de código de teste");*/
/*
    // 🔟 Services devem ter anotação @Service
    @ArchTest
    public static final ArchRule services_devem_ter_anotacao =
            classes().that().resideInAPackage("..service..")
                    .and().haveSimpleNameEndingWith("Service")
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .because("Services devem ser componentes");
}*/