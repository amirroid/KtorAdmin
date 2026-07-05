package ir.amirroid.ktoradmin.audit

import ir.amirroid.ktoradmin.listener.AdminEventListener
import ir.amirroid.ktoradmin.models.events.ColumnEvent
import ir.amirroid.ktoradmin.models.events.FieldEvent

class AuditEventListener(
    private val logger: AuditLogger,
    private val enrich: (AuditEvent) -> AuditEvent = { it },
) : AdminEventListener() {
    override suspend fun onInsertJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>,
    ) {
        logger.log(
            buildMutationEvent(
                action = AuditAction.INSERT,
                resourceType = AuditResourceType.JDBC_TABLE,
                resourceName = tableName,
                objectPrimaryKey = objectPrimaryKey,
                changes = events.map { it.toAuditFieldChange() },
            ),
        )
    }

    override suspend fun onInsertMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>,
    ) {
        logger.log(
            buildMutationEvent(
                action = AuditAction.INSERT,
                resourceType = AuditResourceType.MONGO_COLLECTION,
                resourceName = collectionName,
                objectPrimaryKey = objectPrimaryKey,
                changes = events.map { it.toAuditFieldChange() },
            ),
        )
    }

    override suspend fun onUpdateJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>,
    ) {
        logger.log(
            buildMutationEvent(
                action = AuditAction.UPDATE,
                resourceType = AuditResourceType.JDBC_TABLE,
                resourceName = tableName,
                objectPrimaryKey = objectPrimaryKey,
                changes = events.map { it.toAuditFieldChange() },
            ),
        )
    }

    override suspend fun onUpdateMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>,
    ) {
        logger.log(
            buildMutationEvent(
                action = AuditAction.UPDATE,
                resourceType = AuditResourceType.MONGO_COLLECTION,
                resourceName = collectionName,
                objectPrimaryKey = objectPrimaryKey,
                changes = events.map { it.toAuditFieldChange() },
            ),
        )
    }

    override suspend fun onDeleteJdbcObjects(
        tableName: String,
        objectPrimaryKeys: List<String>,
    ) {
        logger.log(
            buildDeleteEvent(
                resourceType = AuditResourceType.JDBC_TABLE,
                resourceName = tableName,
                objectPrimaryKeys = objectPrimaryKeys,
            ),
        )
    }

    override suspend fun onDeleteMongoObjects(
        collectionName: String,
        objectPrimaryKeys: List<String>,
    ) {
        logger.log(
            buildDeleteEvent(
                resourceType = AuditResourceType.MONGO_COLLECTION,
                resourceName = collectionName,
                objectPrimaryKeys = objectPrimaryKeys,
            ),
        )
    }

    private fun buildMutationEvent(
        action: AuditAction,
        resourceType: AuditResourceType,
        resourceName: String,
        objectPrimaryKey: String,
        changes: List<AuditFieldChange>,
    ): AuditEvent =
        enrich(
            AuditEvent(
                action = action,
                resourceType = resourceType,
                resourceName = resourceName,
                objectPrimaryKeys = listOf(objectPrimaryKey),
                changes = changes,
            ),
        )

    private fun buildDeleteEvent(
        resourceType: AuditResourceType,
        resourceName: String,
        objectPrimaryKeys: List<String>,
    ): AuditEvent =
        enrich(
            AuditEvent(
                action = AuditAction.DELETE,
                resourceType = resourceType,
                resourceName = resourceName,
                objectPrimaryKeys = objectPrimaryKeys,
            ),
        )

    private fun ColumnEvent.toAuditFieldChange(): AuditFieldChange =
        AuditFieldChange(
            name = columnSet.columnName,
            verboseName = columnSet.verboseName,
            changed = changed,
            value = value,
        )

    private fun FieldEvent.toAuditFieldChange(): AuditFieldChange =
        AuditFieldChange(
            name = fieldSet.fieldName.orEmpty(),
            verboseName = fieldSet.verboseName,
            changed = changed,
            value = value,
        )
}
