package com.pixlfox.scriptablemc

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.pixlfox.scriptablemc.core.js.JavaScriptPluginEngine
import com.pixlfox.scriptablemc.core.python.PythonPluginEngine
import com.pixlfox.scriptablemc.smartinvs.MainMenu
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.graalvm.polyglot.PolyglotException


@Suppress("unused")
@CommandAlias("scriptablemc|smc")
class ScriptablePluginCommand(private val basePlugin: ScriptablePluginMain) : BaseCommand() {

    @Subcommand("info|i")
    @CommandPermission("scriptablemc.info")
    fun info(player: CommandSender) {
        val scriptEngine = basePlugin.jsPluginEngine
        val isGraalRuntime = scriptEngine?.eval("if (typeof Graal != 'undefined') { Graal.isGraalRuntime() } else { false }")?.asBoolean() == true
        player.sendMessage("${ChatColor.GREEN}ScriptableMC Version: ${basePlugin.description.version}")
        player.sendMessage("${ if(isGraalRuntime) ChatColor.GREEN else ChatColor.YELLOW }GraalVM Java Runtime: $isGraalRuntime")
        if(isGraalRuntime) {
            player.sendMessage("${ChatColor.AQUA}GraalVM Runtime Version: ${scriptEngine?.eval("Graal.versionGraalVM")}")
            player.sendMessage("${ChatColor.AQUA}GraalJS Engine Version: ${scriptEngine?.eval("Graal.versionJS")}")
        }
    }

    @Subcommand("menu|m")
    @CommandPermission("scriptablemc.menu")
    fun menu(player: Player) {
        MainMenu.INVENTORY.open(player)
    }

    @Subcommand("reload|rl")
    @CommandAlias("smcrl")
    @CommandPermission("scriptablemc.reload")
    fun reload(sender: CommandSender) {
        basePlugin.patchClassLoader {
            try {
                if(basePlugin.jsPluginEngine != null) {
                    basePlugin.jsPluginEngine!!.close()
                    basePlugin.logger.info("JavaScript plugin engine shutdown.")
                }

                basePlugin.reloadConfig()

                basePlugin.jsPluginEngine = JavaScriptPluginEngine(
                    basePlugin,
                    basePlugin.config.getString("root_scripts_folder", "./scripts").orEmpty(),
                    basePlugin.config.getBoolean("debug", false),
                    basePlugin.config.getBoolean("extract_libs", true)
                )

                basePlugin.jsPluginEngine!!.start()
                basePlugin.logger.info("JavaScript plugin engine started.")

                sender.sendMessage("JavaScript plugin engine reloaded.")
            } catch (e: Exception) {
                e.printStackTrace()

                sender.sendMessage("${ChatColor.DARK_RED}$e")
                for (stackTrace in e.stackTrace) {
                    sender.sendMessage("${ChatColor.DARK_RED}$stackTrace")
                }
            }


            try {
                if(basePlugin.pythonPluginEngine != null) {
                    basePlugin.pythonPluginEngine!!.close()
                    basePlugin.logger.info("Python plugin engine shutdown.")
                }

                basePlugin.reloadConfig()

                basePlugin.pythonPluginEngine = JavaScriptPluginEngine(
                    basePlugin,
                    basePlugin.config.getString("root_scripts_folder", "./scripts").orEmpty(),
                    basePlugin.config.getBoolean("debug", false),
                    basePlugin.config.getBoolean("extract_libs", true)
                )

                basePlugin.pythonPluginEngine!!.start()
                basePlugin.logger.info("Python plugin engine started.")

                sender.sendMessage("Python plugin engine reloaded.")
            } catch (e: Exception) {
                e.printStackTrace()

                sender.sendMessage("${ChatColor.DARK_RED}$e")
                for (stackTrace in e.stackTrace) {
                    sender.sendMessage("${ChatColor.DARK_RED}$stackTrace")
                }
            }
        }
    }

}


@Suppress("unused")
@CommandAlias("scriptablemc|smc")
@Subcommand("javascript|js")
class ScriptablePluginJavaScriptCommand(private val basePlugin: ScriptablePluginMain) : BaseCommand() {

