package com.wuyumoom.yusynacc

import io.wispforest.accessories.api.events.AccessoryChangeCallback
import java.io.File
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import com.wuyumoom.yusynacc.listener.PlayerJoin
class YuSynAcc : JavaPlugin() {
    companion object {
        var map: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
        lateinit var pluginFile: File
        lateinit var INSTANCE: YuSynAcc
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
        iniEvent()
        Bukkit.getPluginManager().registerEvents(PlayerJoin(), INSTANCE)
        Bukkit.getConsoleSender().sendMessage(*LOGO)
    }

    private fun iniEvent() {
        AccessoryChangeCallback.EVENT.register { stack, otherStack, reference, stateChange ->
            val entity = reference.entity()
            if (entity !is ServerPlayer) return@register
            var playerdata = map.getOrDefault(entity.name.string, mutableMapOf())
            val nbt = stack.save(entity.registryAccess())
            when {
                // 情况 A：之前是空的，现在有物品了 -> 戴上 (Equip)
                otherStack.isEmpty && !stack.isEmpty -> {
                    println("玩家戴上了: ${stack.displayName.string}")
                    playerdata[reference.slotName()] = nbt.toString()
                    return@register
                }

                // 情况 B：之前有物品，现在空了 -> 取下 (Unequip)
                !otherStack.isEmpty && stack.isEmpty -> {
                    println("玩家取下了: ${otherStack.displayName.string}")
                    return@register
                }

                // 情况 C：之前有，现在也有（但物品变了） -> 替换 (Replace)
                // 对应枚举中的 REPLACEMENT
                !otherStack.isEmpty && !stack.isEmpty -> {
                    println(
                            "玩家替换了: ${otherStack.displayName.string} -> ${stack.displayName.string}"
                    )
                    return@register
                }

                // 情况 D：物品没变，但 NBT 或数量变了 -> 变更 (Mutation)
                // 对应枚举中的 MUTATION
                else -> {
                    // println("饰品发生了变动 (NBT 或数量)")
                }
            }
        }
    }

    override fun onDisable() {}
}
