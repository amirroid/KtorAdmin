package ir.amirroid.ktoradmin.models

/**
 * Object containing constants for various JDBC driver class names.
 * These drivers are required to establish database connections in KtorAdmin.
 */
object JDBCDrivers {
    /** PostgreSQL JDBC driver class name */
    const val POSTGRES = "org.postgresql.Driver"

    /** MySQL JDBC driver class name */
    const val MYSQL = "com.mysql.cj.jdbc.Driver"

    /** MariaDB JDBC driver class name */
    const val MARIADB = "org.mariadb.jdbc.Driver"

    /** SQLite JDBC driver class name */
    const val SQLITE = "org.sqlite.JDBC"

    /** Microsoft SQL Server JDBC driver class name */
    const val MSSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

    /** Oracle JDBC driver class name */
    const val ORACLE = "oracle.jdbc.OracleDriver"

    /** IBM Db2 JDBC driver class name */
    const val DB2 = "com.ibm.db2.jcc.DB2Driver"

    /** Apache Derby Embedded JDBC driver class name */
    const val DERBY_EMBEDDED = "org.apache.derby.jdbc.EmbeddedDriver"

    /** Apache Derby Network JDBC driver class name */
    const val DERBY_NETWORK = "org.apache.derby.jdbc.ClientDriver"

    /** H2 Database Engine JDBC driver class name */
    const val H2 = "org.h2.Driver"

    /** Firebird SQL JDBC driver class name */
    const val FIREBIRD = "org.firebirdsql.jdbc.FBDriver"

    /** Sybase ASE JDBC driver class name */
    const val SYBASE = "com.sybase.jdbc4.jdbc.SybDriver"

    /** ClickHouse JDBC driver class name */
    const val CLICKHOUSE = "ru.yandex.clickhouse.ClickHouseDriver"

    /** Amazon Redshift JDBC driver class name */
    const val REDSHIFT = "com.amazon.redshift.jdbc.Driver"

    /** Snowflake JDBC driver class name */
    const val SNOWFLAKE = "net.snowflake.client.jdbc.SnowflakeDriver"

    /** Google Cloud Spanner JDBC driver class name */
    const val SPANNER = "com.google.cloud.spanner.jdbc.JdbcDriver"

    /** SAP HANA JDBC driver class name */
    const val SAP_HANA = "com.sap.db.jdbc.Driver"

    /** Vertica JDBC driver class name */
    const val VERTICA = "com.vertica.jdbc.Driver"

    /** NuoDB JDBC driver class name */
    const val NUODB = "com.nuodb.jdbc.Driver"

    /** Informix JDBC driver class name */
    const val INFORMIX = "com.informix.jdbc.IfxDriver"
}