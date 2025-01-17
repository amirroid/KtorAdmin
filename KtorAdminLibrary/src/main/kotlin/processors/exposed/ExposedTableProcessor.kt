package processors.exposed

import annotations.exposed.ExposedTable
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
import models.ColumnSet
import models.ColumnType
import models.Limit
import models.UploadTarget
import utils.*

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
        val packageName = classDeclaration.packageName.asString()
        val simpleFileName = classDeclaration.simpleName.asString()
        val fileName = FileUtils.getGeneratedFileName(simpleFileName)
        val columns = classDeclaration.getAllColumnSets()
        val generatedClass = generateClass(classDeclaration, fileName, columns)
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addImport(ColumnType::class.java.packageName, ColumnType::class.java.simpleName)
            .addImport(UploadTarget::class.java.packageName, UploadTarget::class.java.simpleName)
            .addImport(Limit::class.java.packageName, Limit::class.java.simpleName)
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
        val getAllColumnsFunction = FunSpec.builder("getAllColumns")
            .addModifiers(KModifier.OVERRIDE)
            .returns(
                List::class.asClassName().parameterizedBy(columnSet)
            )
            .addStatement("return listOf(${columnSets.joinToString { it.toSuitableStringForFile() }})")
            .build()

        val getTableNameFunction = FunSpec.builder("getTableName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getTableName())
            .build()

        val getPluralNameFunction = FunSpec.builder("getPluralName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getPluralName())
            .build()
        val getGroupNameFunction = FunSpec.builder("getGroupName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", classDeclaration.getGroupName())
            .build()
        return TypeSpec.classBuilder(fileName)
            .addSuperinterfaces(listOf(adminTable))
            .addFunction(getAllColumnsFunction)
            .addFunction(getTableNameFunction)
            .addFunction(getPluralNameFunction)
            .addFunction(getGroupNameFunction)
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
                ColumnsUtils.getColumnSets(property, type)?.let {
                    columns += it
                }
            }
        }
        return columns
    }

    private fun KSClassDeclaration.findProperty(propertyName: String) =
        declarations.filterIsInstance<KSPropertyDeclaration>().find { it.simpleName.asString() == propertyName }

    private fun KSClassDeclaration.getTableName() = getAnnotationArguments()
        ?.find { it.name?.asString() == "tableName" }
        ?.value as? String ?: ""

    private fun KSClassDeclaration.getGroupName() = getAnnotationArguments()
        ?.find { it.name?.asString() == "groupName" }
        ?.value as? String ?: ""

    private fun KSClassDeclaration.getPluralName() = getAnnotationArguments()
        ?.find { it.name?.asString() == "pluralName" }
        ?.value as? String ?: ""

    private fun KSClassDeclaration.getAnnotationArguments() = annotations
        .find { it.shortName.asString() == ExposedTable::class.simpleName }
        ?.arguments

    companion object {
        private const val COLUMN_TYPE = "org.jetbrains.exposed.sql.Column"
    }
}