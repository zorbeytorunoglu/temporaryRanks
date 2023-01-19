package com.zorbeytorunoglu.temporaryRanks.messages

import com.zorbeytorunoglu.kLib.configuration.Resource
import com.zorbeytorunoglu.kLib.extensions.color
import java.text.SimpleDateFormat

class MessageContainer(private val resource: Resource) {

    val noPermission: String = resource.getString("no-permission").color
    val rankGiven: String = resource.getString("rank-given").color
    val rankTaken: String = resource.getString("rank-taken").color
    val noRank: String = resource.getString("no-rank").color
    val checkRank: String = resource.getString("check-rank").color
    val checkRankSelf: String = resource.getString("check-rank-self").color
    val noRankSelf: String = resource.getString("no-rank-self").color
    val onlyInGame: String = resource.getString("only-in-game").color
    val yourRanks: String = resource.getString("your-ranks").color
    val checkRankMultiple: String = resource.getString("check-rank-multiple-format").color
    val dateFormat: SimpleDateFormat = SimpleDateFormat(resource.getString("date-format"))
    val playerNotFound: String = resource.getString("player-not-found").color
    val rankNotFound: String = resource.getString("rank-not-found").color
    val playerDoesntHaveTheRank: String = resource.getString("player-doesnt-have-the-rank").color
    val notValidDate: String = resource.getString("not-valid-date").color
    val playerRanks: String = resource.getString("player-ranks").color
    val commandHelp: List<String> get() {
        val colored: MutableList<String> = mutableListOf()
        resource.getStringList("cmd-help").forEach {
            colored.add(it.color)
        }
        return colored
    }

}