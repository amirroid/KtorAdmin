package ir.amirroid.ktoradmin.annotation_providers

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import ir.amirroid.ktoradmin.processors.mongo.MongoCollectionProcessor

class MongoCollectionAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return MongoCollectionProcessor(environment)
    }
}