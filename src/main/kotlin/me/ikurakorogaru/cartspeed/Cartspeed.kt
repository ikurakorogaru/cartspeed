package me.ikurakorogaru.cartspeed

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Minecart
import org.bukkit.event.EventHandler
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.bukkit.util.BlockIterator

class Cartspeed : JavaPlugin(), Listener {
    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (event.vehicle is Minecart) {
            val fromPos = event.from
            val toPos = event.to
            if (fromPos.block != toPos.block) {
                val faces = listOf(
                    BlockFace.NORTH,
                    BlockFace.SOUTH,
                    BlockFace.EAST,
                    BlockFace.WEST,
                    BlockFace.DOWN,
                    BlockFace.UP,
                )
                val serializer = PlainTextComponentSerializer.plainText()
                var totalSpeed = 0.0
                var speedSignCount = 0
                val defspeed = 0.4
                val start = fromPos.toVector()
                val delta = toPos.toVector().subtract(start)
                val distance = delta.length()
                var overStepd = false
                val iterator = BlockIterator(
                    fromPos.world,
                    start,
                    delta.normalize(),
                    0.0,
                    kotlin.math.ceil(distance).toInt()
                )

                iterator.forEachRemaining { rail ->
                    if (!overStepd) {
                        var localTotalSpeed = 0.0
                        var localSpeedSignCount = 0
                        for (face in faces) {
                            val block = rail.getRelative((face))
                            if (block.state is Sign) {
                                val sign = block.state as Sign
                                val sides = listOf(Side.FRONT, Side.BACK)
                                for (side in sides) {
                                    val nowSignSide = sign.getSide(side)
                                    val lines = nowSignSide.lines()
                                    if (serializer.serialize(lines[0]) == "[speed]") {
                                        val speed = serializer.serialize(lines[1]).toDoubleOrNull()
                                        if (speed != null && speed >= 0) {
                                            localTotalSpeed += speed
                                            localSpeedSignCount++
                                        }
                                    }
                                }
                            }
                        }

                        if (rail == toPos.block) {
                            overStepd = true
                        }
                        if (localSpeedSignCount != 0) {
                            totalSpeed = localTotalSpeed
                            speedSignCount = localSpeedSignCount
                        }
                    }
                }
                if (speedSignCount != 0) {
                    val average = totalSpeed / speedSignCount
                    (event.vehicle as Minecart).maxSpeed = defspeed * average
                }
            }
        }
    }
}