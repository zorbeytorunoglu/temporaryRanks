package com.zorbeytorunoglu.temporaryRanks.rank

import com.zorbeytorunoglu.kLib.configuration.Resource
import com.zorbeytorunoglu.kLib.configuration.createYamlResource
import com.zorbeytorunoglu.kLib.extensions.numbers
import com.zorbeytorunoglu.kLib.task.MCDispatcher
import com.zorbeytorunoglu.kLib.task.Scopes
import com.zorbeytorunoglu.kLib.task.suspendFunctionAsync
import com.zorbeytorunoglu.kLib.task.suspendFunctionSync
import com.zorbeytorunoglu.temporaryRanks.TemporaryRanks
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
import java.util.Calendar
import java.util.Date

class RankManager(private val plugin: TemporaryRanks) {

    private var ranks: HashMap<String, Rank> = hashMapOf()

    private var tPlayers: HashMap<String, TPlayer> = hashMapOf()

    internal fun loadRanks(ranksResource: Resource) {

        val rankSet = ranksResource.getKeys(false)

        if (rankSet.isEmpty()) return

        for (key in rankSet) {

            val permissionGroup = ranksResource.getString("$key.permission_group")
            val defaultTime = ranksResource.getString("$key.default_time")

            val defaultTimeObj = getDefaultTimeFromString(defaultTime)

            val giveCommands = ranksResource.getStringList("$key.give_commands")
            val takeCommands = ranksResource.getStringList("$key.take_commands")

            ranks[permissionGroup] = Rank(permissionGroup,defaultTimeObj,giveCommands, takeCommands)

        }

    }

    fun getRank(permissionGroup: String): Rank? {
        if (ranks.containsKey(permissionGroup)) return ranks[permissionGroup]
        return null
    }

    private fun getTPlayer(player: Player): TPlayer? = if (tPlayers.containsKey(player.uniqueId.toString()))
        tPlayers[player.uniqueId.toString()] else null

    fun getRanks(player: Player): Collection<PlayerRank> {

        val tPlayer = getTPlayer(player)

        return tPlayer?.ranks ?: listOf()

    }

    fun giveRank(player: Player, rankObj: Rank, time: String, submitGiveCommands: Boolean) {

        val defaultTime = getDefaultTimeFromString(time)

        if (!hasTPlayer(player)) {
            tPlayers[player.uniqueId.toString()] =
                TPlayer(player.uniqueId.toString(),
                    mutableListOf(PlayerRank(player.uniqueId.toString(), rankObj, getExpirationDate(defaultTime)))
                )
        } else {
            tPlayers[player.uniqueId.toString()]!!.ranks.add(PlayerRank(player.uniqueId.toString(),rankObj, getExpirationDate(defaultTime)))
        }

        if (submitGiveCommands) {
            Scopes.supervisorScope.launch {
                plugin.suspendFunctionSync { submitGiveCommands(rankObj, player) }
            }
        }

    }

    private fun hasTPlayer(player: Player): Boolean = tPlayers.containsKey(player.uniqueId.toString())

    private fun getExpirationDate(defaultTime: DefaultTime): Date {

        val calendar = Calendar.getInstance()

        when (defaultTime.defaultTimeType) {
            TimeType.DAY -> calendar.add(Calendar.DAY_OF_MONTH, defaultTime.defaultTime)
            TimeType.HOUR -> calendar.add(Calendar.HOUR, defaultTime.defaultTime)
            else -> { calendar.add(Calendar.MONTH, defaultTime.defaultTime) }
        }

        return calendar.time

    }

    private fun getPlayerRank(player: Player, rank: Rank): PlayerRank? {
        if (!hasTPlayer(player)) return null
        var playerRank: PlayerRank? = null
        for (pRank in tPlayers[player.uniqueId.toString()]!!.ranks) {
            if (pRank.rank == rank) {
                playerRank = pRank
                break
            }
        }
        return playerRank
    }

