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
    getAllFields().filter { it.showInPanel && it.fieldName != getPrimaryKey() && it.autoNowDate == null }


fun AdminMongoCollection.getAllAutoNowDateInsertFields() =
    getAllFields().filter { it.autoNowDate != null }

fun AdminMongoCollection.getAllAutoNowDateUpdateFields() =
    getAllFields().filter { it.autoNowDate != null && it.autoNowDate.updateOnChange }