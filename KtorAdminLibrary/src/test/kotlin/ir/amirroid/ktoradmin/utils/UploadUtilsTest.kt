package ir.amirroid.ktoradmin.utils

import ir.amirroid.ktoradmin.models.types.FieldType
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UploadUtilsTest {
    @Test
    fun `should accept string properties for upload fields`() {
        assertTrue(UploadUtils.validatePropertyType("kotlin.String", "avatar"))
        assertTrue(UploadUtils.validatePropertyType("avatar", FieldType.String))
    }

    @Test
    fun `should reject non string jdbc upload properties`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                UploadUtils.validatePropertyType("kotlin.Int", "avatar")
            }

        assertTrue(exception.message!!.contains("Column 'avatar' must be of type 'kotlin.String'"))
    }

    @Test
    fun `should reject non string mongo upload fields`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                UploadUtils.validatePropertyType("avatar", FieldType.Integer)
            }

        assertTrue(exception.message!!.contains("must be of type 'kotlin.String'"))
    }
}
