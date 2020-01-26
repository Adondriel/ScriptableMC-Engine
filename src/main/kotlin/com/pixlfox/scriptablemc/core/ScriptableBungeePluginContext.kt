package com.pixlfox.scriptablemc.core

import com.pixlfox.scriptablemc.ScriptableBungeePluginMain
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Event
import org.graalvm.polyglot.Value

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ScriptableBungeePluginContext(private val engine: ScriptablePluginEngine, val pluginName: String, val pluginInstance: Value): IScriptablePluginContext {
    val proxy: ProxyServer
        get() = ProxyServer.getInstance()

    val bungeePlugin: ScriptableBungeePluginMain
        get() = engine.bungeePlugin!!

    private var eventHandler: Value? = null
//    private var eventRouter: BungeeEventRouter = BungeeEventRouter(this)

    override fun load() {
        if(engine.debugEnabled) {
            bungeePlugin.logger.info("[$pluginName] Loading scriptable plugin context.")
        }

        pluginInstance.invokeMember("onLoad")
    }

    override fun enable() {

        if(engine.debugEnabled) {
            bungeePlugin.logger.info("[$pluginName] Enabling scriptable plugin context.")
        }

        pluginInstance.invokeMember("onEnable")
    }

    override fun disable() {
        if(engine.debugEnabled) {
            bungeePlugin.logger.info("[$pluginName] Disabling scriptable plugin context.")
        }

//        if(this.eventHandler != null) {
//            unregisterEventHandler()
//        }

        pluginInstance.invokeMember("onDisable")
    }

//    fun registerEventHandler(handler: Value) {
//        this.eventHandler = handler
//        proxy.pluginManager.registerListener(bungeePlugin, eventRouter)
//    }
//
//    fun unregisterEventHandler() {
//        this.eventHandler = null
//        proxy.pluginManager.unregisterListener(eventRouter)
//    }

    internal fun fireEvent(eventMethod: String, event: Event) {
        if(eventHandler != null) {
            if (eventHandler!!.hasMember(eventMethod) && eventHandler!!.canInvokeMember(eventMethod)) {
                eventHandler!!.invokeMember(eventMethod, event)
            }
        }
    }

    companion object {
        fun newInstance(pluginName: String, engine: ScriptablePluginEngine, pluginInstance: Value): ScriptableBungeePluginContext {
            if(engine.debugEnabled) {
                engine.bungeePlugin!!.logger.info("[$pluginName] Creating new scriptable plugin context.")
            }

            val context =  ScriptableBungeePluginContext(engine, pluginName, pluginInstance)
            context.load()

            return context
        }
    }
}

//class BungeeEventRouter(private val context: ScriptableBungeePluginContext) : Listener {
//
//    @EventHandler fun onChatEvent(event: ChatEvent) = context.fireEvent("onChatEvent", event)
//    @EventHandler fun onLoginEvent(event: LoginEvent) = context.fireEvent("onLoginEvent", event)
//    @EventHandler fun onPlayerDisconnectEvent(event: PlayerDisconnectEvent) = context.fireEvent("onPlayerDisconnectEvent", event)
//    @EventHandler fun onPlayerHandshakeEvent(event: PlayerHandshakeEvent) = context.fireEvent("onPlayerHandshakeEvent", event)
//    @EventHandler fun onPluginMessageEvent(event: PluginMessageEvent) = context.fireEvent("onPluginMessageEvent", event)
//    @EventHandler fun onPostLoginEvent(event: PostLoginEvent) = context.fireEvent("onPostLoginEvent", event)
//    @EventHandler fun onPreLoginEvent(event: PreLoginEvent) = context.fireEvent("onPreLoginEvent", event)
//    @EventHandler fun onProxyPingEvent(event: ProxyPingEvent) = context.fireEvent("onProxyPingEvent", event)
//    @EventHandler fun onProxyReloadEvent(event: ProxyReloadEvent) = context.fireEvent("onProxyReloadEvent", event)
//    @EventHandler fun onServerConnectedEvent(event: ServerConnectedEvent) = context.fireEvent("onServerConnectedEvent", event)
//    @EventHandler fun onServerConnectEvent(event: ServerConnectEvent) = context.fireEvent("onServerConnectEvent", event)
//    @EventHandler fun onServerDisconnectEvent(event: ServerDisconnectEvent) = context.fireEvent("onServerDisconnectEvent", event)
//    @EventHandler fun onServerKickEvent(event: ServerKickEvent) = context.fireEvent("onServerKickEvent", event)
//    @EventHandler fun onServerSwitchEvent(event: ServerSwitchEvent) = context.fireEvent("onServerSwitchEvent", event)
//    @EventHandler fun onSettingsChangedEvent(event: SettingsChangedEvent) = context.fireEvent("onSettingsChangedEvent", event)
//    @EventHandler fun onTabCompleteEvent(event: TabCompleteEvent) = context.fireEvent("onTabCompleteEvent", event)
//    @EventHandler fun onTabCompleteResponseEvent(event: TabCompleteResponseEvent) = context.fireEvent("onTabCompleteResponseEvent", event)
//
//}