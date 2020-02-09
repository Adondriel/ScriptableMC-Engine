package com.pixlfox.scriptablemc.core

import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.File

interface ScriptablePluginEngine {
    val bootstrapPlugin: JavaPlugin
    val rootScriptsFolder: String
    val debugEnabled: Boolean
    val extractLibs: Boolean

    val graalContext: Context
    val globalBindings: Value

    fun start()

    fun close()

    fun eval(source: Source): Value {
        return graalContext.eval(source)
    }

    fun eval(source: String): Value

    fun evalCommandSender(source: String, sender: CommandSender): Value

    fun evalFile(filePath: String): Value

    fun evalFile(scriptFile: File): Value

    fun loadPlugin(scriptableClass: Value): ScriptablePluginContext

    fun enableAllPlugins()

    fun enablePlugin(pluginContext: ScriptablePluginContext)

    fun disablePlugin(pluginContext: ScriptablePluginContext)
}