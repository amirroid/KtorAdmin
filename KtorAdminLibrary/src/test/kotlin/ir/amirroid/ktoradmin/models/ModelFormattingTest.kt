package ir.amirroid.ktoradmin.models

import ir.amirroid.ktoradmin.models.common.Reference
import ir.amirroid.ktoradmin.models.common.foreignKey
import ir.amirroid.ktoradmin.models.common.tableName
import ir.amirroid.ktoradmin.models.common.toFormattedString
import ir.amirroid.ktoradmin.models.order.Order
import ir.amirroid.ktoradmin.models.order.toFormattedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelFormattingTest {
    @Test
    fun `should format upload targets with nullable values and delete strategies`() {
        assertEquals("UploadTarget.LocalFile(path=\"/tmp/uploads\", deleteStrategy=FileDeleteStrategy.DELETE)", UploadTarget.LocalFile("/tmp/uploads", FileDeleteStrategy.DELETE).toFormattedString())
        assertEquals("UploadTarget.AwsS3(bucket=null, deleteStrategy=FileDeleteStrategy.KEEP)", UploadTarget.AwsS3(deleteStrategy = FileDeleteStrategy.KEEP).toFormattedString())
        assertEquals("UploadTarget.Custom(key=\"images\")", UploadTarget.Custom("images").toFormattedString())
    }

    @Test
    fun `should expose reference table and foreign key metadata`() {
        val oneToOne = Reference.OneToOne("profiles", "profile_id")
        val manyToMany = Reference.ManyToMany("roles", "user_roles", "user_id", "role_id")

        assertEquals("profiles", oneToOne.tableName)
        assertEquals("profile_id", oneToOne.foreignKey)
        assertEquals("roles", manyToMany.tableName)
        assertNull(manyToMany.foreignKey)
        assertTrue(manyToMany.toFormattedString().contains("joinTable = \"user_roles\""))
    }

    @Test
    fun `should format limit and order for generated code`() {
        val limit = Limit(maxLength = 10, minLength = 2, regexPattern = "[a-z]+", allowedMimeTypes = listOf("image/png"))
        val order = Order("created_at", direction = "DESC")

        assertTrue(limit.toFormattedString().contains("maxLength = 10"))
        assertTrue(limit.toFormattedString().contains("allowedMimeTypes = listOf(\"image/png\")"))
        assertTrue(order.toFormattedString().contains("name = \"created_at\""))
        assertTrue(order.toFormattedString().contains("direction = \"DESC\""))
    }
}
