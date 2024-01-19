package com.pays.pos.data.typeconvert

import androidx.room.TypeConverter

object TypeConvertersIds {
    @TypeConverter
    fun gettingListFromString(genreIds: String): List<Int> {
        val list = mutableListOf<Int>()

        val array = genreIds.split(",".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()

        for (s in array) {
            if (s.isNotEmpty()) {
                list.add(s.toInt())
            }
        }
        return list
    }

    @TypeConverter
    fun writingStringFromList(list: List<Int>): String {
        var genreIds = ""
        list.forEach {
            if (it != null) {
                genreIds += ",$it"
            }
        }
        return genreIds
    }
}