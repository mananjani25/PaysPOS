package com.android.pos.data.typeconvert

import androidx.room.TypeConverter
import com.android.pos.data.entities.TaxModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        for (i in list) genreIds += ",$i"
        return genreIds
    }
}