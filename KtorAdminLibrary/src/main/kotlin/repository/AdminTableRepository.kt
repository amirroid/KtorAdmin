package repository

import org.reflections.Reflections
import utils.AdminTable
import utils.Constants

internal object AdminTableRepository {
    private const val PACKAGE_NAME = Constants.PACKAGE_NAME

    fun getAll(): List<AdminTable> {
        val tables = mutableListOf<AdminTable>()
        val reflections = Reflections(PACKAGE_NAME)
        val subClasses = reflections.getSubTypesOf(AdminTable::class.java)
        subClasses.forEach {
            kotlin.runCatching {
                it.getDeclaredConstructor().newInstance() as AdminTable
            }.getOrNull()?.let {
                tables += it
            }
        }
        return tables.toList()
    }
}