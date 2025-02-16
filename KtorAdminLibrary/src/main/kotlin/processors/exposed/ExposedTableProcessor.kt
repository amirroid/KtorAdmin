package processors.exposed

import annotations.actions.AdminActions
import annotations.display.DisplayFormat
import annotations.exposed.ExposedTable
import annotations.order.DefaultOrder
import annotations.query.AdminQueries
import annotations.roles.AccessRoles
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import formatters.extractTextInCurlyBraces
import models.ColumnSet
import models.Limit
import models.UploadTarget
import models.actions.Action
import models.chart.AdminChartStyle
import models.common.Reference
import models.date.AutoNowDate
import models.order.Order
import models.order.toFormattedString
import models.types.ColumnType
import repository.PropertiesRepository
import utils.Constants
import utils.FileUtils
import utils.PackagesUtils
import utils.toSuitableStringForFile

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
        val columnSet = PackagesUtils.getColumnSetClass()

        val primaryKey = classDeclaration.getPrimaryKey()
        val displayFormat = classDeclaration.getDisplayFormat()

        if (!columnSets.any { it.columnName == primaryKey }) {
            throw IllegalArgumentException("(${classDeclaration.simpleName.asString()}) The provided primary key does not match any column in the table.")
        }
        if (displayFormat != null && displayFormat.extractTextInCurlyBraces()
                .any { it.split(".").firstOrNull() !in columnSets.map { sets -> sets.columnName } }
        ) {
            throw IllegalArgumentException("(${classDeclaration.simpleName.asString()}) The provided primary key does not match any column in the table.")
        }


        val columnNames = columnSets.map { it.columnName }
        val tableName = classDeclaration.getTableName()

        val getAllColumnsFunction = FunSpec.builder("getAllColumns")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(columnSet)
            )
            .addStatement("return listOf(${columnSets.joinToString { it.toSuitableStringForFile() }})")
            .build()


        val order = classDeclaration.getDefaultOrderFormat()

        if (order != null) {
            if (order.name !in columnSets.map { it.columnName }) {
                throw IllegalArgumentException("The field name '${order.name}' specified in the @DefaultOrder annotation does not exist in the column set.")
            }
            if (order.direction.lowercase() !in listOf("asc", "desc")) {
                throw IllegalArgumentException("The order direction '${order.direction}' specified in the @DefaultOrder annotation must be either 'asc' or 'desc'.")
            }
        }

        val adminActions = classDeclaration.getActionsArguments()
        val defaultActions =
            adminActions?.findActionList("actions") ?: Action.entries.map { "${Action::class.simpleName}.${it.name}" }
        val customActions = adminActions?.findStringList("customActions") ?: emptyList()


        val getDefaultActionsFunction = FunSpec.builder("getDefaultActions")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(Action::class.asClassName())
            )
            .addStatement(
                "return listOf(${defaultActions.joinToString()})"
            )
            .build()

        val getCustomActionsFunction = FunSpec.builder("getCustomActions")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(String::class.asClassName())
            )
            .addStatement("return listOf(${customActions.joinToString { "\"$it\"" }})")
            .build()

        val queryArguments = classDeclaration.getQueryColumnsArguments()
        val searchColumns = queryArguments?.findStringList("searches") ?: emptyList()
        val filterColumns = queryArguments?.findStringList("filters") ?: emptyList()
        val getSearchColumnsFunction = FunSpec.builder("getSearches")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(String::class.asClassName())
            )
            .addStatement("return listOf(${searchColumns.joinToString { "\"$it\"" }})")
            .build()

        val getFilterColumnsFunction = FunSpec.builder("getFilters")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(String::class.asClassName())
            )
            .addStatement("return listOf(${filterColumns.joinToString { "\"$it\"" }})")
            .build()

        val getTableNameFunction = FunSpec.builder("getTableName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", tableName)
            .build()

        val getPluralNameFunction = FunSpec.builder("getPluralName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getPluralName(tableName))
            .build()
        val getSingularNameFunction = FunSpec.builder("getSingularName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getSingularName(tableName))
            .build()
        val getGroupNameFunction = FunSpec.builder("getGroupName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class.asTypeName().copy(nullable = true))
            .addStatement("return ${classDeclaration.getGroupName()?.let { "\"$it\"" }}")
            .build()
        val getDisplayFormatFunction = FunSpec.builder("getDisplayFormat")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class.asTypeName().copy(nullable = true))
            .addStatement("return ${displayFormat?.let { "\"\"\"$it\"\"\"" }}")
            .build()
        val getDatabaseKeyFunction = FunSpec.builder("getDatabaseKey")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class.asTypeName().copy(nullable = true))
            .addStatement("return ${classDeclaration.getDatabaseKey()}")
            .build()
        val getPrimaryKeyFunctions = FunSpec.builder("getPrimaryKey")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", primaryKey)
            .build()
        val getDefaultOrderFunction = FunSpec.builder("getDefaultOrder")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Order::class.asTypeName().copy(nullable = true))
            .addStatement("return ${order?.toFormattedString()}")
            .build()
        val accessRoles = classDeclaration.getAccessRoles()
        val getAccessRolesFunction = FunSpec.builder("getAccessRoles")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(String::class.asClassName()).copy(nullable = true)
            )
            .addStatement("return ${accessRoles?.let { roles -> "listOf(${roles.joinToString { "\"$it\"" }})" }}")
            .build()
        val getIconFileFunction = FunSpec.builder("getIconFile")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                String::class.asClassName().copy(nullable = true)
            )
            .addStatement("return ${classDeclaration.getIconFile()?.let { "\"$it\"" }}")
            .build()

        return TypeSpec.classBuilder(fileName)
            .addSuperinterfaces(listOf(adminTable))
            .addFunction(getAllColumnsFunction)
            .addFunction(getAccessRolesFunction)
            .addFunction(getFilterColumnsFunction)
            .addFunction(getSearchColumnsFunction)
            .addFunction(getPrimaryKeyFunctions)
            .addFunction(getTableNameFunction)
            .addFunction(getSingularNameFunction)
            .addFunction(getPluralNameFunction)
            .addFunction(getGroupNameFunction)
            .addFunction(getDefaultOrderFunction)
            .addFunction(getCustomActionsFunction)
            .addFunction(getDefaultActionsFunction)
            .addFunction(getDisplayFormatFunction)
            .addFunction(getDatabaseKeyFunction)
            .addFunction(getIconFileFunction)
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
        val columns = mutableListOf<ColumnSet>()
        declarations.filterIsInstance<KSPropertyDeclaration>().forEach { property ->
            val type = property.type.resolve()
            if (type.toClassName().canonicalName == COLUMN_TYPE) {
                PropertiesRepository.getColumnSets(property, type)?.let {
                    columns += it
                }
            }
        }
        return columns
    }

    private fun KSClassDeclaration.getDisplayFormat() = annotations
        .find { it.shortName.asString() == DisplayFormat::class.simpleName }
        ?.arguments
        ?.find { it.name?.asString() == "format" }
        ?.value as? String


    private fun KSClassDeclaration.getDefaultOrderFormat() = annotations
        .find { it.shortName.asString() == DefaultOrder::class.simpleName }
        ?.arguments
        ?.let {
            Order(
                name = it.find { ksValueArgument -> ksValueArgument.name?.asString() == "name" }!!.value as String,
                direction = it.find { ksValueArgument -> ksValueArgument.name?.asString() == "direction" }!!.value as String,
            )
        }


    private fun KSClassDeclaration.getAccessRoles(): List<String>? {
        return annotations.find { it.shortName.asString() == AccessRoles::class.simpleName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "role" }
            ?.value
            ?.let { it as? List<*> }
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun KSClassDeclaration.getQueryColumnsArguments() = annotations
        .find { it.shortName.asString() == AdminQueries::class.simpleName }
        ?.arguments


    private fun KSClassDeclaration.getActionsArguments() = annotations
        .find { it.shortName.asString() == AdminActions::class.simpleName }
        ?.arguments


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

    private fun List<KSValueArgument>.findStringList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.filterIsInstance<String>()

    private fun List<KSValueArgument>.findActionList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.mapNotNull {
            it?.toString()
        }

    companion object {
        private const val COLUMN_TYPE = "org.jetbrains.exposed.sql.Column"
    }
}