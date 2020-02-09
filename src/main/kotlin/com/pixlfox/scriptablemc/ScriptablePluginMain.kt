package com.pixlfox.scriptablemc

import co.aikar.commands.PaperCommandManager
import com.pixlfox.scriptablemc.core.ScriptablePluginEngine
import com.pixlfox.scriptablemc.core.js.JavaScriptPluginEngine
import com.pixlfox.scriptablemc.core.python.PythonPluginEngine
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin


@Suppress("unused", "MemberVisibilityCanBePrivate")
class ScriptablePluginMain : JavaPlugin(), Listener {
    internal var jsPluginEngine: ScriptablePluginEngine? = null
    internal var pythonPluginEngine: ScriptablePluginEngine? = null
    private var commandManager: PaperCommandManager? = null

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        commandManager = PaperCommandManager(this)
        commandManager?.registerCommand(ScriptablePluginCommand(this))
        commandManager?.registerCommand(ScriptablePluginJavaScriptCommand(this))
        commandManager?.registerCommand(ScriptablePluginPythonCommand(this))

        patchClassLoader {
            saveDefaultConfig()

            try {
                jsPluginEngine = JavaScriptPluginEngine(
                    this,
                    config.getString("root_scripts_folder", "./scripts").orEmpty(),
                    config.getBoolean("debug", false),
                    config.getBoolean("extract_libs", true)
                )
                jsPluginEngine!!.start()
                logger.info("JavaScript plugin engine started.")
            } catch (e: IllegalStateException) {
                if (e.message?.contains("Make sure the truffle-api.jar is on the classpath.", true) == true) {
                    logger.warning("JavaScript plugin engine failed to start.")
                    e.printStackTrace()
                    logger.severe("It looks like you're trying to run this server with the standard JRE. ScriptableMC only works with the GraalVM java runtime or JDK.")
                } else {
                    logger.warning("JavaScript plugin engine failed to start.")
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                logger.warning("JavaScript plugin engine failed to start.")
                e.printStackTrace()
            }

            try {
                pythonPluginEngine = PythonPluginEngine(
                    this,
                    config.getString("root_scripts_folder", "./scripts").orEmpty(),
                    config.getBoolean("debug", false),
                    config.getBoolean("extract_libs", true)
                )
                pythonPluginEngine!!.start()
                logger.info("Python plugin engine started.")
            } catch (e: IllegalStateException) {
                if (e.message?.contains("Make sure the truffle-api.jar is on the classpath.", true) == true) {
                    logger.warning("Python plugin engine failed to start.")
                    e.printStackTrace()
                    logger.severe("It looks like you're trying to run this server with the standard JRE. ScriptableMC only works with the GraalVM java runtime or JDK.")
                } else {
                    logger.warning("Python plugin engine failed to start.")
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                logger.warning("Python plugin engine failed to start.")
                e.printStackTrace()
            }
        }
    }

    override fun onDisable() {
        patchClassLoader {
            try {
                if(jsPluginEngine != null) {
                    jsPluginEngine!!.close()
                    jsPluginEngine = null
                    logger.info("JavaScript plugin engine shutdown.")
                }
            } catch (e: Exception) {
                logger.warning("JavaScript plugin engine failed to shutdown.")
                e.printStackTrace()
            }

            try {
                if(pythonPluginEngine != null) {
                    pythonPluginEngine!!.close()
                    pythonPluginEngine = null
                    logger.info("Python plugin engine shutdown.")
                }
            } catch (e: Exception) {
                logger.warning("Python plugin engine failed to shutdown.")
                e.printStackTrace()
            }
        }
    }

    /**
     * Patches the bukkit class loader to allow for GraalVM class loading from inside plugin jar.
     * A bit hackish but it works.
     * https://stackoverflow.com/questions/56712178/graalvm-no-language-and-polyglot-implementation-was-found-on-the-classpath
     */
    internal fun patchClassLoader(callback: () -> Unit) {
        val oldCl = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = javaClass.classLoader
        callback()
        Thread.currentThread().contextClassLoader = oldCl
    }

    companion object {
        private var inst: ScriptablePluginMain? = null
        var instance: ScriptablePluginMain
            private set(value) { inst = value }
            get() { return inst!! }
    }
}