package com.zorbeytorunoglu.temporaryRanks

import com.zorbeytorunoglu.kLib.MCPlugin
import com.zorbeytorunoglu.kLib.configuration.createYamlResource
import com.zorbeytorunoglu.kLib.task.Scopes
import com.zorbeytorunoglu.kLib.task.suspendFunctionAsync
import com.zorbeytorunoglu.temporaryRanks.commands.TemporaryRanksCMD
import com.zorbeytorunoglu.temporaryRanks.messages.MessageContainer
import com.zorbeytorunoglu.temporaryRanks.rank.RankManager
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class TemporaryRanks: MCPlugin() {

    lateinit var messageContainer: MessageContainer
    lateinit var rankManager: RankManager

    override fun onEnable() {
        super.onEnable()

        messageContainer = MessageContainer(createYamlResource("messages.yml").load())

        rankManager = RankManager(this)
        rankManager.loadRanks(createYamlResource("ranks.yml").load())

        TemporaryRanksCMD(this)

        ExpirationChecker(this).runTaskTimerAsynchronously(this,20*20L,60*60*20L)

    }

    private class ExpirationChecker(private val plugin: TemporaryRanks): BukkitRunnable() {

        override fun run() {

            Scopes.supervisorScope.launch {
                plugin.suspendFunctionAsync {
                    plugin.server.onlinePlayers.forEach {
                        plugin.rankManager.checkForExpiration(it)
                    }
                }
            }

        }
    }
}