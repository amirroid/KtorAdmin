package processors.mongo

import annotations.mongo.MongoCollection
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import org.bson.types.ObjectId

class MongoCollectionProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(MongoCollection::class.qualifiedName ?: return emptyList())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach(::generateDesiredClass)
        return emptyList()
    }

    private fun generateDesiredClass(classDeclaration: KSClassDeclaration) {
        val propertiesMap = mutableMapOf<String, String>()
        collectPropertiesRecursively(classDeclaration, propertiesMap)
        generatePropertiesFile(propertiesMap, classDeclaration)
    }

    private fun collectPropertiesRecursively(
        classDeclaration: KSClassDeclaration,
        propertiesMap: MutableMap<String, String>, // نگاشت مسیر پراپرتی‌ها به نوع آن‌ها
        parentPath: String = "", // مسیر فعلی والد
        visitedClasses: MutableSet<String> = mutableSetOf() // جلوگیری از پردازش کلاس‌های تکراری
    ) {
        val className = classDeclaration.simpleName.asString()

        // جلوگیری از پردازش کلاس‌های تکراری
        if (visitedClasses.contains(className)) return
        visitedClasses.add(className)

        // پردازش پراپرتی‌های کلاس
        classDeclaration.getDeclaredProperties().forEach { property ->
            val propertyName = property.simpleName.asString()
            val resolvedType = property.type.resolve()
            val propertyType = resolvedType.declaration.simpleName.asString()

            // ساخت مسیر کامل پراپرتی
            val fullPath =
                if (parentPath.isNotEmpty()) "$parentPath${
                    propertyName.lowercase().replaceFirstChar { it.uppercaseChar() }
                }" else propertyName

            // اضافه کردن پراپرتی به لیست
            propertiesMap[fullPath] = propertyType

            // اگر نوع پراپرتی یک کلاس دیگر باشد، بازگشتی پردازش شود
            if (resolvedType.declaration is KSClassDeclaration) {
                val nestedClassDeclaration = resolvedType.declaration as KSClassDeclaration
                collectPropertiesRecursively(
                    nestedClassDeclaration,
                    propertiesMap,
                    fullPath, // مسیر کامل فعلی به‌عنوان مسیر والد برای پراپرتی‌های داخلی
                    visitedClasses
                )
            }
        }
    }

    private fun generatePropertiesFile(
        propertiesMap: Map<String, String>, // نگاشت مسیر پراپرتی‌ها به نوع آن‌ها
        classDeclaration: KSClassDeclaration,
    ) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val fileName = "${className}AllProperties"

        // ایجاد فایل جدید
        val file = environment.codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = fileName
        )

        // نوشتن اطلاعات در فایل
        file.bufferedWriter().use { writer ->
            writer.write("package $packageName\n\n")
            writer.write("object $fileName {\n")
            propertiesMap.forEach { (path, type) ->
                writer.write("    val $path = \"$type\"\n")
            }
            writer.write("}\n")
        }
    }
}
