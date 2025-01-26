package panels

import models.field.FieldSet
import models.types.FieldType

interface AdminMongoCollection : AdminPanel {
    fun getAllFields(): List<FieldSet>
    fun getCollectionName(): String
}

fun AdminMongoCollection.getAllAllowToShowFields() =
    getAllFields().filter { it.showInPanel }

fun AdminMongoCollection.getPrimaryKeyField() =
    getAllFields().first { it.fieldName == getPrimaryKey() }

fun AdminMongoCollection.getAllAllowToShowFieldsInUpsert() =
    getAllFields().filter { it.showInPanel && it.fieldName != getPrimaryKey() }