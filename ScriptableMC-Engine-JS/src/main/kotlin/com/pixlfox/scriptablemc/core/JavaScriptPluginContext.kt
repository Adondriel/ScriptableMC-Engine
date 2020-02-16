package com.pixlfox.scriptablemc.core

import com.smc.version.Version
import fr.minuskube.inv.InventoryManager
import org.bukkit.event.HandlerList
import org.graalvm.polyglot.Value


@Suppress("MemberVisibilityCanBePrivate", "unused")
class JavaScriptPluginContext(override val engine: ScriptablePluginEngine, override val pluginName: String, override val pluginInstance: Value) : ScriptablePluginContext() {
    override val inventoryManager: InventoryManager
        get() = engine.inventoryManager

    override val pluginVersion: Version
        get() = engine.pluginVersion

    override fun load() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Loading JavaScript plugin context.")
        }

        pluginInstance.invokeMember("onLoad")
    }

    override fun enable() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Enabling JavaScript plugin context.")
        }

        pluginInstance.invokeMember("onEnable")
    }

    override fun disable() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Disabling JavaScript plugin context.")
        }

        pluginInstance.invokeMember("onDisable")

        HandlerList.unregisterAll(this)

        val commands = commands.toTypedArray()
        for(command in commands) {
            unregisterCommand(command)
        }
    }

    companion object {
        fun newInstance(pluginName: String, engine: ScriptablePluginEngine, pluginInstance: Value): ScriptablePluginContext {
            if(engine.debugEnabled) {
                engine.bootstrapPlugin.logger.info("[$pluginName] Creating new JavaScript plugin context.")
            }

            return JavaScriptPluginContext(engine, pluginName, pluginInstance)
        }
    }
}