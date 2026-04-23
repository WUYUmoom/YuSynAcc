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
        AccessoryChangeCallback.EVENT.register { prevStack, currentStack, reference, stateChange->
            val entity = reference.entity()
            if (entity !is ServerPlayer) return@register
            // 【关键修复1】只处理 REPLACEMENT 类型，避免频繁的 MUTATION 触发数据库保存
            if (stateChange != SlotStateChange.REPLACEMENT) {
                return@register
            }
            var playerdata = PlayerData.getPlayer(entity.name.string)
            val slot = Slot(reference.slotName(), reference.slot())
            if (currentStack.isEmpty) {
                playerdata.removeSlot(slot.name)
            } else {
                val nbt = currentStack.save(entity.registryAccess())
                playerdata.map[slot] = nbt.toString()
            }
            DatabaseManager.savePlayerData(entity.name.toString(), playerdata)
        }
    }

    override fun onDisable() {}
}
