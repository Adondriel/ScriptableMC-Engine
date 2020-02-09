package com.pixlfox.scriptablemc.core.js

import com.pixlfox.scriptablemc.core.ScriptablePluginContext
import com.pixlfox.scriptablemc.core.ScriptablePluginEngine
import org.bukkit.event.HandlerList
import org.bukkit.command.PluginCommand
import org.graalvm.polyglot.Value


@Suppress("MemberVisibilityCanBePrivate", "unused")
class JavaScriptPluginContext(override val engine: ScriptablePluginEngine, override val pluginName: String, override val pluginInstance: Value): ScriptablePluginContext {
    override val commands = mutableListOf<PluginCommand>()

    internal fun load() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Loading javascript plugin context.")
        }

        pluginInstance.invokeMember("onLoad")
    }

    internal fun enable() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Enabling javascript plugin context.")
        }

        pluginInstance.invokeMember("onEnable")
    }

    internal fun disable() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Disabling javascript plugin context.")
        }

        pluginInstance.invokeMember("onDisable")

        HandlerList.unregisterAll(this)

        val commands = commands.toTypedArray()
        for(command in commands) {
            unregisterCommand(command)
        }
    }

    companion object {
        fun newInstance(pluginName: String, engine: ScriptablePluginEngine, pluginInstance: Value): JavaScriptPluginContext {
            if(engine.debugEnabled) {
                engine.bootstrapPlugin.logger.info("[$pluginName] Creating new scriptable plugin context.")
            }

            return JavaScriptPluginContext(
                engine,
                pluginName,
                pluginInstance
            )
        }
    }
}