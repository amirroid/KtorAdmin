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
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import models.ColumnSet
import models.Limit
import models.UploadTarget
import models.actions.Action
import models.chart.AdminChartStyle
import models.common.Reference
import models.date.AutoNowDate
import models.types.ColumnType
import processors.qualifiedName
import repository.AnnotationRepository
import repository.PropertiesRepository
import utils.Constants
import utils.FileUtils
import utils.PackagesUtils

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
        val generatedClass = generateClass(classDeclaration, fileName, columns.first, columns.second)
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
        columnSets: List<ColumnSet>,
        primaryKey: ColumnSet
    ): TypeSpec {
        val adminTable = PackagesUtils.getAdminTableClass()

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
                    primaryKey = primaryKey.columnName,
                    iconFile = classDeclaration.getIconFile()
                )
            }
            .build()
    }

    private fun KSClassDeclaration.getAllColumnSets(): Pair<List<ColumnSet>, ColumnSet> {
        val columns = mutableListOf<ColumnSet>()
        var primaryKey: ColumnSet? = null

        getDeclaredProperties().forEach { property ->
            val type = property.type.resolve()

            PropertiesRepository.getColumnSetsForHibernate(property, type)?.let { columnSet ->
                columns += columnSet
                val idAnnotationQualifiedNames = getListOfHibernatePackage(
                    HIBERNATE_ID_ANNOTATION_CLASSNAME
                )
                if (property.annotations.any { annotation -> annotation.qualifiedName.orEmpty() in idAnnotationQualifiedNames }) {
                    primaryKey = columnSet
                }
            }
        }

        return columns to (primaryKey ?: throw IllegalStateException("(${getTableName()}) No primary key found"))
    }

    private fun KSClassDeclaration.getTableName() = getAnnotationArguments()
        ?.find { it.name?.asString() == "tableName" }
        ?.value as? String ?: ""


    private fun KSClassDeclaration.getIconFile() = (getAnnotationArguments()
        ?.find { it.name?.asString() == "iconFile" }
        ?.value as? String)?.takeIf { it.isNotEmpty() }


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
        .find { it.shortName.asString() == HibernateTable::class.simpleName }
        ?.arguments

    companion object {
        internal const val HIBERNATE_ID_ANNOTATION_CLASSNAME = "Id"
        fun getListOfHibernatePackage(className: String): List<String> {
            return listOf(
                "jakarta.persistence.$className",
                "javax.persistence.$className",
            )
        }
    }
}