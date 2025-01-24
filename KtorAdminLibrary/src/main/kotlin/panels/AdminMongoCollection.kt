package panels

import models.field.FieldSet

interface AdminMongoCollection : AdminPanel {
    fun getAllFields(): List<FieldSet>
    fun getCollectionName(): String
}