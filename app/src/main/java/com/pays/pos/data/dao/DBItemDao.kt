package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbItem


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface DBItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(inventory: TbItem?): Long?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllItem(elementsBeanList: List<TbItem>)

    @get:Query("select * from TbItem where TbItem.hide_status = 'UnHide' and TbItem.isDeleted = 0 and TbItem.name != 'Manual Item' GROUP by TbItem.itemId ORDER BY TbItem.sort DESC")
    val allItem: LiveData<List<TbItem?>>?

    @get:Query("select * from TbItem where TbItem.hide_status = 'UnHide' and TbItem.isDeleted = 0 and TbItem.name != 'Manual Item' GROUP by TbItem.itemId ORDER BY TbItem.sort DESC")
    val allItemFromPos: LiveData<List<TbItem?>>?

    @get:Query("select * from TbItem where (TbItem.hide_status = 'UnHide' and TbItem.isDeleted = 0) GROUP by TbItem.itemId ORDER BY TbItem.sort DESC")
    val allItemsWithManualFromPos: LiveData<List<TbItem?>>?

    @Query("select * from TbItem where TbItem.isDeleted = 0 and TbItem.hide_status ='UnHide' and TbItem.name != 'Manual Item' GROUP by TbItem.itemId ORDER BY TbItem.sort ASC")
    fun getPaginationList(): PagingSource<Int, TbItem>

    /*@get:Query("select * from TbItem whe  re TbItem.isManualSales = 1")
    val manualItems : LiveData<List<TabItem?>>?
    */

    @Query("select * from TbItem where TbItem.name like :desc and (TbItem.website_hide_status ='UnHideOnWebsite' or TbItem.hide_status ='UnHide') and  TbItem.name != 'Manual Item' and TbItem.isDeleted = 0 GROUP by TbItem.itemId ORDER BY TbItem.sort ASC")
    fun getItemSearchResults(desc: String): PagingSource<Int, TbItem>


    @get:Query("select * from TbItem where TbItem.hide_status != 'UnHide' and TbItem.isDeleted = 0 and TbItem.name != 'Manual Item' ORDER BY TbItem.sort DESC")
    val unhideItemPos: LiveData<List<TbItem>>

    @get:Query("select * from TbItem where TbItem.website_hide_status != 'UnHideOnWebsite' and TbItem.isDeleted = 0 and TbItem.name != 'Manual Item' ORDER BY TbItem.sort DESC")
    val unhideItemWebsite: LiveData<List<TbItem>>

    @Query("select * from TbItem where TbItem.categoryId  = :id and TbItem.name != 'Manual Item' and TbItem.isDeleted = 0")
    fun getItemList(id: Int): LiveData<List<TbItem>>

    @Query("select * from TbItem where TbItem.categoryId  = :id and TbItem.name != 'Manual Item' and TbItem.isDeleted = 0 and TbItem.isHide = 1")
    fun getItemListByCategory(id: Int?): PagingSource<Int, TbItem>

    @Query("SELECT * from TbItem where TbItem.itemId  = :id and TbItem.name != 'Manual Item' and TbItem.isDeleted = 0 LIMIT 1")
    fun itemById(id: Int?): LiveData<TbItem>?

    @Query("SELECT * from TbItem where TbItem.id  = :id and TbItem.manualSaleId = :manualSetId and TbItem.name != 'Manual Item' and TbItem.isDeleted = 0 LIMIT 1")
    fun itemByIdMod(id: Int?,manualSetId:String?):TbItem?

    @Query("SELECT * from TbItem where TbItem.sku  = :productCode and TbItem.name != 'Manual Item' and TbItem.isDeleted = 0 LIMIT 1")
    fun itemByProductCode(productCode: String): LiveData<TbItem>?

    @Query("SELECT * from TbItem  where TbItem.itemId = :id LIMIT 1")
    fun itemOne(id: Int): TbItem?


    @Query("SELECT * from TbItem where TbItem.itemId  = :restId  and TbItem.isDeleted = 0 LIMIT 1")
    fun itemByInventoryId(restId: Int?): TbItem?

    @Query("SELECT * from TbItem where TbItem.itemId  = :id and TbItem.isDeleted = 0")
    fun itemId(id: Int?): List<TbItem?>?

    @Query("DELETE FROM TbItem where TbItem.itemId  = :id")
    suspend fun deleteItem(id: Int?)

    @Query("DELETE FROM TbItem")
    suspend fun delete()

    @Query("UPDATE TbItem SET hide_status = :hide_status WHERE  TbItem.itemId = :id")
    suspend fun updateItemPos(id: Int, hide_status: String): Int

    @Query("UPDATE TbItem SET website_hide_status = :hide_status WHERE  TbItem.itemId = :id")
    suspend fun updateItemWebsite(id: Int, hide_status: String): Int

    @Query("UPDATE TbItem SET hide_status = 'UnHide' WHERE  TbItem.itemId = :id")
    suspend fun updateShowItem(id: Int): Int


    @Query("UPDATE TbItem SET categoryId = :catId,categoryName = :catName  WHERE  TbItem.itemId = :itemId")
    suspend fun updateItem(catId: Int, catName: String, itemId: Int?): Int


//    @Transaction
//    @Query("SELECT * FROM TbItem")
//    fun cartWithModifierList(): LiveData<List<ItemWithModifier?>>?
//
//    @Transaction
//    @Query("SELECT * FROM TbItem")
//    fun cartWithModifier(): List<ItemWithModifier?>?

    @Query("SELECT itemId from TbItem where TbItem.categoryId  = :catId and TbItem.isDeleted = 0")
    suspend fun getListByCategory(catId: Int?): List<Int?>?

    @Query("UPDATE TbItem SET itemQuantity = :qty WHERE  TbItem.itemId = :id")
    fun updateItemQty(id: Int?, qty: Int?)

    @Query("UPDATE TbItem SET taxes = :taxes WHERE  TbItem.itemId = :id")
    suspend fun updateItemTaxes(id: Int, taxes: List<TaxData>)

    @Query("UPDATE TbItem SET modifier_set_ids = :modifierSetIds WHERE TbItem.itemId = :id")
    suspend fun updateItemModifiers(id: Int, modifierSetIds: List<Int>)

    @Query("select * from TbItem where TbItem.isDeleted = 0 and TbItem.name != 'Manual Item'")
    suspend fun allItemsList(): List<TbItem?>?

    /*Added by Rahul, to solved the tax update issue - START*/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllTbItems(tbItemsList: List<TbItem?>?)
    /*Added by Rahul, to solved the tax update issue - END*/

}