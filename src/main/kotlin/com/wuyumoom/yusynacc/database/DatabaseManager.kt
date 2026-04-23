package com.wuyumoom.yusynacc.database

import com.wuyumoom.yusynacc.YuSynAcc
import com.wuyumoom.yusynacc.config.ConfigManager
import com.wuyumoom.yusynacc.data.PlayerData
import com.wuyumoom.yusynacc.data.Slot
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.collections.iterator

object DatabaseManager {
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
            YuSynAcc.INSTANCE.server.logger
                .warning("无法创建数据库 $databaseName。")
        }
        if (createTables()) {
            YuSynAcc.INSTANCE.server.logger
                .info("§a数据库表创建/检测成功")
        } else {

            YuSynAcc.INSTANCE.server.logger
                .warning("无法创建数据库表。")
        }
        if (!connect()) {
            YuSynAcc.INSTANCE.server.logger
                .warning("无法连接到数据库！")
        }
    }

    // 创建表
    private fun createTables(): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn
                    .prepareStatement(
                        """
                        CREATE TABLE IF NOT EXISTS `player_data` (
                            `player_name` VARCHAR(64) NOT NULL PRIMARY KEY,
                            `slot_data` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
                            `last_update` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            INDEX `idx_last_update` (`last_update`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.executeUpdate()
                    }
            }
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    /** 保存玩家数据到数据库 */
    fun savePlayerData(
        playerName: String,
        playerData: PlayerData,
    ): Boolean =
        try {
            ensureConnection()?.use { conn ->
                val json = serializeSlotMap(playerData.map)
                conn
                    .prepareStatement(
                        """
                        INSERT INTO `player_data` (`player_name`, `slot_data`) 
                        VALUES (?, ?) 
                        ON DUPLICATE KEY UPDATE `slot_data` = VALUES(`slot_data`)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.setString(2, json)
                        stmt.executeUpdate()
                    }
            }
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    /** 从数据库加载玩家数据 */
    fun loadPlayerData(playerName: String): PlayerData =
        try {
            ensureConnection()?.use { conn ->
                conn
                    .prepareStatement("SELECT `slot_data` FROM `player_data` WHERE `player_name` = ?")
                    .use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                val json = rs.getString("slot_data")
                                val slotMap = deserializeSlotMap(json)
                                PlayerData(slotMap)
                            } else {
                                PlayerData()
                            }
                        }
                    }
            } ?: PlayerData()
        } catch (e: SQLException) {
            e.printStackTrace()
            PlayerData()
        }

    /** 删除玩家数据 */
    fun deletePlayerData(playerName: String): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn
                    .prepareStatement("DELETE FROM `player_data` WHERE `player_name` = ?")
                    .use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.executeUpdate() > 0
                    }
            } ?: false
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    /** 序列化 Slot Map 为 JSON 字符串 */
    private fun serializeSlotMap(map: MutableMap<Slot, String>): String {
        if (map.isEmpty()) return "{}"

        val entries =
            map
                .map { (slot, value) ->
                    """{"name":"${escapeJson(slot.name)}","id":${slot.id},"value":"${escapeJson(value)}"}"""
                }.joinToString(",", "[", "]")

        return entries
    }

    /** 反序列化 JSON 字符串为 Slot Map */
    private fun deserializeSlotMap(json: String?): MutableMap<Slot, String> {
        if (json.isNullOrBlank() || json == "{}") return mutableMapOf()

        val map = mutableMapOf<Slot, String>()

        try {
            val regex = """\{"name":"([^"]*)","id":(\d+),"value":"([^"]*)"\}""".toRegex()
            regex.findAll(json).forEach { match ->
                val name = match.groupValues[1]
                val id = match.groupValues[2].toInt()
                val value = match.groupValues[3]
                map[Slot(name, id)] = value
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    /** 转义 JSON 特殊字符 */
    private fun escapeJson(str: String): String =
        str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    /** 创建库 */
    private fun createDatabase(): Boolean {
        if (checkDatabaseExists()) return true
        return try {
            DriverManager.getConnection(urlOriginal, username, password).use { conn ->
                conn
                    .prepareStatement(
                        "CREATE DATABASE IF NOT EXISTS `$databaseName` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    ).use { stmt -> stmt.executeUpdate() }
            }
            YuSynAcc.INSTANCE.server.logger
                .info("§a数据库 §b$databaseName §a创建成功。")
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    /** 测试库链接 */
    private fun checkDatabaseExists(): Boolean =
        try {
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
