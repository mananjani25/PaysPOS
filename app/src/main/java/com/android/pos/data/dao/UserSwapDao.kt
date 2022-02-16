package com.android.pos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.UserSwapModel

@Dao
interface UserSwapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(userSwapModel: UserSwapModel): Long?


    @Query("DELETE FROM UserSwap where UserSwap.employee_id = :id")
    suspend fun deleteUser(id: Int?)

    @Query("select * from UserSwap ORDER BY date_time ")
    suspend fun userList(): List<UserSwapModel>

}
