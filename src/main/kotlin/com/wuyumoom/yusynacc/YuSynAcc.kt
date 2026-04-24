package com.wuyumoom.yusynacc

import com.wuyumoom.yusynacc.config.ConfigManager
import com.wuyumoom.yusynacc.data.PlayerData
import com.wuyumoom.yusynacc.data.Slot
import com.wuyumoom.yusynacc.database.DatabaseManager
import com.wuyumoom.yusynacc.listener.PlayerJoin
import io.wispforest.accessories.api.events.AccessoryChangeCallback
import io.wispforest.accessories.api.events.SlotStateChange
import java.io.File
import net.minecraft.server.dedicated.DedicatedPlayerList
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_21_R1.CraftServer
import org.bukkit.plugin.java.JavaPlugin

class YuSynAcc : JavaPlugin() {
    companion object {
        lateinit var pluginFile: File
        lateinit var INSTANCE: YuSynAcc
        lateinit var playerList: DedicatedPlayerList
        val LOGO =
                arrayOf(
                        "===============================================================================",
                        "§f██╗   ██╗██╗   ██╗███████╗██╗   ██╗███╗   ██╗ █████╗  ██████╗ ██████╗",
                        "§f╚██╗ ██╔╝██║   ██║██╔════╝╚██╗ ██╔╝████╗  ██║██╔══██╗██╔════╝██╔════╝",
                        "§f ╚████╔╝ ██║   ██║███████╗ ╚████╔╝ ██╔██╗ ██║███████║██║     ██║     ",
                        "§f  ╚██╔╝  ██║   ██║╚════██║  ╚██╔╝  ██║╚██╗██║██╔══██║██║     ██║     ",
                        "§f   ██║   ╚██████╔╝███████║   ██║   ██║ ╚████║██║  ██║╚██████╗╚██████╗",
                        "§f   ╚═╝    ╚═════╝ ╚══════╝   ╚═╝   ╚═╝  ╚═══╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝",
                        "§e§l语之饰品同步 §6§l启动完成！",
                        "§e§l作者 : 姬无语 §6§lQQ1841375451",
                        "==============================================================================="
                )
    }

    override fun onEnable() {
        INSTANCE = this
        saveDefaultConfig()
        ConfigManager.load()
        val craftServer: CraftServer = Bukkit.getServer() as CraftServer
        playerList = craftServer.server.playerList
        DatabaseManager.connect()
        if (DatabaseManager.isConnected()) {
            server.logger.info("§a数据库连接成功")
        }
        iniEvent()
        Bukkit.getPluginManager().registerEvents(PlayerJoin(), INSTANCE)
        Bukkit.getConsoleSender().sendMessage(*LOGO)
    }

     private fun iniEvent() {
        AccessoryChangeCallback.EVENT.register { stack, otherStack, reference, stateChange ->
            val entity = reference.entity()
            if (entity !is ServerPlayer) return@register

            val playerName = entity.name.string

            if (PlayerData.isSyncing(playerName)) {
                return@register
            }

            var playerdata = PlayerData.getPlayer(playerName)
            val slot = Slot(reference.slotName(), reference.slot())

            when {
                // stack 是旧物品，otherStack 是新物品
                // 旧空 + 新有 = 佩戴
                stack.isEmpty && !otherStack.isEmpty -> {
                    val nbt = otherStack.save(entity.registryAccess())
                    playerdata.map[slot] = nbt.toString()
                }

                // 旧有 + 新空 = 取下
                !stack.isEmpty && otherStack.isEmpty -> {
                    playerdata.map.remove(slot)
                }

                // 旧有 + 新有 = 替换
                !stack.isEmpty && !otherStack.isEmpty -> {
                    val nbt = otherStack.save(entity.registryAccess())
                    playerdata.map[slot] = nbt.toString()
                }

                else -> {
                    server.logger.warning("§c玩家 §e$playerName §c饰品状态异常")
                    return@register
                }
            }

            val success = DatabaseManager.savePlayerData(playerName, playerdata)
            if (!success) {
                server.logger.warning("§c数据保存失败: §e$playerName")
            }
        }
    }

    override fun onDisable() {}
}
