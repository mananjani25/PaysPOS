package com.pays.pos.utils

import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel

interface OnCancelOrder {
    fun onSucess(data:OnlineOrderResponseModel.Data,pos:Int,isCount:Boolean,startDate:String,endDate:String,action:String)
}