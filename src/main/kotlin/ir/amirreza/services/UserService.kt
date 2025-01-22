package ir.amirreza.services

import ir.amirreza.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class User(
    val id: Int = 0,
    val username: String,
    val email: String,
    val password: String,
)

class UserService(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: User): Int = dbQuery {
        Users.insert {
            it[username] = user.username
            it[email] = user.email
            it[password] = user.password
        }[Users.id]
    }

    suspend fun read(id: Int): User? {
        return dbQuery {
            Users.selectAll().where { Users.id eq id }
                .map { User(it[Users.id], it[Users.username], it[Users.email], it[Users.password]) }
                .singleOrNull()
        }
    }

    suspend fun getUser(username: String, password: String): User? {
        return dbQuery {
            Users.selectAll().where { (Users.username eq username) and (Users.password eq password) }
                .map { User(it[Users.id], it[Users.username], it[Users.email], it[Users.password]) }
                .singleOrNull()
        }
    }


    suspend fun readAll(): List<User> {
        return dbQuery {
            Users.selectAll()
                .map { User(it[Users.id], it[Users.username], it[Users.email], it[Users.password]) }
        }
    }

    suspend fun updateAllNames(name: String) = dbQuery {
        Users.update {
            it[this.username] = name
        }
    }

    suspend fun update(id: Int, user: User) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[username] = user.username
                it[email] = user.email
                it[password] = user.password
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id eq id }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}