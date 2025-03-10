package repository

import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import panels.AdminPanel
import utils.Constants

internal object AdminTableRepository {
    fun getAll(): List<AdminPanel> {
        val tables = mutableListOf<AdminPanel>()
        val reflections = Reflections(ConfigurationBuilder().forPackages(Constants.PACKAGE_NAME))
        val subClasses = reflections.getSubTypesOf(AdminPanel::class.java)
        subClasses.forEach {
            kotlin.runCatching {
                it.getDeclaredConstructor().newInstance() as AdminPanel
            }.getOrNull()?.let {
                tables += it
            }
        }
        return tables.toList()
    }
}