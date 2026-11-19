package co.crediflow.api;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/** Reglas de capas verificadas en cada build: la arquitectura no se degrada con el tiempo. */
@AnalyzeClasses(packages = "co.crediflow.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule capas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy("co.crediflow.api.web")
            .layer("DTO").definedBy("co.crediflow.api.web.dto..")
            .layer("Servicio").definedBy("co.crediflow.api.service..")
            .layer("Mapper").definedBy("co.crediflow.api.mapper..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("DTO").mayOnlyBeAccessedByLayers("Web", "Mapper")
            .whereLayer("Servicio").mayOnlyBeAccessedByLayers("Web")
            .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Web");

    @ArchTest
    static final ArchRule controladoresSinRepositorios = noClasses()
            .that().resideInAPackage("co.crediflow.api.web..")
            .should().dependOnClassesThat().resideInAPackage("co.crediflow.db.repository..")
            .because("los controladores pasan siempre por la capa de servicio");

    @ArchTest
    static final ArchRule controladoresSinPlsql = noClasses()
            .that().resideInAPackage("co.crediflow.api.web..")
            .should().dependOnClassesThat().resideInAPackage("co.crediflow.db.plsql..")
            .because("el acceso a PL/SQL se encapsula en servicios");

    @ArchTest
    static final ArchRule serviciosSinWeb = noClasses()
            .that().resideInAPackage("co.crediflow.api.service..")
            .should().dependOnClassesThat().resideInAnyPackage("co.crediflow.api.web..", "org.springframework.web..")
            .because("la lógica de negocio no conoce HTTP");
}
