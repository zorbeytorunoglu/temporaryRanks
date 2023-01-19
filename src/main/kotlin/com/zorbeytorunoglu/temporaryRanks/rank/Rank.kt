package com.zorbeytorunoglu.temporaryRanks.rank

data class Rank(
    val permissionGroup: String,
    val defaultTime: DefaultTime,
    val giveCommands: List<String>,
    val takeCommands: List<String>)

enum class TimeType { DAY,MONTH,HOUR }

data class DefaultTime(val defaultTime: Int, val defaultTimeType: TimeType)