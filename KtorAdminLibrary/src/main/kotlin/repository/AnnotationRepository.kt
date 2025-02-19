package repository

import annotations.actions.AdminActions
import annotations.display.DisplayFormat
import annotations.display.PanelDisplayList
import annotations.order.DefaultOrder
import annotations.query.AdminQueries
import annotations.roles.AccessRoles
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import formatters.extractTextInCurlyBraces
import models.ColumnSet
import models.actions.Action
import models.order.Order
import models.order.toFormattedString
import utils.PackagesUtils
import utils.toSuitableStringForFile

internal object AnnotationRepository {
    private const val INVALID_ORDER_DIRECTION_MESSAGE = "The order direction '%s' must be either 'asc' or 'desc'."
    private const val INVALID_COLUMN_MESSAGE =
        "The field name '%s' specified in the @%s annotation does not exist in the column set."
    private val VALID_ORDER_DIRECTIONS = setOf("asc", "desc")

    internal fun addCommonFunctionsToClass(
        typedSpec: TypeSpec.Builder,
        classDeclaration: KSClassDeclaration,
        columnSets: List<ColumnSet>,
        singularName: String,
        pluralName: String,
        tableName: String,
        databaseKey: String?,
        groupName: String?,
        primaryKey: String,
        iconFile: String?,
        isShowInAdminPanel: Boolean,
    ): TypeSpec.Builder {
        val columnNames = columnSets.map { it.columnName }

        return typedSpec.apply {
            addFunction(createSearchColumnsFunction(classDeclaration))
            addFunction(createFilterColumnsFunction(classDeclaration))
            addFunction(createDefaultOrderFunction(classDeclaration, columnNames))
            addFunction(createAccessRolesFunction(classDeclaration))
            addFunction(createAllColumnsFunction(columnSets))
            addFunction(createCustomActionsFunction(classDeclaration))
            addFunction(createDefaultActionsFunction(classDeclaration))
            addFunction(createDisplayListFunction(classDeclaration, columnNames))
            addFunction(createDisplayFormatFunction(classDeclaration, columnNames))
            addFunction(createIsShowInAdminPanelFunction(isShowInAdminPanel))
            addFunction(createBasicGetterFunction("getPrimaryKey", primaryKey))
            addFunction(createBasicGetterFunction("getIconFile", iconFile, nullable = true))
            addFunction(createBasicGetterFunction("getDatabaseKey", databaseKey, nullable = true))
            addFunction(createBasicGetterFunction("getTableName", tableName))
            addFunction(createBasicGetterFunction("getGroupName", groupName, nullable = true))
            addFunction(createBasicGetterFunction("getPluralName", pluralName))
            addFunction(createBasicGetterFunction("getSingularName", singularName))
        }
    }

