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
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        PlayerData.markSynced(event.player.name)
    }

    // 玩家进服务器事件
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = YuSynAcc.playerList.getPlayer(event.player.uniqueId)
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage("未找到玩家停止同步")
            return
        }
        // 标记玩家正在同步，阻止回调触发保存
        PlayerData.markSyncing(player.name.string)
        try {
            val playerData = PlayerData.getPlayer(player.name.string)
            val capability = AccessoriesCapability.get(player)
            if (capability != null) {
                capability.clearSlotModifiers()
                playerData.map.forEach { (slot, string) ->
                    val reference = SlotReference.of(player, slot.name, slot.id)
                    val container = reference.slotContainer()
                    if (container != null) {
                        val nbt = TagParser.parseTag(string)
                        val parse =
                            ItemStack
                                .parse(player.registryAccess(), nbt)
                                .orElse(ItemStack.EMPTY)
                        if (parse == null) {
                            Bukkit.getConsoleSender().sendMessage("nbt解析失败")
                            return@forEach
                        }
                        reference.setStack(parse)
                    }
                }
                // 标记所有容器需要更新
                capability.getContainers().values.forEach { container -> container.markChanged() }

                // 触发容器更新（会在下一个 tick 自动执行）
                capability.updateContainers()

                // 手动广播变化到客户端
                player.containerMenu.broadcastChanges()
            } else {
                Bukkit.getConsoleSender().sendMessage("未找到饰品界面")
            }
        } finally {
            // 同步完成后，允许回调触发保存
            PlayerData.markSynced(player.name.toString())
        }
    }
}
