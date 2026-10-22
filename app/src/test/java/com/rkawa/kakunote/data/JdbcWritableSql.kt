package com.rkawa.kakunote.data

import java.sql.DriverManager

/** JDBC（sqlite-jdbc）で読み書きする。JVM の単体テストで、Android の SQLite の代わりに使う。 */
class JdbcWritableSql(path: String) : WritableSql, AutoCloseable {

    private val connection = DriverManager.getConnection("jdbc:sqlite:$path")
    private var lastChanges = 0

    override fun execute(sql: String, args: List<Any>) {
        connection.prepareStatement(sql).use { statement ->
            args.forEachIndexed { i, value -> statement.setObject(i + 1, value) }
            statement.execute()
            lastChanges = statement.updateCount.coerceAtLeast(0)
        }
    }

    override fun query(sql: String, args: List<Any>, row: (List<Any?>) -> Unit) {
        connection.prepareStatement(sql).use { statement ->
            args.forEachIndexed { i, value -> statement.setObject(i + 1, value) }
            statement.executeQuery().use { result ->
                val count = result.metaData.columnCount
                while (result.next()) row((1..count).map { result.getObject(it) })
            }
        }
    }

    override fun changes(): Int = lastChanges

    override fun close() = connection.close()
}
