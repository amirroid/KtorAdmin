package ir.amirreza.hibernate

import kotlinx.serialization.Serializable
import javax.persistence.*

@Entity
@Table(name = "post")
@Serializable
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    var id: Long = 0,
    @Column(name = "title")
    var title: String,
    @Column(name = "content")
    var content: String
)

//@Serializable
//@Entity
//@Table(name = "authors")
//data class Author(
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    var id: Long = 0,
//
//    @Column(nullable = false)
//    val name: String
//)