package com.pixlfox.scriptablemc.smartinvs

import com.pixlfox.scriptablemc.core.js.JavaScriptPluginEngine
import fr.minuskube.inv.content.InventoryContents
import fr.minuskube.inv.content.InventoryProvider
import org.bukkit.entity.Player
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import fr.minuskube.inv.ClickableItem
import fr.minuskube.inv.SmartInventory
import org.bukkit.Bukkit
import org.bukkit.ChatColor


@Suppress("unused")
class MainMenu(private val pluginEngine: JavaScriptPluginEngine) : InventoryProvider {
    override fun init(player: Player?, contents: InventoryContents?) {
        if(contents != null && player != null) {
            contents.fillBorders(ClickableItem.empty(ItemStack(Material.BLACK_CONCRETE)))

            if(player.hasPermission("scriptablemc.reload")) {
                val reloadItemStack = ItemStack(Material.GOLD_NUGGET)
                val reloadItemMeta = reloadItemStack.itemMeta
                reloadItemMeta?.setDisplayName("${ChatColor.DARK_AQUA}Reload Script Engine")
                reloadItemStack.itemMeta = reloadItemMeta
                contents.set(1, 1, ClickableItem.of(reloadItemStack) {
                    Bukkit.getServer().dispatchCommand(player, "scriptablemc reload")
                })
            }

            if(player.hasPermission("scriptablemc.info")) {
                val pluginsItemStack = ItemStack(Material.IRON_NUGGET)
                val pluginsItemMeta = pluginsItemStack.itemMeta
                pluginsItemMeta?.setDisplayName("${ChatColor.GREEN}Print ScriptableMC Info")
                pluginsItemStack.itemMeta = pluginsItemMeta
                contents.set(1, 2, ClickableItem.of(pluginsItemStack) {
                    Bukkit.getServer().dispatchCommand(player, "scriptablemc info")
                    player.closeInventory()
                })
            }

            val closeItemStack = ItemStack(Material.BARRIER)
            val closeItemMeta = closeItemStack.itemMeta
            closeItemMeta?.setDisplayName("${ChatColor.RED}Close Menu")
            closeItemStack.itemMeta = closeItemMeta
            contents.set(1, 7, ClickableItem.of(closeItemStack) {
                player.closeInventory()
            })
        }
    }

    override fun update(player: Player?, contents: InventoryContents?) {}

    companion object {
        val INVENTORY: SmartInventory
            get() = SmartInventory.builder()
                    .id("spm.mainmenu")
                    .manager(JavaScriptPluginEngine.instance!!.inventoryManager)
                    .provider(MainMenu(JavaScriptPluginEngine.instance!!))
                    .size(3, 9)
                    .title(ChatColor.LIGHT_PURPLE.toString() + "ScriptableMC | Main Menu")
                    .closeable(true)
                    .build()
    }
}

//class PluginsMenu(private val pluginEngine: ScriptablePluginEngine) : InventoryProvider {
//    override fun init(player: Player?, contents: InventoryContents?) {
//        if(contents != null) {
//            val items = mutableListOf<ClickableItem>()
//            val pagination = contents.pagination()
//
//            for (plugin in pluginEngine.scriptablePlugins) {
//                val itemStack = ItemStack(Material.WHITE_CONCRETE)
//                val itemMeta = itemStack.itemMeta
//                itemMeta?.setDisplayName("${ChatColor.DARK_AQUA}${plugin.pluginName}")
//                itemStack.itemMeta = itemMeta
//                items.add(ClickableItem.of(itemStack) {
//                    player?.sendMessage("NYI")
//                })
//            }
//
//            pagination.setItems(*items.toTypedArray())
//            pagination.setItemsPerPage(21)
//
//            pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1))
//
//            contents.set(5, 3, ClickableItem.of(ItemStack(Material.ARROW)) {
//                INVENTORY.open(player, pagination.previous().page)
//            })
//            contents.set(5, 5, ClickableItem.of(ItemStack(Material.ARROW)) {
//                INVENTORY.open(player, pagination.next().page)
//            })
//        }
//    }
//
//    override fun update(player: Player?, contents: InventoryContents?) {}
//
//    companion object {
//        val INVENTORY: SmartInventory
//            get() = SmartInventory.builder()
//                .id("spm.pluginsmenu")
//                .provider(PluginsMenu(ScriptablePluginEngine.instance!!))
//                .size(6, 9)
//                .title(ChatColor.LIGHT_PURPLE.toString() + "Scriptable Plugin | Plugins")
//                .closeable(true)
//                .build()
//    }
//}