    @Subcommand("execute|ex")
    @CommandAlias("jsex")
    @CommandPermission("scriptablemc.js.execute")
    @Syntax("<code>")
    fun execute(sender: CommandSender, code: String) {
        try {
            val response = basePlugin.jsPluginEngine!!.evalCommandSender(code, sender)
            if (!response.isNull) {
                sender.sendMessage(response.toString())
            }
        }
        catch (e: PolyglotException) {
            e.printStackTrace()

            sender.sendMessage("${ChatColor.RED}$e")
            for (stackTrace in e.stackTrace) {
                if(stackTrace.fileName?.endsWith(".js", true) == true) {
                    sender.sendMessage("${ChatColor.YELLOW}$stackTrace")
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()

            sender.sendMessage("${ChatColor.DARK_RED}$e")
            for (stackTrace in e.stackTrace) {
                if(stackTrace.className.startsWith("com.pixlfox.scriptablemc", true)) {
                    sender.sendMessage("${ChatColor.RED}$stackTrace")
                }
            }
        }
    }

    @Subcommand("file|f")
    @CommandAlias("jsexf")
    @CommandPermission("scriptablemc.js.execute.file")
    @Syntax("<filePath>")
    fun executeFile(sender: CommandSender, filePath: String) {
        if(filePath.equals("main.js", true)) {
            sender.sendMessage("${ChatColor.DARK_RED}Unable to execute the main script entrypoint. Use the command /jsrl to reload scripts.")
            return
        }

        try {
            val response = basePlugin.jsPluginEngine!!.evalFile(filePath)
            if (!response.isNull) {
                sender.sendMessage(response.toString())
            }
        }
        catch (e: Exception) {
            e.printStackTrace()

            sender.sendMessage("${ChatColor.DARK_RED}$e")
            for (stackTrace in e.stackTrace) {
                sender.sendMessage("${ChatColor.DARK_RED}$stackTrace")
            }
        }
    }

    @Subcommand("reload|rl")
    @CommandAlias("jsrl")
    @CommandPermission("scriptablemc.py.reload")
    fun reload(sender: CommandSender) {
        basePlugin.patchClassLoader {
            try {
                if(basePlugin.jsPluginEngine != null) {
                    basePlugin.jsPluginEngine!!.close()
                    basePlugin.logger.info("JavaScript plugin engine shutdown.")
                }

                basePlugin.reloadConfig()

                basePlugin.jsPluginEngine = JavaScriptPluginEngine(
                    basePlugin,
                    basePlugin.config.getString("root_scripts_folder", "./scripts").orEmpty(),
                    basePlugin.config.getBoolean("debug", false),
                    basePlugin.config.getBoolean("extract_libs", true)
                )

                basePlugin.jsPluginEngine!!.start()
                basePlugin.logger.info("JavaScript plugin engine started.")

                sender.sendMessage("JavaScript plugin engine reloaded.")
            } catch (e: Exception) {
                e.printStackTrace()

                sender.sendMessage("${ChatColor.DARK_RED}$e")
                for (stackTrace in e.stackTrace) {
                    sender.sendMessage("${ChatColor.DARK_RED}$stackTrace")
                }
            }
        }
    }
}

@Suppress("unused")
@CommandAlias("scriptablemc|smc")
@Subcommand("python|py")
class ScriptablePluginPythonCommand(private val basePlugin: ScriptablePluginMain) : BaseCommand() {

    @Subcommand("execute|ex")
    @CommandAlias("pyex")
    @CommandPermission("scriptablemc.py.execute")
    @Syntax("<code>")
    fun execute(sender: CommandSender, code: String) {
        try {
            val response = basePlugin.pythonPluginEngine!!.evalCommandSender(code, sender)
            if (!response.isNull) {
                sender.sendMessage(response.toString())
            }
        }
        catch (e: PolyglotException) {
            e.printStackTrace()

            sender.sendMessage("${ChatColor.RED}$e")
            for (stackTrace in e.stackTrace) {
                if(stackTrace.fileName?.endsWith(".py", true) == true) {
                    sender.sendMessage("${ChatColor.YELLOW}$stackTrace")
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()

            sender.sendMessage("${ChatColor.DARK_RED}$e")
            for (stackTrace in e.stackTrace) {
                if(stackTrace.className.startsWith("com.pixlfox.scriptablemc", true)) {
                    sender.sendMessage("${ChatColor.RED}$stackTrace")
                }
            }
        }
    }

    @Subcommand("file|f")
    @CommandAlias("pyexf")
    @CommandPermission("scriptablemc.py.execute.file")
    @Syntax("<filePath>")
    fun executeFile(sender: CommandSender, filePath: String) {
        if(filePath.equals("main.py", true)) {
            sender.sendMessage("${ChatColor.DARK_RED}Unable to execute the main script entrypoint. Use the command /pyrl to reload python scripts.")
            return
        }

        try {
            val response = basePlugin.pythonPluginEngine!!.evalFile(filePath)
            if (!response.isNull) {
                sender.sendMessage(response.toString())
            }
        }
        catch (e: Exception) {
            e.printStackTrace()

            sender.sendMessage("${ChatColor.DARK_RED}$e")
            for (stackTrace in e.stackTrace) {
                sender.sendMessage("${ChatColor.DARK_RED}$stackTrace")
            }
        }
    }

    @Subcommand("reload|rl")
    @CommandAlias("pyrl")
    @CommandPermission("scriptablemc.py.reload")
    fun reload(sender: CommandSender) {
        basePlugin.patchClassLoader {
            try {
                if(basePlugin.pythonPluginEngine != null) {
                    basePlugin.pythonPluginEngine!!.close()
                    basePlugin.logger.info("Python plugin engine shutdown.")
                }

                basePlugin.reloadConfig()

                basePlugin.pythonPluginEngine = PythonPluginEngine(
                    basePlugin,
                    basePlugin.config.getString("root_scripts_folder", "./scripts").orEmpty(),
                    basePlugin.config.getBoolean("debug", false),
                    basePlugin.config.getBoolean("extract_libs", true)
                )

                basePlugin.pythonPluginEngine!!.start()
                basePlugin.logger.info("Python plugin engine started.")

                sender.sendMessage("Python plugin engine reloaded.")
            }
            catch (e: Exception) {
                e.printStackTrace()

                sender.sendMessage("${ChatColor.DARK_RED}$e")
                for (stackTrace in e.stackTrace) {
                    sender.sendMessage("${ChatColor.DARK_RED}$stackTrace")
                }
            }
        }
    }
}