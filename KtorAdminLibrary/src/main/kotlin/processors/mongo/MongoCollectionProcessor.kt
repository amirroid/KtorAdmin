package processors.mongo

import annotations.actions.AdminActions
import annotations.display.DisplayFormat
import annotations.mongo.MongoCollection
import annotations.order.DefaultOrder
import annotations.query.AdminQueries
import annotations.roles.AccessRoles
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import formatters.extractTextInCurlyBraces
import models.Limit
import models.UploadTarget
import models.actions.Action
import models.common.Reference
import models.date.AutoNowDate
import models.field.FieldSet
import models.order.Order
import models.order.toFormattedString
import models.types.FieldType
import repository.PropertiesRepository
import utils.FileUtils
import utils.PackagesUtils
import utils.guessFieldPropertyType
import utils.toSuitableStringForFile

class MongoCollectionProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(MongoCollection::class.qualifiedName ?: return emptyList())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach(::generateDesiredClass)
        return emptyList()
    }

    private fun generateDesiredClass(classDeclaration: KSClassDeclaration) {
        val visitedClasses = mutableSetOf<String>()
        val containingFile = classDeclaration.containingFile ?: return
        val fieldSets = collectPropertiesRecursively(classDeclaration, visitedClasses = visitedClasses)
        val packageName = classDeclaration.packageName.asString()
        val simpleFileName = classDeclaration.simpleName.asString()
        val fileName = FileUtils.getGeneratedFileName(simpleFileName)
        val generatedClass = generateClass(classDeclaration, fileName, fieldSets = fieldSets)
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addImport(FieldType::class.java.packageName, FieldType::class.java.simpleName)
            .addImport(UploadTarget::class.java.packageName, UploadTarget::class.java.simpleName)
            .addImport(Limit::class.java.packageName, Limit::class.java.simpleName)
            .addImport(Reference::class.java.packageName, Reference::class.java.simpleName)
            .addImport(AutoNowDate::class.java.packageName, AutoNowDate::class.java.simpleName)
            .addImport(Action::class.java.packageName, Action::class.java.simpleName)
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



    private fun KSClassDeclaration.getActionsArguments() = annotations
        .find { it.shortName.asString() == AdminActions::class.simpleName }
        ?.arguments

    private fun collectPropertiesRecursively(
        classDeclaration: KSClassDeclaration,
        visitedClasses: MutableSet<String>
    ): List<FieldSet> {
        val qualifiedName = classDeclaration.qualifiedName?.asString() ?: return emptyList()
        if (visitedClasses.contains(qualifiedName)) return emptyList()
        val properties = mutableListOf<FieldSet>()
        visitedClasses.add(qualifiedName)
        classDeclaration.getDeclaredProperties().forEach { property ->
            val resolvedType = property.type.resolve()
            val propertyType = resolvedType.declaration.qualifiedName?.asString() ?: property.simpleName.asString()
            when (val type = guessFieldPropertyType(propertyType)) {
                FieldType.NotAvailable -> {
                    val nestedClassDeclaration = resolvedType.declaration as KSClassDeclaration
                    collectPropertiesRecursively(
                        nestedClassDeclaration,
                        visitedClasses
                    ).also {
                        val fieldType = FieldType.Map(it)
                        PropertiesRepository.getFieldSet(property, fieldType)?.let { fieldSet ->
                            properties.add(fieldSet)
                        }
                    }
                }

                is FieldType.List -> {
                    val listItemType = resolvedType.arguments.firstOrNull()?.type?.resolve()
                    if (listItemType != null) {
                        val listItemTypeDeclaration = listItemType.declaration
                        if (listItemTypeDeclaration is KSClassDeclaration) {
                            val listFieldType = guessFieldPropertyType(
                                listItemTypeDeclaration.qualifiedName?.asString() ?: return@forEach
                            )
                            if (listFieldType == FieldType.NotAvailable) {
                                collectPropertiesRecursively(
                                    listItemTypeDeclaration, visitedClasses
                                ).also { fields ->
                                    val fieldType = FieldType.List(fields)
                                    PropertiesRepository.getFieldSet(property, fieldType)?.let { fieldSet ->
                                        properties.add(fieldSet)
                                    }
                                }
                            } else {
                                val fieldListSet = FieldSet(
                                    fieldName = null,
                                    type = listFieldType
                                )
                                PropertiesRepository.getFieldSet(property, FieldType.List(listOf(fieldListSet)))
                                    ?.let { fieldSet ->
                                        properties.add(fieldSet)
                                    }
                            }
                        }
                    }
                }

                is FieldType.Map -> {
                    val mapItemType = resolvedType.arguments.getOrNull(1)?.type?.resolve()
                    if (mapItemType != null) {
                        val mapItemTypeDeclaration = mapItemType.declaration
                        if (mapItemTypeDeclaration is KSClassDeclaration) {
                            val mapFieldType = guessFieldPropertyType(
                                mapItemTypeDeclaration.qualifiedName?.asString() ?: return@forEach
                            )
                            if (mapFieldType == FieldType.NotAvailable) {
                                collectPropertiesRecursively(
                                    mapItemTypeDeclaration, visitedClasses
                                ).also { fields ->
                                    val fieldType = FieldType.Map(fields)
                                    PropertiesRepository.getFieldSet(property, fieldType)?.let { fieldSet ->
                                        properties.add(fieldSet)
                                    }
                                }
                            } else {
                                val fieldListSet = FieldSet(
                                    fieldName = null,
                                    type = mapFieldType
                                )
                                PropertiesRepository.getFieldSet(property, FieldType.Map(listOf(fieldListSet)))
                                    ?.let { fieldSet ->
                                        properties.add(fieldSet)
                                    }
                            }
                        }
                    }
                }

                else -> {
                    PropertiesRepository.getFieldSet(property, type)?.let { fieldSet ->
                        properties.add(fieldSet)
                    }
                }
            }
        }
        return properties
    }

    private fun generateClass(
        classDeclaration: KSClassDeclaration,
        fileName: String,
        fieldSets: List<FieldSet>
    ): TypeSpec {
        val adminMongoCollection = PackagesUtils.getAdminMongoCollectionClass()
        val fieldSet = PackagesUtils.getFieldSetClass()
        val primaryKey = classDeclaration.getPrimaryKey()
        val displayFormat = classDeclaration.getDisplayFormat()
        if (!fieldSets.any { it.fieldName == primaryKey }) {
            throw IllegalArgumentException("(${classDeclaration.simpleName.asString()}) The provided primary key does not match any field in the collection.")
        }
        if (displayFormat != null && displayFormat.extractTextInCurlyBraces()
                .any { it.split(".").firstOrNull() !in fieldSets.map { sets -> sets.fieldName } }
        ) {
            throw IllegalArgumentException("(${classDeclaration.simpleName.asString()}) The provided primary key does not match any field in the cllection.")
        }

        val getAllFieldsFunction = FunSpec.builder("getAllFields")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(fieldSet)
            )
            .addStatement("return listOf(${fieldSets.joinToString { it.toSuitableStringForFile() }})")
            .build()


        val collectionName = classDeclaration.getCollectionName()


        val order = classDeclaration.getDefaultOrderFormat()

        if (order != null) {
            if (order.name !in fieldSets.map { it.fieldName }) {
                throw IllegalArgumentException("The field name '${order.name}' specified in the @DefaultOrder annotation does not exist in the column set.")
            }
            if (order.direction.lowercase() !in listOf("asc", "desc")) {
                throw IllegalArgumentException("The order direction '${order.direction}' specified in the @DefaultOrder annotation must be either 'asc' or 'desc'.")
            }
        }


        val getCollectionNameFunction = FunSpec.builder("getCollectionName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", collectionName)
            .build()



        val adminActions = classDeclaration.getActionsArguments()
        val defaultActions = adminActions?.findActionList("actions") ?: listOf(Action.ADD, Action.DELETE, Action.EDIT)
        val customActions = adminActions?.findStringList("customActions") ?: emptyList()


        val getDefaultActionsFunction = FunSpec.builder("getDefaultActions")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(Action::class.asClassName())
            )
            .addStatement(
                "return listOf(${defaultActions.joinToString { "${Action::class.simpleName}.$it" }})"
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

        val getPluralNameFunction = FunSpec.builder("getPluralName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getPluralName(collectionName))
            .build()
        val getSingularNameFunction = FunSpec.builder("getSingularName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getSingularName(collectionName))
            .build()
        val getGroupNameFunction = FunSpec.builder("getGroupName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class.asTypeName().copy(nullable = true))
            .addStatement("return ${classDeclaration.getGroupName()?.let { "\"$it\"" }}")
            .build()
        val getDisplayFormatFunction = FunSpec.builder("getDisplayFormat")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class.asTypeName().copy(nullable = true))
            .addStatement("return ${displayFormat?.let { "\"$it\"" }}")
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

        return TypeSpec.classBuilder(fileName)
            .addSuperinterfaces(listOf(adminMongoCollection))
            .addFunction(getAllFieldsFunction)
            .addFunction(getCollectionNameFunction)
            .addFunction(getPrimaryKeyFunctions)
            .addFunction(getSingularNameFunction)
            .addFunction(getPluralNameFunction)
            .addFunction(getGroupNameFunction)
            .addFunction(getDefaultOrderFunction)
            .addFunction(getCustomActionsFunction)
            .addFunction(getDefaultActionsFunction)
            .addFunction(getFilterColumnsFunction)
            .addFunction(getSearchColumnsFunction)
            .addFunction(getDisplayFormatFunction)
            .addFunction(getDatabaseKeyFunction)
            .addFunction(getAccessRolesFunction)
            .build()
    }



    private fun List<KSValueArgument>.findActionList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.mapNotNull {
            (it as? KSName)?.getEnumValue<Action>()
        }

    private inline fun <reified T : Enum<T>> KSName.getEnumValue(): T? {
        return enumValues<T>().firstOrNull { it.name == this.asString() }
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

    private fun KSClassDeclaration.getDisplayFormat() = annotations
        .find { it.shortName.asString() == DisplayFormat::class.simpleName }
        ?.arguments
        ?.find { it.name?.asString() == "format" }
        ?.value as? String


    private fun KSClassDeclaration.getQueryColumnsArguments() = annotations
        .find { it.shortName.asString() == AdminQueries::class.simpleName }
        ?.arguments


    private fun KSClassDeclaration.getDefaultOrderFormat() = annotations
        .find { it.shortName.asString() == DefaultOrder::class.simpleName }
        ?.arguments
        ?.let {
            Order(
                name = it.find { ksValueArgument -> ksValueArgument.name?.asString() == "name" }!!.value as String,
                direction = it.find { ksValueArgument -> ksValueArgument.name?.asString() == "direction" }!!.value as String,
            )
        }

    private fun KSClassDeclaration.getCollectionName() = getAnnotationArguments()
        ?.find { it.name?.asString() == "collectionName" }
        ?.value as? String ?: ""

    private fun KSClassDeclaration.getPrimaryKey() = getAnnotationArguments()
        ?.find { it.name?.asString() == "primaryKey" }
        ?.value as? String ?: ""

    private fun KSClassDeclaration.getGroupName() = (getAnnotationArguments()
        ?.find { it.name?.asString() == "groupName" }
        ?.value as? String)?.takeIf { it.isNotEmpty() }

    private fun KSClassDeclaration.getDatabaseKey() = (getAnnotationArguments()
        ?.find { it.name?.asString() == "databaseKey" }
        ?.value as? String)?.takeIf { it.isNotEmpty() }

    private fun KSClassDeclaration.getPluralName(collectionName: String) = (getAnnotationArguments()
        ?.find { it.name?.asString() == "pluralName" }
        ?.value as? String)?.takeIf { it.isNotEmpty() } ?: (collectionName + "s")

    private fun KSClassDeclaration.getSingularName(collectionName: String) = (getAnnotationArguments()
        ?.find { it.name?.asString() == "singularName" }
        ?.value as? String)?.takeIf { it.isNotEmpty() } ?: (collectionName + "s")

    private fun KSClassDeclaration.getAnnotationArguments() = annotations
        .find { it.shortName.asString() == MongoCollection::class.simpleName }
        ?.arguments

    private fun List<KSValueArgument>.findStringList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.filterIsInstance<String>()
}
