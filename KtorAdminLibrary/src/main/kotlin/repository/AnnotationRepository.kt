package repository

import annotations.chart.DashboardChartConfig
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import models.chart.AdminChartStyle
import models.chart.ChartConfig

internal object AnnotationRepository {
    fun findChartConfigs(classDeclaration: KSClassDeclaration): List<ChartConfig> {
        return classDeclaration.annotations.filter {
            it.shortName.asString() == DashboardChartConfig::class.simpleName
        }.map { annotation ->
            ChartConfig(
                sectionName = annotation.arguments.findString("sectionName")!!,
                labelField = annotation.arguments.findString("labelField")!!,
                valuesFields = annotation.arguments.findStringList("valuesFields")!!,
                chartStyle = annotation.arguments.findEnum<AdminChartStyle>("chartStyle")!!,
                borderColors = annotation.arguments.findStringList("borderColors") ?: emptyList(),
                fillColors = annotation.arguments.findStringList("fillColors") ?: emptyList(),
                orderQuery = annotation.arguments.findString("orderQuery")?.takeIf { it.isNotEmpty() },
                limitCount = annotation.arguments.find("limitCount")?.toString()?.toInt()
                    ?.takeIf { it != Int.MAX_VALUE },
                borderWidth = annotation.arguments.find("borderWidth")!!.toString().toFloat(),
                tension = annotation.arguments.find("tension")!!.toString().toFloat(),
            )
        }.toList()
    }

    fun List<KSValueArgument>.findString(name: String) = find { it.name?.asString() == name }?.value as? String
    fun List<KSValueArgument>.find(name: String) = find { it.name?.asString() == name }?.value
    private fun List<KSValueArgument>.findStringList(name: String) = firstOrNull { it.name?.asString() == name }
        ?.value
        ?.let { it as? List<*> }
        ?.filterIsInstance<String>()

    private inline fun <reified T : Enum<T>> List<KSValueArgument>.findEnum(name: String): T? {
        return firstOrNull { it.name?.asString() == name }
            ?.value
            ?.toString()
            ?.split(".")
            ?.lastOrNull()
            ?.let { enumString ->
                enumValues<T>().firstOrNull { it.name == enumString }
            }
    }
}