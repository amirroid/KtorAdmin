package ir.amirreza

import ir.amirreza.hibernate.Author
import ir.amirreza.hibernate.Category
import ir.amirreza.hibernate.Metadata
import ir.amirreza.hibernate.Post
import org.hibernate.SessionFactory

object HibernateUtil {
    val sessionFactory: SessionFactory = org.hibernate.cfg.Configuration()
        .configure()
        .addAnnotatedClass(Post::class.java)
        .addAnnotatedClass(Metadata::class.java)
        .addAnnotatedClass(Category::class.java)
        .addAnnotatedClass(Author::class.java)
        .buildSessionFactory()
}

fun getPosts(): List<Post> {
    val session = HibernateUtil.sessionFactory.openSession()
    val users = session.createQuery("FROM Post", Post::class.java).list()
    session.close()
    return users
}