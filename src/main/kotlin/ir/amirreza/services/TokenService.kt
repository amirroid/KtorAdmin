package ir.amirreza.services

import ir.amirreza.TokenTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter


@Serializable
data class Token(
    val user: User,
    val token: String,
    val expiredAt: String,
)

class TokenService(database: Database) {
    private val userService = UserService(database)

    init {
        transaction(database) {
            SchemaUtils.create(TokenTable)
        }
    }

    suspend fun saveToken(token: String, userId: Int, expiredAt: LocalDateTime): Int = dbQuery {
        TokenTable.upsert {
            it[this.userId] = userId
            it[this.token] = token
            it[this.expiredAt] = expiredAt
        }[TokenTable.userId]
    }

    suspend fun getToken(userId: Int): Token? = dbQuery {
        TokenTable.selectAll().where { TokenTable.userId eq userId }.map {
            Token(
                user = userService.read(it[TokenTable.userId])!!,
                token = it[TokenTable.token],
                expiredAt = it[TokenTable.expiredAt].toJavaLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }.singleOrNull()
    }

    suspend fun getAll(): List<Token> = dbQuery {
        TokenTable.selectAll().map {
            Token(
                user = userService.read(it[TokenTable.userId])!!,
                token = it[TokenTable.token],
                expiredAt = it[TokenTable.expiredAt].toJavaLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }