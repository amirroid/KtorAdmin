package ir.amirreza

import annotations.mongo.MongoCollection
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.LocalDateTime

@MongoCollection(
    "restaurants",
    "id"
)
data class Restaurant(
    @BsonId
    val id: ObjectId,
    val address: Address,
    val borough: String,
    val cuisine: String,
    val grades: List<Grade>,
    val name: String,
    @BsonProperty("restaurant_id")
    val restaurantId: String
)

data class Address(
    val building: String,
    val street: String,
    val zipcode: String,
    val coord: List<Double>,
    val test: Test
)

data class Test(
    val name: String,
    val id: Int
)

data class Grade(
    val date: LocalDateTime,
    val grade: String,
    val score: Int
)