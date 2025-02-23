package ir.amirreza.hibernate

import annotations.hibernate.HibernateTable
import annotations.info.ColumnInfo
import annotations.info.IgnoreColumn
import annotations.limit.Limits
import annotations.preview.Preview
import annotations.roles.AccessRoles
import annotations.type.OverrideColumnType
import annotations.uploads.LocalUpload
import ir.amirreza.Priority
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import models.types.ColumnType
import javax.persistence.*

@Entity
@Table(name = "post")
@HibernateTable("Post", "Posts")
@Serializable
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @IgnoreColumn
    var id: Long? = null,

    var title: String,

    @Column(name = "content")
    var content: String,

    @Enumerated(EnumType.STRING)
    @ColumnInfo(verboseName = "Priority")
    val priority: Priority = Priority.Low,

    @LocalUpload
    @Preview("image")
    @Limits(
        allowedMimeTypes = ["image/jpeg", "image/png"]
    )
    var file: String? = null,


    @OneToOne
    @JoinColumn(name = "author_id", nullable = false)
    var author: Author
)


@Entity
@Table(name = "authors")
@HibernateTable("Author", "Authors")
@Serializable
data class Author(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    val name: String,
)