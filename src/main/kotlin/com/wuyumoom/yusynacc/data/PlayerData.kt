package com.wuyumoom.yusynacc.data

import com.wuyumoom.yusynacc.database.DatabaseManager
import org.bukkit.Bukkit

class PlayerData(
    var map: MutableMap<Slot, String> = mutableMapOf(),
) {
    companion object {
        // 使用 ConcurrentHashMap 的 KeySetView，确保线程安全
        private val syncingPlayers: java.util.concurrent.ConcurrentHashMap.KeySetView<String, Boolean> =
            java.util.concurrent.ConcurrentHashMap
                .newKeySet()

        fun markSyncing(player: String) {
            syncingPlayers.add(player)
        }

        fun markSynced(player: String) {
            val removed = syncingPlayers.remove(player)

            if (!removed) {
                Bukkit.getConsoleSender().sendMessage("§c[DEBUG] 删除失败！当前集合中的标记: $syncingPlayers")
            }
        }

        fun isSyncing(player: String): Boolean {
            val isSyncing = syncingPlayers.contains(player)
            // 如果正在同步，打印日志方便观察
            return isSyncing
        }

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
