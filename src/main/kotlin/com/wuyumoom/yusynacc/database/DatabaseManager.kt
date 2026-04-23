package com.wuyumoom.yusynacc.database

import com.wuyumoom.yusynacc.YuSynAcc
import com.wuyumoom.yusynacc.config.ConfigManager
import com.wuyumoom.yusynacc.data.PlayerData
import com.wuyumoom.yusynacc.data.Slot
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

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

    private fun createTables(): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS `player_data` (
                            `player_name` VARCHAR(64) NOT NULL PRIMARY KEY,
                            `last_update` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            INDEX `idx_last_update` (`last_update`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                        """.trimIndent(),
                    )
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS `player_slots` (
                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                            `player_name` VARCHAR(64) NOT NULL,
                            `slot_name` VARCHAR(64) NOT NULL,
                            `slot_id` INT NOT NULL,
                            `nbt_data` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
                            `last_update` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            FOREIGN KEY (`player_name`) REFERENCES `player_data`(`player_name`) ON DELETE CASCADE,
                            UNIQUE KEY `uk_player_slot` (`player_name`, `slot_name`, `slot_id`),
                            INDEX `idx_player` (`player_name`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                        """.trimIndent(),
                    )
                }
            }
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    fun savePlayerData(
        playerName: String,
        playerData: PlayerData,
    ): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn.autoCommit = false

                try {
                    conn
                        .prepareStatement(
                            "INSERT INTO `player_data` (`player_name`) VALUES (?) ON DUPLICATE KEY UPDATE `player_name` = VALUES(`player_name`)",
                        ).use { stmt ->
                            stmt.setString(1, playerName)
                            stmt.executeUpdate()
                        }

                    conn
                        .prepareStatement("DELETE FROM `player_slots` WHERE `player_name` = ?")
                        .use { stmt ->
                            stmt.setString(1, playerName)
                            stmt.executeUpdate()
                        }

                    if (playerData.map.isNotEmpty()) {
                        val insertStmt =
                            conn.prepareStatement(
                                "INSERT INTO `player_slots` (`player_name`, `slot_name`, `slot_id`, `nbt_data`) VALUES (?, ?, ?, ?)",
                            )
                        playerData.map.forEach { (slot, nbt) ->
                            insertStmt.setString(1, playerName)
                            insertStmt.setString(2, slot.name)
                            insertStmt.setInt(3, slot.id)
                            insertStmt.setString(4, nbt)
                            insertStmt.addBatch()
                        }
                        insertStmt.executeBatch()
                        insertStmt.close()
                    }

                    conn.commit()
                } catch (e: SQLException) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            }
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    fun loadPlayerData(playerName: String): PlayerData =
        try {
            ensureConnection()?.use { conn ->
                val slotMap = mutableMapOf<Slot, String>()
                conn
                    .prepareStatement(
                        "SELECT `slot_name`, `slot_id`, `nbt_data` FROM `player_slots` WHERE `player_name` = ?",
                    ).use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                val slotName = rs.getString("slot_name")
                                val slotId = rs.getInt("slot_id")
                                val nbtData = rs.getString("nbt_data")
                                slotMap[Slot(slotName, slotId)] = nbtData ?: ""
                            }
                        }
                    }

                if (slotMap.isNotEmpty()) {
                    YuSynAcc.INSTANCE.server.logger.info(
                        "§a成功加载玩家 §b$playerName §a的饰品数据，共 §e${slotMap.size} §a个槽位",
                    )
                } else {
                    YuSynAcc.INSTANCE.server.logger
                        .info("§7玩家 §b$playerName §7暂无饰品数据")
                }

                PlayerData(slotMap)
            }
                ?: run {
                    YuSynAcc.INSTANCE.server.logger.warning(
                        "§c数据库连接失败，无法加载玩家 §e$playerName §c的数据",
                    )
                    PlayerData()
                }
        } catch (e: SQLException) {
            YuSynAcc.INSTANCE.server.logger.warning(
                "§c加载玩家 §e$playerName §c的饰品数据时发生错误: ${e.message}",
            )
            e.printStackTrace()
            PlayerData()
        }

    fun deletePlayerData(playerName: String): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn
                    .prepareStatement("DELETE FROM `player_data` WHERE `player_name` = ?")
                    .use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.executeUpdate() > 0
                    }
            }
                ?: false
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    fun saveSlot(
        playerName: String,
        slot: Slot,
        nbtData: String,
    ): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn
                    .prepareStatement(
                        """
                        INSERT INTO `player_slots` (`player_name`, `slot_name`, `slot_id`, `nbt_data`)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE `nbt_data` = VALUES(`nbt_data`)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.setString(2, slot.name)
                        stmt.setInt(3, slot.id)
                        stmt.setString(4, nbtData)
                        stmt.executeUpdate()
                    }
            }
            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    fun removeSlot(
        playerName: String,
        slot: Slot,
    ): Boolean =
        try {
            ensureConnection()?.use { conn ->
                conn
                    .prepareStatement(
                        "DELETE FROM `player_slots` WHERE `player_name` = ? AND `slot_name` = ? AND `slot_id` = ?",
                    ).use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.setString(2, slot.name)
                        stmt.setInt(3, slot.id)
                        stmt.executeUpdate() > 0
                    }
            }
                ?: false
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }

    fun getPlayerSlots(playerName: String): Map<Slot, String> =
        try {
            ensureConnection()?.use { conn ->
                val slotMap = mutableMapOf<Slot, String>()
                conn
                    .prepareStatement(
                        "SELECT `slot_name`, `slot_id`, `nbt_data` FROM `player_slots` WHERE `player_name` = ?",
                    ).use { stmt ->
                        stmt.setString(1, playerName)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                val slotName = rs.getString("slot_name")
                                val slotId = rs.getInt("slot_id")
                                val nbtData = rs.getString("nbt_data")
                                slotMap[Slot(slotName, slotId)] = nbtData ?: ""
                            }
                        }
                    }
                slotMap
            }
                ?: emptyMap()
        } catch (e: SQLException) {
            e.printStackTrace()
            emptyMap()
        }

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
