package com.pays.pos.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class  CategoryWithInventory(

    @Embedded var category: TbCategory,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    var inventoryLists: List<TbItem?>?,

    )