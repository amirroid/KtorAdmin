package repository

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import panels.AdminPanel
import utils.Constants

internal object AdminTableRepository {
    fun getAll(): List<AdminPanel> {
        val tables = mutableListOf<AdminPanel>()
        val reflections = Reflections("", SubTypesScanner(false))
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