package dashboard.list

import dashboard.base.DashboardSection

abstract class ListDashboardSection : DashboardSection {
    override val sectionType: String
        get() = SECTION_TYPE


    abstract val tableName: String

    open val fields: List<String>? = null

    open val limitCount: Int? = null

    open val orderQuery: String? = null


    companion object {
        private const val SECTION_TYPE = "list"
    }
}