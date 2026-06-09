package ir.amirreza.uuid_table

import ir.amirroid.ktoradmin.annotations.defaultvalue.DefaultValueProvider
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import kotlin.uuid.Uuid

open class AdminUuidTable(name: String = "", columnName: String = "id") : IdTable<Uuid>(name) {
    internal var version: UuidVersion = UuidVersion.V4

    /**
     * Identity table with a primary key consisting of an auto-generating [kotlin.uuid.Uuid] value.
     *
     * **Note** The specific UUID column type used depends on the database.
     * BY default, the stored identity value will be auto-generated on the client side, just before insertion of a new row,
     * by calling `Uuid.generateV4()`. If a specific [uuidVersion] is set, the appropriate Uuid companion method will be called,
     * but only for the `id` column. Other Uuid column(s) registered to this table will not be affected by this setting
     * and will rely on their own specified version definition.
     *
     * @param name Table name. By default, this will be resolved from any class name with a "Table" suffix removed (if present).
     * @param columnName Name for the primary key column. By default, "id" is used.
     * @param uuidVersion The specific [UuidVersion] to determine which Uuid companion method to use when generating instances.
     * Setting this parameter to `UuidVersion.V4` is equivalent to instantiating `UuidTable()` without this argument.
     */
    constructor(name: String = "", columnName: String = "id", uuidVersion: UuidVersion) : this(name, columnName) {
        this.version = uuidVersion
    }

    /** The identity column of this [AdminUuidTable], for storing kotlin.uuid.Uuid values wrapped as [EntityID] instances. */
    @DefaultValueProvider("uuid-v4")
    final override val id: Column<EntityID<Uuid>> = uuid(columnName).autoGenerate().entityId()
    final override val primaryKey = PrimaryKey(id)
}
