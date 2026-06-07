package ir.amirroid.ktoradmin.configuration

import ir.amirroid.ktoradmin.action.CustomAdminAction
import ir.amirroid.ktoradmin.mapper.KtorAdminValueMapper
import ir.amirroid.ktoradmin.models.FileDeleteStrategy
import ir.amirroid.ktoradmin.models.forms.LoginFiled
import ir.amirroid.ktoradmin.preview.KtorAdminPreview
import ir.amirroid.ktoradmin.translator.KtorAdminTranslator
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class KtorAdminConfigurationTest {
    @Test
    fun `should delegate mutable settings to dynamic configuration`() {
        val configuration = KtorAdminConfiguration()
        val originalZone = configuration.zoneId
        try {
            configuration.maxItemsInPage = 50
            configuration.rateLimitPerMinutes = 99
            configuration.loginFields = listOf(LoginFiled("email", "Email"))
            configuration.defaultLanguage = "en"
            configuration.zoneId = ZoneId.of("UTC")
            configuration.adminPath = "control"
            configuration.fileDeleteStrategy = FileDeleteStrategy.DELETE

            assertEquals(50, KtorAdminConfiguration().maxItemsInPage)
            assertEquals(99, KtorAdminConfiguration().rateLimitPerMinutes)
            assertEquals(listOf(LoginFiled("email", "Email")), KtorAdminConfiguration().loginFields)
            assertEquals("en", KtorAdminConfiguration().defaultLanguage)
            assertEquals(ZoneId.of("UTC"), KtorAdminConfiguration().zoneId)
            assertEquals("control", KtorAdminConfiguration().adminPath)
            assertEquals(FileDeleteStrategy.DELETE, KtorAdminConfiguration().fileDeleteStrategy)
        } finally {
            configuration.zoneId = originalZone
            configuration.fileDeleteStrategy = FileDeleteStrategy.KEEP
        }
    }

    @Test
    fun `should reject inherit file delete strategy as global setting`() {
        assertFailsWith<IllegalArgumentException> {
            KtorAdminConfiguration().fileDeleteStrategy = FileDeleteStrategy.INHERIT
        }
    }

    @Test
    fun `should reject duplicate value mapper preview translator and custom action keys`() {
        val unique = System.nanoTime().toString()
        val configuration = KtorAdminConfiguration()
        val mapper = valueMapper("mapper-$unique")
        val preview = preview("preview-$unique")
        val translator = translator("lang-$unique")
        val action = action("action-$unique")

        configuration.registerValueMapper(mapper)
        configuration.registerPreview(preview)
        configuration.registerTranslator(translator)
        configuration.registerCustomAdminAction(action)
        configuration.registerCustomAdminActionForAll(action("all-action-$unique"))

        assertFailsWith<IllegalStateException> { configuration.registerValueMapper(mapper) }
        assertFailsWith<IllegalStateException> { configuration.registerPreview(preview) }
        assertFailsWith<IllegalArgumentException> { configuration.registerTranslator(translator) }
        assertFailsWith<IllegalArgumentException> { configuration.registerCustomAdminAction(action) }
        assertFailsWith<IllegalArgumentException> { configuration.registerCustomAdminActionForAll(action("all-action-$unique")) }
        assertSame(preview, DynamicConfiguration.getPreview("preview-$unique"))
        assertSame(translator, DynamicConfiguration.getTranslator("lang-$unique"))
    }

    private fun valueMapper(key: String) =
        object : KtorAdminValueMapper {
            override val key: String = key

            override fun map(value: Any?): Any? = value

            override fun restore(value: Any?): Any? = value
        }

    private fun preview(key: String) =
        object : KtorAdminPreview() {
            override val key: String = key

            override fun createPreview(
                tableName: String,
                name: String,
                value: Any?,
            ): String? = value?.toString()
        }

    private fun translator(languageCode: String) =
        object : KtorAdminTranslator() {
            override val languageCode: String = languageCode
            override val languageName: String = languageCode
            override val translates: Map<String, String> = mapOf(Keys.DASHBOARD to "Dashboard")
        }

    private fun action(key: String) =
        object : CustomAdminAction {
            override var key: String = key
            override val displayText: String = key

            override suspend fun performAction(
                name: String,
                selectedIds: List<String>,
            ) = Unit
        }
}
