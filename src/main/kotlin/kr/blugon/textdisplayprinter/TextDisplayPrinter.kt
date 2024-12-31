package kr.blugon.textdisplayprinter

import kr.blugon.textdisplayprinter.command.registerCommand
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

lateinit var plugin: JavaPlugin
class TextDisplayPrinter : JavaPlugin(), Listener {

    override fun onEnable() {
        plugin = this
        logger.info("Plugin enabled")
        Bukkit.getPluginManager().registerEvents(this, this)
        if(!dataFolder.exists()) dataFolder.mkdir()

        registerCommand()
    }

    override fun onDisable() {
        logger.info("Plugin disabled")
    }
}