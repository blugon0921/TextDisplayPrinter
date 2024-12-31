package kr.blugon.textdisplayprinter.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import kr.blugon.kotlinbrigadier.getValue
import kr.blugon.kotlinbrigadier.player
import kr.blugon.kotlinbrigadier.registerCommandHandler
import kr.blugon.minicolor.MiniColor
import kr.blugon.minicolor.MiniColor.Companion.miniMessage
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.CommandBlock
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Color
import java.io.File
import javax.imageio.IIOException
import javax.imageio.ImageIO

fun JavaPlugin.registerCommand() {
    lifecycleManager.registerCommandHandler {
        register("tdprinter") {
            require { sender is Player }

            then("image_file" to StringArgumentType.string()) {
                suggests {
                    ArrayList<String>().apply {
                        dataFolder.listFiles().forEach { this.add(it.name) }
                    }
                }

                executes { it.run(dataFolder) }

                then("placeblock") {
                    executes {
                        it.run(dataFolder, place = true)
                    }
                }
            }
        }
        register("tdprinter-half") {
            require { sender is Player }

            then("image_file" to StringArgumentType.string()) {
                suggests {
                    ArrayList<String>().apply {
                        dataFolder.listFiles().forEach { this.add(it.name) }
                    }
                }

                executes { it.run(dataFolder, true) }

                then("placeblock") {
                    executes {
                        it.run(dataFolder, true, place = true)
                    }
                }
            }
        }
    }
}

fun CommandContext<CommandSourceStack>.run(dataFolder: File, half: Boolean = false, place: Boolean = false) {
    val image_file: String by this
    val imageFile = File(dataFolder, image_file)
    val image = try { ImageIO.read(imageFile) }
    catch (e: IIOException) {
        this.source.sender.sendMessage("${MiniColor.RED}파일이 존재하지 않거나 이미지가 아닙니다".miniMessage)
        return
    }
    val player = this.source.player
    val facing = player.facing

    val passengerCommands = ArrayList<String>()
    for(y in 0 until image.height) {
        val textList = ArrayList<String>()
        for(x in 0 until image.width) {
            val color = Color(image.getRGB(x, y))
            val hex = "#" + Integer.toHexString(color.rgb).substring(2)
            textList.add("{\"text\":\"■\",\"color\":\"$hex\"}")
        }
        passengerCommands+= "{id:\"minecraft:text_display\",text:'[${textList.joinToString(",")}]',transformation:{scale:[0.4, 0.5, 0.5],translation:[0.0, -${y*0.0625}, 0.0],left_rotation:[0.0, 0.0, 0.0, 1.0],right_rotation:[0.0, 0.0, 0.0, 1.0]},Tags:[img_line]}"
        if(half) continue
        passengerCommands+= "{id:\"minecraft:text_display\",text:'[${textList.joinToString(",")}]',transformation:{scale:[0.4, 0.5, 0.5],translation:[0.01, -${y*0.0625}, 0.0],left_rotation:[0.0, 0.0, 0.0, 1.0],right_rotation:[0.0, 0.0, 0.0, 1.0]},Tags:[img_line, img_line2]}"
    }


    val summonCommands = ArrayList<ArrayList<String>>()
    var p = ArrayList<String>()
    passengerCommands.forEach {
        if(29000 <= p.joinToString("").length) {
            summonCommands.add(p)
            p = ArrayList()
        }
        p.add(it)
    }
    if(p.isNotEmpty()) summonCommands.add(p)

    val commands = ArrayList<String>()
    var i = 0
    summonCommands.forEach {
        val command = "summon text_display ~$i ~ ~ {Passengers:[${it.joinToString(",")}],Tags:[img]}"
        commands.add(command)
        i++
    }
    commands.addAll(listOf(
        "execute as @e[tag=img_line] run data merge entity @s {background:0,line_width:2147483647}",
        "execute as @e[tag=img] at @s run tp @s ~ ~ ~ ${
            when(facing) {
                BlockFace.WEST -> "90"
                BlockFace.NORTH -> "180"
                BlockFace.EAST-> "270"
                else -> "0"
            }
        } 0"
    ))


    val items = ArrayList<ItemStack>()
    var i2 = 0
    commands.forEach { command->
        val block = player.location.add(-i2.toDouble(), .0, .0).block

        val type = (if(i2 == 0) Material.COMMAND_BLOCK
        else Material.CHAIN_COMMAND_BLOCK)

        val state = if(place) {
            block.type = type

            val blockState = block.state
            if(blockState is CommandBlock) {
                blockState.command = command
                blockState.update()
            }

            val data = block.blockData
            if(data is Directional) {
                data.facing = BlockFace.WEST
                block.blockData = data
            }
            blockState
        } else {
            type.createBlockData().createBlockState().apply {
                if(this is CommandBlock) {
                    this.command = command
                    this.update()
                }
            }
        }
        items.add(ItemStack(type).apply {
            val meta = itemMeta as BlockStateMeta
            meta.blockState = state
            itemMeta = meta
        })
        i2++
    }

    player.inventory.addItem(ItemStack(Material.ORANGE_BUNDLE).apply {
        editMeta { meta->
            (meta as BundleMeta).setItems(items)
        }
    })
}