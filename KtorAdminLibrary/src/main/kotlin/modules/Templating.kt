package modules

import io.ktor.server.application.*
import io.ktor.server.velocity.*
import org.apache.velocity.app.event.implement.IncludeRelativePath
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader

fun Application.configureTemplating() {
    if (pluginOrNull(Velocity) == null){
        install(Velocity) {
            setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
            setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
        }
    }
}