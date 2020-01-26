package com.pixlfox.scriptablemc.core

import com.pixlfox.scriptablemc.ScriptableBungeePluginMain
import com.pixlfox.scriptablemc.ScriptablePluginMain
import org.bukkit.event.Listener
import org.graalvm.polyglot.*
import java.io.File
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ScriptablePluginEngine(val bootstrapPlugin: ScriptablePluginMain?, val bungeePlugin: ScriptableBungeePluginMain?, val rootServerFolder: String): Listener {
    var debugEnabled: Boolean = false

    private val graalContext: Context = Context
        .newBuilder()
        .allowAllAccess(true)
        .allowExperimentalOptions(true)
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLoading(true)
        .allowIO(true)
        .option("js.ecmascript-version", "2020")
        .build()
    private val jsBindings: Value = graalContext.getBindings("js")
    internal val scriptablePlugins: MutableList<IScriptablePluginContext> = mutableListOf()

    constructor(bootstrapPlugin: ScriptablePluginMain) : this(bootstrapPlugin, null, "./")
    constructor(bungeePlugin: ScriptableBungeePluginMain) : this(null, bungeePlugin, "./")

    internal fun start() {
        instance = this
        jsBindings.putMember("engine", this)

        val mainScriptFile = File("${rootServerFolder}scripts/main.js")
        if(!mainScriptFile.parentFile.exists()) {
            mainScriptFile.parentFile.mkdirs()
        }

        if(mainScriptFile.exists()) {
            eval(
                Source.newBuilder("js", mainScriptFile)
                    .name("main.js")
                    .mimeType("application/javascript+module")
                    .interactive(false)
                    .build()
            )
        }
        else {
            throw ScriptNotFoundException(mainScriptFile)
        }
    }

    internal fun close() {
        instance = null
        for(scriptablePlugin in scriptablePlugins) {
            scriptablePlugin.disable()
        }
        scriptablePlugins.clear()

        graalContext.close(true)
    }

    fun isBungeeServer(): Boolean {
        return bungeePlugin != null
    }

    fun isMinecraftServer(): Boolean {
        return bootstrapPlugin != null
    }

    fun evalFile(filePath: String): Value {
        val scriptFile = File("${rootServerFolder}scripts/$filePath")

        return if(scriptFile.exists()) {
            eval(
                Source.newBuilder("js", scriptFile)
                    .name(scriptFile.name)
                    .mimeType("application/javascript+module")
                    .interactive(false)
                    .build()
            )
        } else {
            throw ScriptNotFoundException(scriptFile)
        }
    }

    fun evalJs(source: String): Value {
        return graalContext.eval(
            Source.newBuilder("js", source,"${UUID.randomUUID()}.js")
            .mimeType("application/javascript+module")
            .interactive(false)
            .cached(false)
            .build()
        )
    }

    fun eval(source: Source): Value {
        return graalContext.eval(source)
    }

    fun loadPlugin(scriptableClass: Value) {
        val pluginInstance = scriptableClass.newInstance()
        val pluginName = pluginInstance.getMember("pluginName").asString()

        when {
            isMinecraftServer() -> {
                val pluginContext = ScriptablePluginContext.newInstance(pluginName, this, pluginInstance)
                pluginInstance.putMember("context", pluginContext)
                scriptablePlugins.add(pluginContext)
                pluginContext.load()
            }
            isBungeeServer() -> {
                val pluginContext = ScriptableBungeePluginContext.newInstance(pluginName, this, pluginInstance)
                pluginInstance.putMember("context", pluginContext)
                scriptablePlugins.add(pluginContext)
                pluginContext.load()
            }
            else -> {
                throw java.lang.Exception("Unable to create plugin context.")
            }
        }
    }

    fun enableAllPlugins() {
        for (pluginContext in scriptablePlugins) {
            pluginContext.enable()
        }
    }

    fun enablePlugin(pluginContext: ScriptablePluginContext) {
        pluginContext.enable()
    }

    fun disablePlugin(pluginContext: ScriptablePluginContext) {
        pluginContext.disable()
    }

    companion object {
        private var inst: ScriptablePluginEngine? = null
        var instance: ScriptablePluginEngine?
            internal set(value) { inst = value }
            get() { return inst }
    }
}

class ScriptNotFoundException(scriptFile: File) : Exception("Unable to load script: ${scriptFile.path}.")