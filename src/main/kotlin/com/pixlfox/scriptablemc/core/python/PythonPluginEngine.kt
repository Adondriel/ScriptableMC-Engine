package com.pixlfox.scriptablemc.core.python

import com.pixlfox.scriptablemc.core.ScriptablePluginContext
import com.pixlfox.scriptablemc.core.ScriptablePluginEngine
import com.pixlfox.scriptablemc.core.exceptions.ScriptNotFoundException
import com.pixlfox.scriptablemc.utils.UnzipUtility
import fr.minuskube.inv.InventoryManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.event.Listener
import org.graalvm.polyglot.*
import java.io.File
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


@Suppress("MemberVisibilityCanBePrivate", "unused")
class PythonPluginEngine(override val bootstrapPlugin: JavaPlugin, override val rootScriptsFolder: String = "./scripts", override val debugEnabled: Boolean = false, override val extractLibs: Boolean = true) : ScriptablePluginEngine {
    override val graalContext: Context = Context
        .newBuilder("python")
        .allowAllAccess(true)
        .allowExperimentalOptions(true)
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLoading(true)
        .allowIO(true)
        .option("python.CoreHome", "$rootScriptsFolder/python-core/lib-graalpython/")
        .option("python.StdLibHome", "$rootScriptsFolder/python-core/lib-python/3/")
        .build()
    override val globalBindings: Value = graalContext.getBindings("python")

    internal val pythonPlugins: MutableList<PythonPluginContext> = mutableListOf()
    internal val inventoryManager: InventoryManager = InventoryManager(bootstrapPlugin)
    private var enabledAllPlugins: Boolean = false

    override fun start() {
        instance = this
        inventoryManager.init()
        globalBindings.putMember("engine", this)

        val mainScriptFile = File("${rootScriptsFolder}/main.py")
        if(!mainScriptFile.parentFile.exists()) {
            mainScriptFile.parentFile.mkdirs()
        }

        if(extractLibs) {
            val librariesResource = bootstrapPlugin.getResource("python-libraries.zip")
            if (librariesResource != null) {
                UnzipUtility.unzip(librariesResource, "${rootScriptsFolder}/lib")
            }
        }

        if(mainScriptFile.exists()) {
            val pluginTypes = eval(
                Source.newBuilder("python", mainScriptFile)
                    .name("main.py")
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
        for(pythonPlugin in pythonPlugins) {
            pythonPlugin.disable()
        }
        pythonPlugins.clear()

        graalContext.close(true)
    }

    override fun evalFile(filePath: String): Value {
        val scriptFile = File("${rootScriptsFolder}/$filePath")

        return if(scriptFile.exists()) {
            eval(
                Source.newBuilder("python", scriptFile)
                    .name(scriptFile.name)
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
                Source.newBuilder("python", scriptFile)
                    .name(scriptFile.name)
                    .interactive(false)
                    .build()
            )
        } else {
            throw ScriptNotFoundException(scriptFile)
        }
    }

    override fun eval(source: String): Value {
        return graalContext.eval(
            Source.newBuilder("python", source,"${UUID.randomUUID()}.py")
                .interactive(false)
                .cached(false)
                .build()
        )
    }

    override fun evalCommandSender(source: String, sender: CommandSender): Value {
        val tempScriptFile = File("${rootScriptsFolder}/${UUID.randomUUID()}/__init__.py")
        try {
            tempScriptFile.parentFile.mkdirs()
            tempScriptFile.writeText(source)
            return evalFile(tempScriptFile)
        }
        finally {
            tempScriptFile.delete()
            tempScriptFile.parentFile.delete()
        }
    }

    override fun eval(source: Source): Value {
        return graalContext.eval(source)
    }

    override fun loadPlugin(scriptableClass: Value): PythonPluginContext {
        val pluginInstance = scriptableClass.newInstance()
        val pluginName = pluginInstance.getMember("pluginName").asString()
        val pluginContext = PythonPluginContext.newInstance(pluginName, this, pluginInstance)
        pluginInstance.putMember("context", pluginContext)
        pythonPlugins.add(pluginContext)
        pluginContext.load()
        return pluginContext
    }

    override fun enableAllPlugins() {
        for (pluginContext in pythonPlugins) {
            pluginContext.enable()
        }
        enabledAllPlugins = true
    }

    override fun enablePlugin(pluginContext: ScriptablePluginContext) {
        if(pluginContext is PythonPluginContext) {
            pluginContext.enable()
        }
    }

    override fun disablePlugin(pluginContext: ScriptablePluginContext) {
        if(pluginContext is PythonPluginContext) {
            pluginContext.disable()
        }
    }

    companion object {
        private var inst: ScriptablePluginEngine? = null
        var instance: ScriptablePluginEngine?
            internal set(value) { inst = value }
            get() { return inst
            }
    }
}