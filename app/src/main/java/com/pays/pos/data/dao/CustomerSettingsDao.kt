package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse

@Dao
interface CustomerSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(model: GetCustomerReceiptSettingsResponse.Data)

    @get:Query("select * from TbCustomerSettings")
    val getCustomerSettings: LiveData<GetCustomerReceiptSettingsResponse.Data>

    @Query("DELETE FROM TbCustomerSettings")
    suspend fun delete()
}