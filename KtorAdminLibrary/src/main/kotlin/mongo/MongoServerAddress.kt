package mongo

data class MongoServerAddress(
    val host: String,
    val port: Int,
)