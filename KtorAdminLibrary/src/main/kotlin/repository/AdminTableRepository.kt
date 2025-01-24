package repository

import org.reflections.Reflections
import panels.AdminPanel
import utils.Constants

internal object AdminTableRepository {
    private const val PACKAGE_NAME = Constants.PACKAGE_NAME

    fun getAll(): List<AdminPanel> {
        val tables = mutableListOf<AdminPanel>()
        val reflections = Reflections(PACKAGE_NAME)
        val subClasses = reflections.getSubTypesOf(AdminPanel::class.java)
        subClasses.forEach {
            kotlin.runCatching {
                it.getDeclaredConstructor().newInstance() as AdminPanel
            }.getOrNull()?.let {
                tables += it
            }
        }
        println("TABLE: ${tables.count()}")
        return tables.toList()
    }
}