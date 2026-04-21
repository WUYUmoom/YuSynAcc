package com.wuyumoom.yusynacc.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object PluginEvent {
    // 进服事件
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
    }

    // 退出服务器事件
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
    }
}
