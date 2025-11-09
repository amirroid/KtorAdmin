package ir.amirroid.ktoradmin.repository

import ir.amirroid.ktoradmin.panels.AdminPanel
import ir.amirroid.ktoradmin.utils.Constants
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder

internal object AdminTableRepository {
    fun getAll(): List<AdminPanel> {
        val tables = mutableListOf<AdminPanel>()
        val reflections = Reflections(ConfigurationBuilder().forPackages(Constants.PACKAGE_NAME))
        val subClasses = reflections.getSubTypesOf(AdminPanel::class.java)
        subClasses.forEach {
            runCatching {
                it.getDeclaredConstructor().newInstance() as AdminPanel
            }.getOrNull()?.let {
                tables += it
            }
        }
        return tables.toList()
    }
}