    private fun createBasicGetterFunction(
        functionName: String,
        value: String?,
        nullable: Boolean = false
    ): FunSpec {
        val returnType = String::class.asClassName().let { if (nullable) it.copy(nullable = true) else it }
        return FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .returns(returnType)
            .addStatement("return ${value?.let { "\"$it\"" } ?: "null"}")
            .build()
    }

    private fun createDisplayFormatFunction(
        classDeclaration: KSClassDeclaration,
        columnNames: List<String>
    ): FunSpec {
        val displayFormat = getDisplayFormat(classDeclaration)
        validateDisplayFormat(displayFormat, columnNames, classDeclaration.simpleName.asString())

        return FunSpec.builder("getDisplayFormat")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class.asTypeName().copy(nullable = true))
            .addStatement("return ${displayFormat?.let { "\"\"\"$it\"\"\"" }}")
            .build()
    }

    private fun createDisplayListFunction(
        classDeclaration: KSClassDeclaration,
        columnNames: List<String>
    ): FunSpec {
        val displayList = getDisplayList(classDeclaration) ?: columnNames
        if (displayList.any { it !in columnNames }) {
            throw IllegalArgumentException("Display list contains invalid column names: ${displayList.filter { it !in columnNames }}")
        }

        return FunSpec.builder("getPanelListColumns")
            .addModifiers(KModifier.OVERRIDE)
            .returns(List::class.parameterizedBy(String::class))
            .addStatement("return ${displayList.toSuitableStringForFile()}")
            .build()
    }

    private fun createDefaultOrderFunction(
        classDeclaration: KSClassDeclaration,
        columnNames: List<String>
    ): FunSpec {
        val order = getDefaultOrderFormat(classDeclaration)
        validateOrder(order, columnNames)

        return FunSpec.builder("getDefaultOrder")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Order::class.asTypeName().copy(nullable = true))
            .addStatement("return ${order?.toFormattedString()}")
            .build()
    }

    private fun createAllColumnsFunction(columnSets: List<ColumnSet>): FunSpec {
        val columnSetType = PackagesUtils.getColumnSetClass()
        return FunSpec.builder("getAllColumns")
            .addModifiers(KModifier.OVERRIDE)
            .returns(List::class.asClassName().parameterizedBy(columnSetType))
            .addStatement("return listOf(${columnSets.joinToString { it.toSuitableStringForFile() }})")
            .build()
    }

    private fun createSearchColumnsFunction(classDeclaration: KSClassDeclaration): FunSpec {
        val searchColumns = classDeclaration.getQueryColumnsArguments()
            ?.findStringList("searches")
            ?: emptyList()

        return createStringListFunction("getSearches", searchColumns)
    }

    private fun createFilterColumnsFunction(classDeclaration: KSClassDeclaration): FunSpec {
        val filterColumns = classDeclaration.getQueryColumnsArguments()
            ?.findStringList("filters")
            ?: emptyList()

        return createStringListFunction("getFilters", filterColumns)
    }

    private fun createAccessRolesFunction(classDeclaration: KSClassDeclaration): FunSpec {
        val accessRoles = getAccessRoles(classDeclaration)
        return FunSpec.builder("getAccessRoles")
            .addModifiers(KModifier.OVERRIDE)
            .returns(List::class.asClassName().parameterizedBy(String::class.asClassName()).copy(nullable = true))
            .addStatement("return ${accessRoles?.let { roles -> "listOf(${roles.joinToString { "\"$it\"" }})" }}")
            .build()
    }

    private fun createCustomActionsFunction(classDeclaration: KSClassDeclaration): FunSpec {
        val customActions = classDeclaration.getActionsArguments()
            ?.findStringList("customActions")
            ?: emptyList()

        return createStringListFunction("getCustomActions", customActions)
    }


    private fun createIsShowInAdminPanelFunction(isShowInAdminPanel: Boolean): FunSpec {
        return FunSpec.builder("isShowInAdminPanel")
            .addModifiers(KModifier.OVERRIDE)
            .returns(BOOLEAN)
            .addStatement("return $isShowInAdminPanel")
            .build()
    }

    private fun createDefaultActionsFunction(classDeclaration: KSClassDeclaration): FunSpec {
        val defaultActions = classDeclaration.getActionsArguments()
            ?.findList("actions")
            ?: Action.entries.map { "${Action::class.simpleName}.${it.name}" }

        return FunSpec.builder("getDefaultActions")
            .addModifiers(KModifier.OVERRIDE)
            .returns(List::class.asClassName().parameterizedBy(Action::class.asClassName()))
            .addStatement("return listOf(${defaultActions.joinToString()})")
            .build()
    }

    private fun createStringListFunction(name: String, items: List<String>): FunSpec {
        return FunSpec.builder(name)
            .addModifiers(KModifier.OVERRIDE)
            .returns(List::class.asClassName().parameterizedBy(String::class.asClassName()))
            .addStatement("return listOf(${items.joinToString { "\"$it\"" }})")
            .build()
    }

    private fun validateDisplayFormat(displayFormat: String?, columnNames: List<String>, className: String) {
        if (displayFormat != null) {
            val invalidColumns = displayFormat.extractTextInCurlyBraces()
                .map { it.split(".").first() }
                .filter { it !in columnNames }

            if (invalidColumns.isNotEmpty()) {
                throw IllegalArgumentException("($className) The following columns in display format do not exist: ${invalidColumns.joinToString()}")
            }
        }
    }

    private fun validateOrder(order: Order?, columnNames: List<String>) {
        if (order != null) {
            if (order.name !in columnNames) {
                throw IllegalArgumentException(
                    INVALID_COLUMN_MESSAGE.format(
                        order.name,
                        DefaultOrder::class.simpleName
                    )
                )
            }
            if (order.direction.lowercase() !in VALID_ORDER_DIRECTIONS) {
                throw IllegalArgumentException(INVALID_ORDER_DIRECTION_MESSAGE.format(order.direction))
            }
        }
    }

    private fun getDisplayFormat(classDeclaration: KSClassDeclaration) = classDeclaration.annotations
        .find { it.shortName.asString() == DisplayFormat::class.simpleName }
        ?.arguments
        ?.find { it.name?.asString() == "format" }
        ?.value as? String

    private fun getDisplayList(classDeclaration: KSClassDeclaration) = (classDeclaration.annotations
        .find { it.shortName.asString() == PanelDisplayList::class.simpleName }
        ?.arguments
        ?.find { it.name?.asString() == "field" }
        ?.value as? List<*>)?.filterIsInstance<String>()

    private fun getDefaultOrderFormat(classDeclaration: KSClassDeclaration) = classDeclaration.annotations
        .find { it.shortName.asString() == DefaultOrder::class.simpleName }
        ?.arguments
        ?.let {
            Order(
                name = it.find { arg -> arg.name?.asString() == "name" }!!.value as String,
                direction = it.find { arg -> arg.name?.asString() == "direction" }!!.value as String,
            )
        }

    private fun getAccessRoles(classDeclaration: KSClassDeclaration): List<String>? {
        return classDeclaration.annotations
            .find { it.shortName.asString() == AccessRoles::class.simpleName }
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

    private fun List<KSValueArgument>.findStringList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.filterIsInstance<String>()

    private fun List<KSValueArgument>.findList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.mapNotNull { it?.toString() }
}