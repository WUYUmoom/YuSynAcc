package com.wuyumoom.yusynacc.data

import com.wuyumoom.yusynacc.database.DatabaseManager
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

class PlayerData(
    var map: MutableMap<Slot, String> = mutableMapOf(),
) {
    companion object {
        // 获取
        fun getPlayer(player: String): PlayerData = DatabaseManager.loadPlayerData(player)
    }

    // 删除位置
    fun removeSlot(name: String) {
        val slot = getSlot(name)
        if (slot != null) {
            map.remove(slot)
            Bukkit.getConsoleSender().sendMessage("删除slot完成")
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
