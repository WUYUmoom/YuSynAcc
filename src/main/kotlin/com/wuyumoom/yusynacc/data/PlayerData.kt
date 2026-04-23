package com.wuyumoom.yusynacc.data

import com.wuyumoom.yusynacc.database.DatabaseManager
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

class PlayerData(
    var map: MutableMap<Slot, String> = mutableMapOf(),
) {
    companion object {
        // 记录正在初始化的玩家（同步期间不触发回调保存）
        private val syncingPlayers: MutableSet<String> = mutableSetOf()

        fun markSyncing(player: String) {
            syncingPlayers.add(player)
        }

        fun markSynced(player: String) {
            syncingPlayers.remove(player)
            Bukkit.getConsoleSender().sendMessage("玩家同步标签删除")
        }

        fun isSyncing(player: String): Boolean = syncingPlayers.contains(player)

        fun clearCache(player: String) {
            syncingPlayers.remove(player)
        }

        fun clearAllCache() {
            syncingPlayers.clear()
        }

        // 获取
        fun getPlayer(player: String): PlayerData = DatabaseManager.loadPlayerData(player)
    }

    // 删除位置
    fun removeSlot(name: String) {
        val slot = getSlot(name)
        if (slot != null) {
            map.remove(slot)
        }
    }

    private fun getSlot(name: String): Slot? {
        map.forEach { (slot, string) ->
            if (slot.name == name) {
                return slot
            }
        }
        return null
    }
}
