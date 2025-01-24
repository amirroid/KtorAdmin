package ir.amirreza

import annotations.mongo.MongoCollection
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.LocalDateTime

@MongoCollection(
    "restaurants",
    "id",
    pluralName = "restaurants",
    singularName = "restaurant",
)
data class Restaurant(
    @BsonId
    val id: ObjectId,
    val address: Address,
    val borough: LocalDateTime,
    val cuisine: String,
    val tests: List<LocalDateTime>,
    val grades: List<Grade>,
    val name: String,
    @BsonProperty("restaurant_id")
    val restaurantId: String,
    val p: Map<String, String>
)

data class Address(
    val building: String,
    val street: String,
    val zipcode: LocalDateTime,
    val coord: List<Double>,
//    val test: Test
)

data class Test(
    val name: String,
    val id: Int
)

data class Grade(
    val date: LocalDateTime,
    val grade: String,
    val score: Int,
    val tests: List<Test>
)