package processors

import com.google.devtools.ksp.symbol.KSAnnotation

internal val KSAnnotation.qualifiedName: String?
    get() = annotationType.resolve().declaration.qualifiedName?.asString()