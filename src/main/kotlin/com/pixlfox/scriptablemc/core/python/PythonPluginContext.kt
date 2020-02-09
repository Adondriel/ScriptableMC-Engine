package com.pixlfox.scriptablemc.core.python

import com.pixlfox.scriptablemc.core.ScriptablePluginContext
import com.pixlfox.scriptablemc.core.ScriptablePluginEngine
import org.bukkit.event.HandlerList
import org.bukkit.command.PluginCommand
import org.graalvm.polyglot.Value


@Suppress("MemberVisibilityCanBePrivate", "unused")
class PythonPluginContext(override val engine: ScriptablePluginEngine, override val pluginName: String, override val pluginInstance: Value): ScriptablePluginContext {
    override val commands = mutableListOf<PluginCommand>()

    internal fun load() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Loading python plugin context.")
        }

        pluginInstance.invokeMember("onLoad")
    }

    internal fun enable() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Enabling python plugin context.")
        }

        pluginInstance.invokeMember("onEnable")
    }

    internal fun disable() {
        if(engine.debugEnabled) {
            engine.bootstrapPlugin.logger.info("[$pluginName] Disabling python plugin context.")
        }

        pluginInstance.invokeMember("onDisable")

        HandlerList.unregisterAll(this)

        val commands = commands.toTypedArray()
        for(command in commands) {
            unregisterCommand(command)
        }
    }

    companion object {
        fun newInstance(pluginName: String, engine: ScriptablePluginEngine, pluginInstance: Value): PythonPluginContext {
            if(engine.debugEnabled) {
                engine.bootstrapPlugin.logger.info("[$pluginName] Creating new python plugin context.")
            }

            return PythonPluginContext(
                engine,
                pluginName,
                pluginInstance
            )
        }
    }
}