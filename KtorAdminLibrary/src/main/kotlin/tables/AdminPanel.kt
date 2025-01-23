package tables

interface AdminPanel {
    fun getSingularName(): String
    fun getPluralName(): String
    fun getGroupName(): String?
}

fun List<AdminPanel>.findWithPluralName(name: String?) = find { it.getPluralName() == name }