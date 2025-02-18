package ir.amirreza.hibernate

import annotations.hibernate.HibernateTable
import annotations.info.ColumnInfo
import annotations.type.OverrideColumnType
import annotations.uploads.LocalUpload
import ir.amirreza.Priority
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import models.types.ColumnType
import org.hibernate.annotations.Type
import javax.persistence.*

@Entity
@Table(name = "post")
@HibernateTable("post")
@Serializable
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    var id: Long = 0,
    var titleContent: String,
    @Column(name = "content")
    var content: String,
    @Enumerated(EnumType.STRING)
    @ColumnInfo(verboseName = "Priority")
    val priority: Priority = Priority.Low,
    @LocalUpload
    var file: String? = null
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