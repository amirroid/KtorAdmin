package processors.hibernate

import annotations.hibernate.HibernateTable
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.writeTo
import models.ColumnSet
import repository.PropertiesRepository
import utils.Constants
import utils.FileUtils
import utils.toSuitableStringForFile
import kotlin.sequences.forEach

class HibernateTableProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(HibernateTable::class.qualifiedName ?: return emptyList())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach(::generateDesiredClass)
        return emptyList()
    }

    private fun generateDesiredClass(classDeclaration: KSClassDeclaration) {
        val containingFile = classDeclaration.containingFile ?: return
        val packageName = "${Constants.PACKAGE_NAME}.ktorAdmin.hibernate"
        val simpleFileName = classDeclaration.simpleName.asString()
        val fileName = FileUtils.getGeneratedFileName(simpleFileName)
        val columns = classDeclaration.getAllColumnSets()
        val myProperty = PropertySpec.builder("myVariable", String::class)
            .initializer("%S", columns.joinToString { it.toSuitableStringForFile() })
            .build()
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addProperty(myProperty)
            .build()
        fileSpec.writeTo(
            environment.codeGenerator,
            Dependencies(
                false,
                containingFile
            )
        )
    }

    private fun KSClassDeclaration.getAllColumnSets(): List<ColumnSet> {
        val columns = mutableListOf<ColumnSet>()
        getDeclaredProperties().forEach { property ->
            val type = property.type.resolve()
            PropertiesRepository.getColumnSets(property, type)?.let {
                columns += it
            }
        }
        return columns
    }
}