package utils

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import java.io.OutputStream

object FileUtils {
    fun getGeneratedFileName(simpleFileName: String): String = "${simpleFileName}AdminPanel"
}