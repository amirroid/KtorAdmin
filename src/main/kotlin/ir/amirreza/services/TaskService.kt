package ir.amirreza.services

import ir.amirreza.Priority
import ir.amirreza.Tasks
import ir.amirreza.TestTable
import ir.amirreza.ref.TaskUsersCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
data class Task(
    val id: Int = 0,
    val name: String,
    val description: String,
    val priority: Priority,
    val user: User? = null
)


class TaskService(private val database: Database) {
    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Tasks)
            SchemaUtils.create(TaskUsersCrossRef)
            SchemaUtils.create(TestTable)
        }
    }

    suspend fun read(id: Int): Task? {
        return dbQuery {
            Tasks.selectAll().where { Tasks.id eq id }
                .map {
//                    val user = UserService(database).read(it[Tasks.userId])!!
                    Task(it[Tasks.id], it[Tasks.name], it[Tasks.description], it[Tasks.priority], )
                }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<Task> {
        return dbQuery {
            Tasks.selectAll()
                .map {
//                    val user = UserService(database).read(it[Tasks.userId])!!
                    Task(it[Tasks.id], it[Tasks.name], it[Tasks.description], it[Tasks.priority])
                }
        }
    }

    suspend fun updateThumbnail(id: Int, thumbnail: String) {
        dbQuery {
            Tasks.update({ Tasks.id eq id }) {
                it[videoThumbnail] = thumbnail
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}