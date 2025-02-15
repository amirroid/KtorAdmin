package ir.amirreza.hibernate

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "posts")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "content", columnDefinition = "TEXT")
    val content: String,

    @Column(name = "published", nullable = false)
    val published: Boolean = false,

    @Column(name = "view_count")
    val viewCount: Int = 0,

    @Column(name = "rating")
    val rating: Double = 0.0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "publish_date")
    val publishDate: LocalDate? = null,

    @Column(name = "publish_time")
    val publishTime: LocalTime? = null,

    @Lob
    @Column(name = "image")
    val image: ByteArray? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: Status = Status.DRAFT,

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "tag")
    val tags: Set<String> = emptySet(),

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "metadata_id", referencedColumnName = "id")
    val metadata: Metadata? = null,

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    val author: Author,

    @ManyToMany
    @JoinTable(
        name = "post_categories",
        joinColumns = [JoinColumn(name = "post_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")]
    )
    val categories: Set<Category> = emptySet()
)

enum class Status {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}

@Entity
@Table(name = "metadata")
data class Metadata(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "keywords")
    val keywords: String? = null,

    @Column(name = "description")
    val description: String? = null
)

@Entity
@Table(name = "authors")
data class Author(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false)
    val name: String,

    @OneToMany(mappedBy = "author")
    val posts: List<Post> = emptyList()
)

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false)
    val name: String,

    @ManyToMany(mappedBy = "categories")
    val posts: Set<Post> = emptySet()
)