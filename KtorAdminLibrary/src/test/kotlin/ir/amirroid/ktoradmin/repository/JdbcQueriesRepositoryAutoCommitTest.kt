package ir.amirroid.ktoradmin.repository

class JdbcQueriesRepositoryAutoCommitTest : JdbcQueriesRepositoryIntegrationTest() {
    override val autoCommit: Boolean = false
}