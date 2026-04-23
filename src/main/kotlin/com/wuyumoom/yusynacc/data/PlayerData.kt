package com.wuyumoom.yusynacc.data

import java.util.concurrent.ConcurrentHashMap

class PlayerData(
    var map: MutableMap<Slot, String> = mutableMapOf(),
) {
    companion object {
        private val cache: ConcurrentHashMap<String, PlayerData> = ConcurrentHashMap()

        // 获取
        fun getPlayer(player: String): PlayerData = cache.getOrPut(player) { loadData(player) }

        private fun loadData(player: String): PlayerData = PlayerData()
    }
}
