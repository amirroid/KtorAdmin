package models

enum class JDBCDrivers(val driver: String) {
    POSTGRESQL_NG("com.impossibl.postgres.jdbc.PGDataSource"),
    POSTGRESQL("org.postgresql.Driver"),
    SQLITE("org.sqlite.SQLiteDataSource"),
    SY_BASE("com.sybase.jdbc4.jdbc.SybDataSource"),
    MYSQL("com.microsoft.sqlserver.jdbc.SQLServerDataSource"),
    H2("org.h2.jdbcx.JdbcDataSource"),
    APACHE_DERBY("org.apache.derby.jdbc.ClientDataSource"),
    ORACLE("oracle.jdbc.pool.OracleDataSource"),
}