    fun takeRank(player: Player, rank: Rank, submitTakeCommands: Boolean) {

        tPlayers[player.uniqueId.toString()]!!.ranks.remove(getPlayerRank(player,rank))

        if (submitTakeCommands) {
            Scopes.supervisorScope.launch {
                plugin.suspendFunctionSync { submitTakeCommands(rank, player) }
            }
        }

    }

    fun hasAnyRank(player: Player): Boolean =
        tPlayers.containsKey(player.uniqueId.toString()) && tPlayers[player.uniqueId.toString()]!!.ranks.isNotEmpty()

    fun hasRank(player: Player, rank: Rank): Boolean {

        if (!hasTPlayer(player)) return false

        return tPlayers[player.uniqueId.toString()]!!.ranks.any {
            it.rank == rank
        }

    }

    private fun submitGiveCommands(rank: Rank, player: Player) {
        rank.giveCommands.forEach {
            plugin.server.dispatchCommand(plugin.server.consoleSender, it
                .replace("%player%", player.name)
                .replace("%rank%", rank.permissionGroup))
        }
    }

    private fun submitTakeCommands(rank: Rank, player: Player) {
        rank.takeCommands.forEach {
            plugin.server.dispatchCommand(plugin.server.consoleSender, it
                .replace("%player%", player.name)
                .replace("%rank%", rank.permissionGroup))
        }
    }

    private fun getDefaultTimeFromString(time: String): DefaultTime {

        val timeType: TimeType = when (time.filter { it.isLetter() }) {
            "d" -> TimeType.DAY
            "h" -> TimeType.HOUR
            "m" -> TimeType.MONTH
            else -> TimeType.DAY
        }

        return DefaultTime(time.numbers, timeType)
    }

    fun checkForExpiration(player: Player) {

        if (!tPlayers.containsKey(player.uniqueId.toString())) return

        val tPlayer: TPlayer = tPlayers[player.uniqueId.toString()]!!

        val today = Date()

        Scopes.supervisorScope.launch {
            plugin.suspendFunctionAsync {
                tPlayer.ranks.forEach {
                    if (!it.expirationDate.after(today)) {
                        takeRank(player,it.rank,true)
                    }
                }
            }
        }
    }

    fun loadData(resource: Resource) {

        if (!resource.file.exists()) return

        if (resource.getKeys(false).isEmpty()) return

        Scopes.supervisorScope.launch {
            plugin.suspendFunctionAsync {
                for (key in resource.getKeys(false)) {

                    val ranks: MutableList<PlayerRank> = mutableListOf()

                    for (rankName in resource.getConfigurationSection(key).getKeys(false)) {

                        if (!rankExists(rankName)) {
                            resource.set(key, null)
                            continue
                        }

                        val playerRank = PlayerRank(key, getRank(rankName)!!,
                            plugin.messageContainer.dateFormat.parse(
                                resource.getString("$key.$rankName.expire_date")
                            ))

                        ranks.add(playerRank)

                    }

                    tPlayers[key] = TPlayer(key, ranks)

                }
            }
        }

    }

    fun saveData(resource: Resource) = runBlocking(MCDispatcher(plugin, false)) {

        if (tPlayers.isEmpty()) return@runBlocking

        for (key in tPlayers.keys) {

            if (tPlayers[key]!!.ranks.isEmpty()) continue

            for (pRank in tPlayers[key]!!.ranks) {

                resource.set(pRank.uuid+"."+pRank.rank.permissionGroup+".expire_date",
                    plugin.messageContainer.dateFormat.format(pRank.expirationDate))

            }

        }

        resource.save()

    }

    fun resetDataFile(resource: Resource): Resource {
        if (resource.file.exists()) resource.file.delete()
        return plugin.createYamlResource("data.yml").load()
    }

    fun rankExists(rank: String): Boolean = ranks.containsKey(rank)

    data class PlayerRank(val uuid: String, val rank: Rank, val expirationDate: Date)

    data class TPlayer(val uuid: String, val ranks: MutableCollection<PlayerRank>)

}