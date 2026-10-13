package com.example.pokedex;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the Clean Architecture dependency rule: dependencies point inward only.
 */
@AnalyzeClasses(packages = "com.example.pokedex", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule layersDependInwardOnly = layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.withOptionalLayers(true)
			.layer("Domain").definedBy("..pokedex.domain..")
			.layer("Application").definedBy("..pokedex.application..")
			.layer("Infrastructure").definedBy("..pokedex.infrastructure..")
			.layer("Web").definedBy("..pokedex.web..")
			.whereLayer("Web").mayNotBeAccessedByAnyLayer()
			.whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
			.whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Infrastructure")
			.whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Web", "Infrastructure");

	@ArchTest
	static final ArchRule domainIsFrameworkFree = noClasses()
			.that().resideInAPackage("..pokedex.domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "tools.jackson..", "com.fasterxml.jackson..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule applicationIsFrameworkFree = noClasses()
			.that().resideInAPackage("..pokedex.application..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "tools.jackson..", "com.fasterxml.jackson..")
			.allowEmptyShould(true);
}
