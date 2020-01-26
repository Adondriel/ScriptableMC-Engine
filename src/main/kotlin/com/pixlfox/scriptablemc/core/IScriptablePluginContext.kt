package com.pixlfox.scriptablemc.core

internal interface IScriptablePluginContext {
    fun load()
    fun enable()
    fun disable()
}