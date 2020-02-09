package com.pixlfox.scriptablemc.core.js

import com.pixlfox.scriptablemc.core.ScriptablePluginContext
import com.pixlfox.scriptablemc.core.ScriptablePluginEngine
import com.pixlfox.scriptablemc.core.exceptions.ScriptNotFoundException
import com.pixlfox.scriptablemc.utils.UnzipUtility
import fr.minuskube.inv.InventoryManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.graalvm.polyglot.*
import java.io.File
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


@Suppress("MemberVisibilityCanBePrivate", "unused")
class JavaScriptPluginEngine(override val bootstrapPlugin: JavaPlugin, override val rootScriptsFolder: String = "./scripts", override val debugEnabled: Boolean = false, override val extractLibs: Boolean = true): ScriptablePluginEngine {
    override val graalContext: Context = Context
        .newBuilder("js")
        .allowAllAccess(true)
        .allowExperimentalOptions(true)
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLoading(true)
        .allowIO(true)
        .option("js.ecmascript-version", "2020")
        .build()
    override val globalBindings: Value = graalContext.getBindings("js")

    internal val javaScriptPlugins: MutableList<JavaScriptPluginContext> = mutableListOf()
    internal val inventoryManager: InventoryManager = InventoryManager(bootstrapPlugin)
    private var enabledAllPlugins: Boolean = false

    override fun start() {
        instance = this
        inventoryManager.init()
        globalBindings.putMember("engine", this)

        val mainScriptFile = File("${rootScriptsFolder}/main.js")
        if(!mainScriptFile.parentFile.exists()) {
            mainScriptFile.parentFile.mkdirs()
        }

        if(extractLibs) {
            val librariesResource = bootstrapPlugin.getResource("js-libraries.zip")
            if (librariesResource != null) {
                UnzipUtility.unzip(librariesResource, "${rootScriptsFolder}/lib")
            }
        }

        if(mainScriptFile.exists()) {
            val pluginTypes = eval(
                Source.newBuilder("js", mainScriptFile)
                    .name("main.js")
                    .mimeType("application/javascript+module")
                    .interactive(false)
                    .build()
            )

            // Load all plugin types returned as an array
            if(pluginTypes.hasArrayElements()) {
                for (i in 0 until pluginTypes.arraySize) {
                    this.loadPlugin(pluginTypes.getArrayElement(i))
                }
            }

            // Enable all plugins if not already enabled
            if(!enabledAllPlugins) {
                enableAllPlugins()
            }
        }
        else {
            throw ScriptNotFoundException(mainScriptFile)
        }
    }

    override fun close() {
        instance = null
        for(scriptablePlugin in javaScriptPlugins) {
            scriptablePlugin.disable()
        }
        javaScriptPlugins.clear()

        graalContext.close(true)
    }

    override fun evalFile(filePath: String): Value {
        val scriptFile = File("${rootScriptsFolder}/$filePath")

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

    override fun evalFile(scriptFile: File): Value {
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

    override fun evalCommandSender(source: String, sender: CommandSender): Value {
        val tempScriptFile = File("${rootScriptsFolder}/${UUID.randomUUID()}.js")
        try {
            tempScriptFile.writeText("import * as lib from './lib/global.js';\n" +
                    "new (class EvalCommandSenderContext {\n" +
                    "    run(sender, server, servicesManager) {\n" +
                    "        $source\n" +
                    "    }\n" +
                    "})()\n")
            val evalCommandSenderContext = evalFile(tempScriptFile)

            evalCommandSenderContext.putMember("sender", sender)
            evalCommandSenderContext.putMember("server", Bukkit.getServer())
            evalCommandSenderContext.putMember("servicesManager", Bukkit.getServicesManager())
            return evalCommandSenderContext.invokeMember("run", sender, Bukkit.getServer(), Bukkit.getServicesManager())
        }
        finally {
            tempScriptFile.delete()
        }
    }

    override fun eval(source: String): Value {
        return eval(
            Source.newBuilder("js", source,"${UUID.randomUUID()}.js")
                .mimeType("application/javascript+module")
                .interactive(false)
                .cached(false)
                .build()
        )
    }

    override fun loadPlugin(scriptableClass: Value): ScriptablePluginContext {
        val pluginInstance = scriptableClass.newInstance()
        val pluginName = pluginInstance.getMember("pluginName").asString()
        val pluginContext = JavaScriptPluginContext.newInstance(pluginName, this, pluginInstance)
        pluginInstance.putMember("context", pluginContext)
        javaScriptPlugins.add(pluginContext)
        pluginContext.load()
        return pluginContext
    }

    override fun enableAllPlugins() {
        for (pluginContext in javaScriptPlugins) {
            pluginContext.enable()
        }
        enabledAllPlugins = true
    }

    override fun enablePlugin(pluginContext: ScriptablePluginContext) {
        if(pluginContext is JavaScriptPluginContext) {
            pluginContext.enable()
        }
    }

    override fun disablePlugin(pluginContext: ScriptablePluginContext) {
        if(pluginContext is JavaScriptPluginContext) {
            pluginContext.disable()
        }
    }

    companion object {
        private var inst: JavaScriptPluginEngine? = null
        var instance: JavaScriptPluginEngine?
            internal set(value) { inst = value }
            get() { return inst }
    }
}