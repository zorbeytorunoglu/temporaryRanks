package com.zorbeytorunoglu.temporaryRanks.commands

import com.zorbeytorunoglu.kLib.extensions.color
import com.zorbeytorunoglu.kLib.extensions.isAlphanumeric
import com.zorbeytorunoglu.kLib.extensions.numbers
import com.zorbeytorunoglu.kLib.task.Scopes
import com.zorbeytorunoglu.kLib.task.suspendFunctionSync
import com.zorbeytorunoglu.temporaryRanks.TemporaryRanks
import com.zorbeytorunoglu.temporaryRanks.messages.MessageContainer
import com.zorbeytorunoglu.temporaryRanks.rank.Rank
import com.zorbeytorunoglu.temporaryRanks.rank.RankManager
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TemporaryRanksCMD(private val plugin: TemporaryRanks): CommandExecutor {

    private val messages: MessageContainer = plugin.messageContainer
    private val rankManager: RankManager = plugin.rankManager

    init {
        plugin.getCommand("temporaryranks").executor = this
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (command.name == "temporaryranks") {

            if (args.isEmpty()) {

                if (sender !is Player) {
                    sender.sendMessage(messages.onlyInGame)
                    return false
                }

                val player: Player = sender

                val ranks = rankManager.getRanks(player)

                if (ranks.isEmpty()) {
                    player.sendMessage(messages.noRankSelf)
                    return false
                }

                if (ranks.size > 1) {
                    sender.sendMessage(messages.yourRanks)
                    ranks.forEach { player.sendMessage(messages.checkRankMultiple
                        .replace("%rank%", it.rank.permissionGroup)
                        .replace("%time%", messages.dateFormat.format(it.expirationDate))) }
                } else {
                    player.sendMessage(messages.checkRankSelf
                        .replace("%rank%", ranks.first().rank.permissionGroup)
                        .replace("%time%", messages.dateFormat.format(ranks.first().expirationDate)))
                }

            }

            else if (args.size == 1 && args[0] == "help") {
                if (hasAdminPermission(sender)) {
                    messages.commandHelp.forEach { sender.sendMessage(it.color) }
                } else {
                    sender.sendMessage(messages.noPermission)
                }
            }

            else if (args.size == 2 && args[0] == "check") {

                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(messages.noPermission)
                    return false
                }

                val player: Player? = plugin.server.getPlayer(args[1])

                if (player == null) {
                    sender.sendMessage(messages.playerNotFound)
                    return false
                }

                if (!rankManager.hasAnyRank(player)) {
                    sender.sendMessage(messages.noRank.replace("%player%", player.name))
                    return false
                }

                if (rankManager.getRanks(player).size == 1) {

                    val rank = rankManager.getRanks(player).first()

                    sender.sendMessage(messages.checkRank.replace("%rank%", rank.rank.permissionGroup)
                        .replace("%time%", messages.dateFormat.format(rank.expirationDate))
                        .replace("%player%", player.name))

                } else {

                    sender.sendMessage(messages.playerRanks.replace("%player%", player.name))
                    rankManager.getRanks(player).forEach {

                        sender.sendMessage(messages.checkRankMultiple
                            .replace("%player%", player.name)
                            .replace("%rank%", it.rank.permissionGroup)
                            .replace("%time%", messages.dateFormat.format(it.expirationDate)))

                    }

                }

            }

            else if (args.size == 3 && args[0] == "remove") {

                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(messages.noPermission)
                    return false
                }

                val player = plugin.server.getPlayer(args[1])

                if (player==null) {
                    sender.sendMessage(messages.playerNotFound)
                    return false
                }

                if (!rankManager.rankExists(args[2])) {
                    sender.sendMessage(messages.rankNotFound)
                    return false
                }

                val rank: Rank = rankManager.getRank(args[2])!!

                if (!rankManager.hasRank(player,rank)) {
                    sender.sendMessage(messages.playerDoesntHaveTheRank)
                    return false
                }

                rankManager.takeRank(player,rank,submitTakeCommands = true)

                sender.sendMessage(messages.rankTaken
                    .replace("%player%", player.name)
                    .replace("%rank%", rank.permissionGroup))

                return true

            }

            else if (args.size == 4 && args[0] == "give") {

                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(messages.noPermission)
                    return false
                }

                val player = plugin.server.getPlayer(args[1])

                if (player==null) {
                    sender.sendMessage(messages.playerNotFound)
                    return false
                }

                if (!rankManager.rankExists(args[2])) {

                    sender.sendMessage(messages.rankNotFound)
                    return false
                }

                if (!validDate(args[3])) {
                    sender.sendMessage(messages.notValidDate)
                    return false
                }

                val rank: Rank = rankManager.getRank(args[2])!!

                if (rankManager.hasRank(player,rank)) {

                    Scopes.supervisorScope.launch {
                        plugin.suspendFunctionSync {
                            rankManager.takeRank(player, rank, true)
                            rankManager.giveRank(player, rank, args[3], submitGiveCommands = true)
                        }
                    }

                } else {

                    rankManager.giveRank(player, rank, args[3], submitGiveCommands = true)

                    sender.sendMessage(messages.rankGiven.replace("%time%",args[3])
                        .replace("%rank%", rank.permissionGroup)
                        .replace("%player%", player.name))

                }

            }

        }

        return false

    }

    private fun hasAdminPermission(sender: CommandSender): Boolean = sender.hasPermission("temporaryranks.admin")

    private fun validDate(string: String): Boolean {

        if (!string.isAlphanumeric) return false

        if (string.none { it.isLetter() }) return false

        if (string.numbers <= 0 || string.numbers > 365) return false

        return true

    }

}