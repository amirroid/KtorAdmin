package panels

interface AdminPanel {
    fun getSingularName(): String
    fun getPluralName(): String
    fun getGroupName(): String?
    fun getDatabaseKey(): String?
    fun getPrimaryKey(): String
    fun getDisplayFormat(): String?
    fun getSearchColumns(): List<String>
    fun getFilterColumns(): List<String>
}

fun List<AdminPanel>.findWithPluralName(name: String?) = find { it.getPluralName() == name }