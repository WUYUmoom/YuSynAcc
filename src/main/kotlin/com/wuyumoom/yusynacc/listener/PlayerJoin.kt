package com.wuyumoom.yusynacc.listener

import com.wuyumoom.yusynacc.YuSynAcc
import com.wuyumoom.yusynacc.data.PlayerData
import io.wispforest.accessories.api.AccessoriesCapability
import io.wispforest.accessories.api.slot.SlotReference
import net.minecraft.nbt.TagParser
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoin : Listener {
    // 玩家进服务器事件
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = YuSynAcc.playerList.getPlayer(event.player.uniqueId)
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage("未找到玩家停止同步")
            return
        }
        clearAllAccessories(player)
        val playerData = PlayerData.getPlayer(player.name.string)
        val capability = AccessoriesCapability.get(player)
        if (capability != null) {
            Bukkit.getConsoleSender().sendMessage("玩家开始同步")
            playerData.map.forEach { (slot, string) ->
                val reference = SlotReference.of(player, slot.name, slot.id)
                val container = reference.slotContainer()
                if (container != null) {
                    val nbt = TagParser.parseTag(string)
                    val parse =
                        ItemStack.parse(player.registryAccess(), nbt).orElse(ItemStack.EMPTY)
                    if (parse == null) {
                        Bukkit.getConsoleSender().sendMessage("nbt解析失败")
                        return@forEach
                    }
                    Bukkit.getConsoleSender().sendMessage("设置${slot.name}:${slot.id}")
                    reference.setStack(parse)
                }
            }
        } else {
            Bukkit.getConsoleSender().sendMessage("未找到饰品界面")
        }
    }

    // 清除玩家饰品
    private fun clearAllAccessories(player: ServerPlayer) {
        val capability = AccessoriesCapability.get(player)
        if (capability != null) {
            capability.reset(false)
            if (player.containerMenu != null) {
                player.containerMenu.broadcastChanges()
            }
            Bukkit.getConsoleSender().sendMessage("清理完成")
        } else {
            Bukkit.getConsoleSender().sendMessage("未找到饰品界面")
        }
    }
}
