package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.data.entities.TbCountryList


@Dao
interface CountryListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(countryList: List<TbCountryList>)

    @get:Query("select * from TbCountryList")
    val all: LiveData<List<TbCountryList>>
}