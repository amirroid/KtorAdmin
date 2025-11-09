package ir.amirroid.ktoradmin.filters

import com.mongodb.client.model.Filters
import ir.amirroid.ktoradmin.configuration.DynamicConfiguration
import ir.amirroid.ktoradmin.getters.toDate
import io.ktor.http.*
import ir.amirroid.ktoradmin.models.field.FieldSet
import ir.amirroid.ktoradmin.models.filters.FilterTypes
import ir.amirroid.ktoradmin.models.filters.FiltersData
import ir.amirroid.ktoradmin.models.types.FieldType
import ir.amirroid.ktoradmin.panels.AdminMongoCollection
import ir.amirroid.ktoradmin.utils.Constants
import org.bson.conversions.Bson
import java.time.Instant
import kotlin.collections.forEach

internal object MongoFilters {
    fun findFiltersData(
        panel: AdminMongoCollection,
    ): List<FiltersData> {
        return panel.getFilters().map { filterField ->
            val field = panel.getAllFields().find { it.fieldName == filterField }

            when (field?.type) {
                FieldType.Date -> FiltersData(
                    paramName = field.fieldName.toString(),
                    verboseName = field.verboseName,
                    type = FilterTypes.DATE
                )

                FieldType.DateTime, FieldType.Instant -> FiltersData(
                    paramName = field.fieldName.toString(),
                    verboseName = field.verboseName,
                    type = FilterTypes.DATETIME
                )

                FieldType.Enumeration -> FiltersData(
                    paramName = field.fieldName.toString(),
                    type = FilterTypes.ENUMERATION,
                    verboseName = field.verboseName,
                    values = field.enumerationValues
                )

                FieldType.Boolean -> FiltersData(
                    paramName = field.fieldName.toString(),
                    verboseName = field.verboseName,
                    type = FilterTypes.BOOLEAN,
                )

                else -> throw IllegalArgumentException("Filters are currently supported only for types: DATE, DATETIME, ENUMERATION, and REFERENCE")
            }
        }
    }

    fun extractMongoFilters(
        panel: AdminMongoCollection,
        parameters: Parameters
    ): Bson {
        val fieldSets = panel.getAllFields()
        val filters = mutableListOf<Bson>()

        fieldSets.forEach { fieldSet ->
            if (!fieldSet.showInPanel) return@forEach

            when (fieldSet.type) {
                FieldType.Date, FieldType.DateTime, FieldType.Instant -> {
                    handleMongoDateTimeFilter(fieldSet, parameters, filters)
                }

                FieldType.Enumeration -> {
                    parameters[Constants.FILTERS_PREFIX + fieldSet.fieldName.toString()]?.let { value ->
                        if (fieldSet.enumerationValues?.contains(value) == true) {
                            filters.add(Filters.eq(fieldSet.fieldName.toString(), value))
                        }
                    }
                }

                FieldType.Boolean -> {
                    parameters[Constants.FILTERS_PREFIX + fieldSet.fieldName.toString()]?.let { value ->
                        filters.add(Filters.eq(fieldSet.fieldName.toString(), value == Constants.TRUE_FORM))
                    }
                }

                else -> {
                    parameters[Constants.FILTERS_PREFIX + fieldSet.fieldName.toString()]?.let { value ->
                        filters.add(Filters.eq(fieldSet.fieldName.toString(), value))
                    }
                }
            }
        }

        return if (filters.isEmpty()) Filters.empty() else Filters.and(filters)
    }

    private fun handleMongoDateTimeFilter(
        fieldSet: FieldSet,
        parameters: Parameters,
        filters: MutableList<Bson>
    ) {
        val startParamName = "${fieldSet.fieldName}-start"
        val endParamName = "${fieldSet.fieldName}-end"

        if (parameters.contains(Constants.FILTERS_PREFIX + startParamName)) {
            parameters[Constants.FILTERS_PREFIX + startParamName]?.let { startValue ->
                val start = Instant.ofEpochMilli(startValue.toLong())
                    .atZone(DynamicConfiguration.timeZone)
                    .toLocalDateTime()
                    .toDate()
                filters.add(Filters.gte(fieldSet.fieldName.toString(), start))
            }
        }

        if (parameters.contains(Constants.FILTERS_PREFIX + endParamName)) {
            parameters[Constants.FILTERS_PREFIX + endParamName]?.let { endValue ->
                val end = Instant.ofEpochMilli(endValue.toLong())
                    .atZone(DynamicConfiguration.timeZone)
                    .toLocalDateTime()
                    .toDate()
                filters.add(Filters.lte(fieldSet.fieldName.toString(), end))
            }
        }
    }
}