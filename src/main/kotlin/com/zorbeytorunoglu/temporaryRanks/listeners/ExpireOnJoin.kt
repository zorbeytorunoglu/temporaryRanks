package com.zorbeytorunoglu.temporaryRanks.listeners

import com.zorbeytorunoglu.kLib.extensions.registerEvents
import com.zorbeytorunoglu.kLib.task.Scopes
import com.zorbeytorunoglu.kLib.task.suspendFunctionAsync
import com.zorbeytorunoglu.temporaryRanks.TemporaryRanks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class ExpireOnJoin(private val plugin: TemporaryRanks): Listener {

    init {
        plugin.registerEvents(this)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {

        Scopes.supervisorScope.launch {
            delay(20L)
            plugin.suspendFunctionAsync {
                plugin.rankManager.checkForExpiration(event.player)
            }
        }

    }

}