package com.pixlfox.scriptablemc

import com.pixlfox.scriptablemc.core.ScriptablePluginEngine
import net.md_5.bungee.api.plugin.Plugin

@Suppress("unused")
class ScriptableBungeePluginMain : Plugin() {
    private var scriptEngine: ScriptablePluginEngine? = null
    internal val mainThread: Thread = Thread.currentThread()

    override fun onLoad() {

    }

    override fun onEnable() {
        runInPluginContext {
            try {
                scriptEngine = ScriptablePluginEngine(this)
                scriptEngine!!.start()
                logger.info("Scriptable plugin engine started.")
            } catch (e: IllegalStateException) {
                if (e.message?.contains("Make sure the truffle-api.jar is on the classpath.", true) == true) {
                    logger.warning("Scriptable plugin engine failed to start.")
                    e.printStackTrace()
                    logger.severe("It looks like you're trying to run this server with the standard java runtime. ScriptableMC only works with OpenJDK or the GraalVM java runtime.")
                } else {
                    logger.warning("Scriptable plugin engine failed to start.")
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                logger.warning("Scriptable plugin engine failed to start.")
                e.printStackTrace()
            }
        }
    }

    override fun onDisable() {
        runInPluginContext {
            try {
                scriptEngine!!.close()
                logger.info("Scriptable plugin engine shutdown.")
            } catch (e: Exception) {
                logger.warning("Scriptable plugin engine failed to shutdown.")
                e.printStackTrace()
            }
        }
    }

    private fun runInPluginContext(callback: () -> Unit) {
        val oldCl = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = javaClass.classLoader
        callback()
        Thread.currentThread().contextClassLoader = oldCl
    }
}