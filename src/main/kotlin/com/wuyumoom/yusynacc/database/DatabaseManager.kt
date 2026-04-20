package com.wuyumoom.yusynacc.database.DataseManager

import com.wuyumoom.yusynacc.config.ConfigManager
import kotlin.collections.iterator
import java.sql.Connection
import java.sql.DriverManager

object DataseManager {
    private val databaseName: String
        get() = ConfigManager.mysql_database
    private val urlOriginal: String
        get() =
                "jdbc:mysql://${ConfigManager.mysql_host}:${ConfigManager.mysql_port}?useSSL=false&connectTimeout=5000&socketTimeout=10000&autoReconnect=true&failOverReadOnly=false&characterEncoding=utf8"

    private val url: String
        get() =
                "jdbc:mysql://${ConfigManager.mysql_host}:${ConfigManager.mysql_port}/$databaseName?useSSL=false&connectTimeout=5000&socketTimeout=10000&autoReconnect=true&failOverReadOnly=false&characterEncoding=utf8"

    private val username: String
        get() = ConfigManager.mysql_username

    private val password: String
        get() = ConfigManager.mysql_password

    @Volatile private var connection: Connection? = null

    init {
        if (!createDatabase()) {
            YuSyaAcc.INSTANCE.server.logger.warning("无法创建数据库 $databaseName。")
        }
        if (createTables()) {
            YuSynAcc.INSTANCE.server.logger.info("§a数据库表创建/检测成功")
        } else {
            
            YuSynAcc.INSTANCE.server.logger.warning("无法创建数据库表。")
        }
        if (!connect()) {
            YuSynAcc.INSTANCE.server.logger.warning("无法连接到数据库！")
        }
    }
    // 创建表
    private fun createTables(): Boolean {
        TODO()
    }
    /** 创建库 */
    private fun createDatabase(): Boolean {
        if (checkDatabaseExists()) return true
        return try {
            DriverManager.getConnection(urlOriginal, username, password).use { conn ->
                conn.prepareStatement(
                                "CREATE DATABASE IF NOT EXISTS `$databaseName` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                        )
                        .use { stmt -> stmt.executeUpdate() }
            }
            YuSynAcc.INSTANCE.server.logger.info("§a数据库 §b$databaseName §a创建成功。")
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    /** 测试库链接 */
    private fun checkDatabaseExists(): Boolean {
        return try {
            DriverManager.getConnection(urlOriginal, username, password).use { conn ->
                conn.prepareStatement("SHOW DATABASES LIKE ?").use { stmt ->
                    stmt.setString(1, databaseName)
                    stmt.executeQuery().use { rs -> rs.next() }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    @Synchronized
    fun ensureConnection(): Connection? {
        return try {
            val conn = connection
            if (conn != null && !conn.isClosed && conn.isValid(2)) return conn
            if (!connect()) null else connection
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }

    /** 数据库连接 */
    fun connect(): Boolean {
        return try {
            val conn = connection
            if (conn != null && !conn.isClosed && conn.isValid(2)) return true

            connection = DriverManager.getConnection(url, username, password)
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    /** 是否链接数据库 */
    fun isConnected(): Boolean {
        return try {
            val conn = connection ?: return false
            !conn.isClosed && conn.isValid(2)
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

}
