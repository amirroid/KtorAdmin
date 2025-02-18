package processors.exposed

import annotations.exposed.ExposedTable
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import models.ColumnSet
import models.Limit
import models.UploadTarget
import models.actions.Action
import models.chart.AdminChartStyle
import models.common.Reference
import models.date.AutoNowDate
import models.reference.EmptyColumn
import models.types.ColumnType
import repository.AnnotationRepository
import repository.PropertiesRepository
import utils.Constants
import utils.FileUtils
import utils.PackagesUtils

/**
 * A Kotlin Symbol Processing (KSP) processor that generates classes for Exposed table definitions.
 * This processor scans classes annotated with `@ExposedTable`, validates their structure, extracts
 * relevant metadata, and generates a corresponding class with properties, queries, and actions.
 *
 * Key functionalities:
 * - Extracts column sets and validates primary keys.
 * - Generates default and custom actions for admin interactions.
 * - Handles search and filter query parameters.
 * - Generates table metadata, including display formats and access roles.
 *
 * @property environment The KSP environment providing code generation capabilities.
 */

class ExposedTableProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ExposedTable::class.qualifiedName ?: return emptyList())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach(::generateDesiredClass)
        return emptyList()
    }

    private fun generateDesiredClass(classDeclaration: KSClassDeclaration) {
        classDeclaration.validateImplementations()
        val containingFile = classDeclaration.containingFile ?: return
        val packageName = "${Constants.PACKAGE_NAME}.ktorAdmin.exposed"
        val simpleFileName = classDeclaration.simpleName.asString()
        val fileName = FileUtils.getGeneratedFileName(simpleFileName)
        val columns = classDeclaration.getAllColumnSets()
        val generatedClass = generateClass(classDeclaration, fileName, columns)
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addImport(ColumnType::class.java.packageName, ColumnType::class.java.simpleName)
            .addImport(UploadTarget::class.java.packageName, UploadTarget::class.java.simpleName)
            .addImport(Limit::class.java.packageName, Limit::class.java.simpleName)
            .addImport(Reference::class.java.packageName, Reference::class.java.simpleName)
            .addImport(AutoNowDate::class.java.packageName, AutoNowDate::class.java.simpleName)
            .addImport(Action::class.java.packageName, Action::class.java.simpleName)
            .addImport(AdminChartStyle::class.java.packageName, AdminChartStyle::class.java.simpleName)
            .addType(generatedClass)
            .build()
        fileSpec.writeTo(
            environment.codeGenerator,
            Dependencies(
                false,
                containingFile
            )
        )
    }

    private fun generateClass(
        classDeclaration: KSClassDeclaration,
        fileName: String,
        columnSets: List<ColumnSet>
    ): TypeSpec {
        val adminTable = PackagesUtils.getAdminTableClass()

        val primaryKey = classDeclaration.getPrimaryKey()
        if (!columnSets.any { it.columnName == primaryKey }) {
            throw IllegalArgumentException("(${classDeclaration.simpleName.asString()}) The provided primary key does not match any column in the table.")
        }

        val tableName = classDeclaration.getTableName()
        return TypeSpec.classBuilder(fileName)
            .addSuperinterfaces(listOf(adminTable))
            .let {
                AnnotationRepository.addCommonFunctionsToClass(
                    typedSpec = it,
                    classDeclaration = classDeclaration,
                    columnSets = columnSets,
                    singularName = classDeclaration.getSingularName(tableName),
                    pluralName = classDeclaration.getPluralName(tableName),
                    tableName = tableName,
                    databaseKey = classDeclaration.getDatabaseKey(),
                    groupName = classDeclaration.getGroupName(),
                    primaryKey = primaryKey,
                    iconFile = classDeclaration.getIconFile()
                )
            }
            .build()
    }

    private fun KSClassDeclaration.validateImplementations() {
        val hasTableSuperType = superTypes.any { superType ->
            superType.resolve().declaration.qualifiedName?.asString() == "org.jetbrains.exposed.sql.Table"
        }
        if (!hasTableSuperType) {
            val message = "Class ${simpleName.asString()} must inherit from Table."
            throw IllegalArgumentException(message)
        }
    }

    private fun KSClassDeclaration.getAllColumnSets(): List<ColumnSet> {
        val emptyColumnName = EmptyColumn::class.qualifiedName
        val columns = mutableListOf<ColumnSet>()
        declarations.filterIsInstance<KSPropertyDeclaration>().forEach { property ->
            val type = property.type.resolve()
            val typeName = type.toClassName().canonicalName
            if (typeName == COLUMN_TYPE || typeName == emptyColumnName) {
                PropertiesRepository.getColumnSetsForExposed(property, type, isEmpty = typeName == emptyColumnName)
                    ?.let {
                        columns += it
                    }
            }
        }
        return columns
    }

    private fun KSClassDeclaration.getTableName() = getAnnotationArguments()
        ?.find { it.name?.asString() == "tableName" }
        ?.value as? String ?: ""


    private fun KSClassDeclaration.getIconFile() = (getAnnotationArguments()
        ?.find { it.name?.asString() == "iconFile" }
        ?.value as? String)?.takeIf { it.isNotEmpty() }

    private fun KSClassDeclaration.getPrimaryKey() = getAnnotationArguments()
        ?.find { it.name?.asString() == "primaryKey" }
        ?.value as? String ?: ""

    private fun KSClassDeclaration.getGroupName() = (getAnnotationArguments()
        ?.find { it.name?.asString() == "groupName" }
        ?.value as? String)?.takeIf { it.isNotEmpty() }

    private fun KSClassDeclaration.getDatabaseKey() = (getAnnotationArguments()
        ?.find { it.name?.asString() == "databaseKey" }
        ?.value as? String)?.takeIf { it.isNotEmpty() }

    private fun KSClassDeclaration.getPluralName(tableName: String) = (getAnnotationArguments()
        ?.find { it.name?.asString() == "pluralName" }
        ?.value as? String)?.takeIf { it.isNotEmpty() } ?: (tableName + "s")

    private fun KSClassDeclaration.getSingularName(tableName: String) = (getAnnotationArguments()
        ?.find { it.name?.asString() == "singularName" }
        ?.value as? String)?.takeIf { it.isNotEmpty() } ?: (tableName + "s")

    private fun KSClassDeclaration.getAnnotationArguments() = annotations
        .find { it.shortName.asString() == ExposedTable::class.simpleName }
        ?.arguments

    companion object {
        private const val COLUMN_TYPE = "org.jetbrains.exposed.sql.Column"
    }
}