package repository

import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.kotlin.client.coroutine.MongoClient
import mongo.MongoCredential
import mongo.MongoServerAddress

internal object MongoClientRepository {
    private var clients: MutableMap<String?, MongoClient> = mutableMapOf()

    fun registerNewClient(key: String?, address: MongoServerAddress, credential: MongoCredential? = null) {
        if (clients.containsKey(key)) {
            throw IllegalArgumentException("Client with key '$key' already exists")
        }
        val settings = MongoClientSettings.builder()
            .let {
                if (credential != null) {
                    it.credential(
                        com.mongodb.MongoCredential.createCredential(
                            credential.username, credential.database, credential.password.toCharArray()
                        )
                    )
                } else it
            }
            .applyToClusterSettings { cluster -> cluster.hosts(listOf(ServerAddress(address.host, address.port))) }
            .build()
        clients[key] = MongoClient.create(settings)
    }
}