package ir.amirreza

import io.ktor.server.application.Application
import io.ktor.server.application.install
import ir.amirreza.action.MyCustomAction
import ir.amirreza.audit.registerSampleAuditLog
import ir.amirreza.dashboard.CustomDashboard
import ir.amirreza.dashboard.QuickLinksSection
import ir.amirreza.dashboard.ServerStatusSection
import ir.amirreza.dashboard.SystemOverviewDashboard
import ir.amirreza.listeners.AdminListener
import ir.amirreza.pages.AboutPage
import ir.amirreza.pages.HelpPage
import ir.amirreza.pages.SettingsPage
import ir.amirreza.pages.SystemStatusPage
import ir.amirreza.previews.ImagePreview
import ir.amirreza.previews.VideoPreview
import ir.amirroid.ktoradmin.mapper.KtorAdminValueMapper
import ir.amirroid.ktoradmin.models.FileDeleteStrategy
import ir.amirroid.ktoradmin.models.JDBCDrivers
import ir.amirroid.ktoradmin.models.UploadTarget
import ir.amirroid.ktoradmin.models.forms.LoginFiled
import ir.amirroid.ktoradmin.models.menu.Menu
import ir.amirroid.ktoradmin.mongo.MongoCredential
import ir.amirroid.ktoradmin.mongo.MongoServerAddress
import ir.amirroid.ktoradmin.plugins.KtorAdmin
import ir.amirroid.ktoradmin.provider.defaultvalue.uuid.UUIDDefaultValueProvider
import ir.amirroid.ktoradmin.template.DefaultAdminTemplateSettings
import ir.amirroid.ktoradmin.templates.fluent.FluentAdminTemplate
import ir.amirroid.ktoradmin.tiny.TinyMCEConfig
import ir.amirroid.ktoradmin.translator.locals.fa.PersianKtorAdminTranslator
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

const val MEDIA_ROOT = "files"
const val MEDIA_PATH = "uploads"

fun Application.configureAdmin(database: Database) {
    install(KtorAdmin) {
        jdbc(
            key = null,
            url = "jdbc:postgresql://localhost:5432/postgres",
            username = "amirreza",
            password = "your_password",
            driver = JDBCDrivers.POSTGRES
        )
        mongo(
            key = null,
            databaseName = "0@localhost",
            address = MongoServerAddress("localhost", 27017),
            credential = MongoCredential(
                "amirreza", "admin", "your_password"
            ),
        )
        fileDeleteStrategy = FileDeleteStrategy.DELETE
//        authenticateName = "admin"
        mediaPath = MEDIA_PATH
        registerTranslator(PersianKtorAdminTranslator)
        mediaRoot = MEDIA_ROOT
        dashboard {
            register(CustomDashboard())
            register(SystemOverviewDashboard())

            page("analytics") {
                title = "Analytics"
                icon = "/static/images/info.svg"
                groupName = "Operations"
                order = 1

                configureLayout {
                    addSection(section = ServerStatusSection(), height = "200px")
                    addSection(section = QuickLinksSection(), height = "200px")
                    media(maxWidth = "600px", template = listOf(1))
                }
            }

            page("metrics") {
                title = "Metrics"
                groupName = "Monitoring"
                order = 0

                configureLayout {
                    addSection(section = QuickLinksSection(), height = "250px")
                }
            }
        }
        defaultAwsS3Bucket = "school-data"
        s3SignatureDuration = 1.minutes.toJavaDuration()
        loginFields = adminLoginFields
        csrfTokenExpirationTime = 1000 * 60
        registerCustomAdminActionForAll(MyCustomAction())
        registerEventListener(AdminListener(database))
        registerSampleAuditLog()
        canDownloadDataAsCsv = true
        canDownloadDataAsPdf = true
        tinyMCEConfig =
            TinyMCEConfig.Professional.copy(uploadTarget = UploadTarget.LocalFile(path = null))
        registerValueMapper(
            CustomValueMapper
        )

//        template = FluentAdminTemplate()
//        template = DefaultAdminTemplate(
//            settings = DefaultAdminTemplateSettings(
//                colors = lightModeColors,
//                darkModeColors = darkModeColors,
////                typography = DefaultAdminTemplateSettings.Typography(
////                    font = FontFamily.fromGoogleFonts("Roboto", weights = listOf(300, 400, 700)),
////                ),
//                header = DefaultAdminTemplateSettings.HeaderStyle(
//                    content = DefaultAdminTemplateSettings.HeaderContent.Text(
//                        prefix = "K",
//                        text = "Admin"
//                    ),
//                ),
//            )
//        )

        registerDefaultValueProvider(UUIDDefaultValueProvider())
        registerPreview(VideoPreview())
        registerPreview(ImagePreview())
        autoCompletePageSize = 10
        registerValueMapper(
            CustomValueMapper2
        )
        loginPageMessage =
            "Enter <strong  class=\"istok-web-black\">admin</strong> as username and <strong class=\"istok-web-black\">password</strong> as password."
        adminPath = "app"
        provideMenu { tableName ->
            if (tableName == "tasks") listOf(
                Menu(
                    "Github", "https://github.com/Amirroid"
                )
            ) else emptyList()
        }

        customPage(SettingsPage())
        customPage(SystemStatusPage())
        customPage(HelpPage())
        customPage(AboutPage())
    }
}

object CustomValueMapper : KtorAdminValueMapper {
    override fun map(value: Any?): Any? {
        return when (value) {
            is Int -> value.times(2)
            else -> null
        }
    }

    override fun restore(value: Any?): Any? {
        return when (value) {
            is Int -> value.div(2)
            else -> null
        }
    }

    override val key: String
        get() = "timesTo2"
}

object CustomValueMapper2 : KtorAdminValueMapper {
    override fun map(value: Any?): Any? {
        return (value as? String)?.plus(" Test")
    }

    override fun restore(value: Any?): Any? {
        return (value as? String)?.replace(" Test", "")
    }

    override val key: String
        get() = "test"
}


private val adminLoginFields = listOf(
    LoginFiled(
        name = "Username",
        key = "username",
        autoComplete = "username"
    ),
    LoginFiled(
        name = "Password",
        key = "password",
        autoComplete = "current-password",
        type = "password"
    )
)

val lightModeColors = DefaultAdminTemplateSettings.Colors(
    primaryColor = "#059669",
    secondaryColor = "#047857",
    backgroundGradientStart = "#ECFDF5",
    backgroundGradientEnd = "#D1FAE5",
    highlightColor = "#10B981",
    errorColor = "#DC2626",

    evenRowColor = "rgba(236, 253, 245, 0.35)",
    oddRowColor = "transparent",
    hoverRowColor = "rgba(5, 150, 105, 0.15)",
)

val darkModeColors = DefaultAdminTemplateSettings.Colors(
    primaryColor = "#E5E7EB",
    secondaryColor = "#34D399",
    backgroundGradientStart = "#052E26",
    backgroundGradientEnd = "#064E3B",
    highlightColor = "#10B981",
    errorColor = "#EF4444",

    evenRowColor = "rgba(52, 211, 153, 0.12)",
    oddRowColor = "transparent",
    hoverRowColor = "rgba(16, 185, 129, 0.18)",
)
