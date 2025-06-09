package com.pays.pos.data.model.responseModel

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.pays.pos.data.model.GetPaymentOrderDetailsResponse
import com.pays.pos.data.model.requestModel.OrderItemVariationAttribute
import com.google.gson.annotations.SerializedName
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse.Data.RefundDetails
import kotlinx.parcelize.Parcelize


data class OnlineOrderResponseModel(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {

    @SuppressLint("ParcelCreator")
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("creation_time_on_terminal")
        val creationTimeOnTerminal: Any,
        @SerializedName("customer")
        val customer: Customer?,
        @SerializedName("customer_id")
        val customerId: Any,
        @SerializedName("date")
        val date: String,
        @SerializedName("delivery_employee_id")
        val deliveryEmployeeId: Any,
        @SerializedName("delivery_type")
        val deliveryType: String,
        @SerializedName("discount_type_id")
        val discountTypeId: Any,
        @SerializedName("dynamic_discount_id")
        val dynamicDiscountId: Any,
        @SerializedName("edit_order_count")
        val editOrderCount: Any,
        @SerializedName("edited_order_timestamp")
        val editedOrderTimestamp: Any,
        @SerializedName("employee")
        val employee: Employee?,
        @SerializedName("employee_id")
        val employeeId: Int,
        @SerializedName("future_delivery_date")
        val futureDeliveryDate: String? = "",
        @SerializedName("future_delivery_time")
        val futureDeliveryTime: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("custom_order_id")
        val custom_order_id: Int,
        @SerializedName("index_of_date")
        val indexOfDate: Any,
        @SerializedName("integer")
        val integer: Any,
        @SerializedName("is_edited")
        val isEdited: Boolean,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("note")
        val note: String,
        @SerializedName("offline_id")
        val offlineId: String,
        @SerializedName("open_order_type")
        val openOrderType: Any,
        @SerializedName("open_order_type_id")
        val openOrderTypeId: Any,
        @SerializedName("order_items")
        val orderItems: List<OrderItem>,
        @SerializedName("order_service_charges")
        val orderServiceCharges: List<OrderServiceCharge>,
        @SerializedName("order_type")
        var orderType: String,
        @SerializedName("order_type_name")
        val orderTypeName: String,
        @SerializedName("order_type_id")
        val orderTypeId: Int,
        @SerializedName("payment_status")
        var paymentStatus: String,
        @SerializedName("order_status")
        var order_status: String,
        @SerializedName("payments")
        val payments: List<Payment>,
        @SerializedName("service_charge_enabled")
        val serviceChargeEnabled: Boolean,
        @SerializedName("sub_total")
        val subTotal: Double,
        @SerializedName("tax_enabled")
        val taxEnabled: Boolean,
        @SerializedName("terminal_id")
        val terminalId: Int,
        @SerializedName("total_amount")
        val totalAmount: Double,
        @SerializedName("cash_discount_or_surcharge")
        val cash_discount_or_surcharge: Double,
        @SerializedName("total_discount")
        var totalDiscount: Double,
        @SerializedName("total_service_charges")
        val totalServiceCharges: Double,
        @SerializedName("total_tax_amount")
        val totalTaxAmount: Double,
        @SerializedName("total_tips")
        val totalTips: Double,
        @SerializedName("loyalty_amount")
        val loyaltyAmount: Double,
        @SerializedName("is_loyalty_applied")
        val isLoyaltyApplied: Boolean,
        @SerializedName("used_reward_points")
        val usedRewardPoints: Int,
        @SerializedName("loyalty_program_id")
        val loyaltyProgramId: Int,
        @SerializedName("digital_receipt_url")
        val digitalReceiptUrl: String,
        @SerializedName("cash_discount_type")
        val cashDiscountType: String,
        @SerializedName("updated_at")
        val updatedAt: String,
        var isCheck: Boolean = false,
        @SerializedName("magensa_response")
        val magensa_response_data: String,
        @SerializedName("terminal_name")
        val terminalName: String,
        @SerializedName("refund_detail")
        val refundDetails: RefundDetails
        //@SerializedName("order_type_name") val order_type_name: String,
    ) : Parcelable {
        data class OrderItem(
            @SerializedName("category_id")
            val categoryId: Int,
            @SerializedName("custom_item_id")
            val custom_item_id: Int,
            @SerializedName("is_edited")
            val isEdited: Boolean,
            @SerializedName("completed_in_kitchen")
            val completedInKitchen: Boolean,
            @SerializedName("discount_amount")
            var discountAmount: Double,
            @SerializedName("discount_id")
            val discountId: Int? = null,
            @SerializedName("discount_type")
            val discountType: String,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("float")
            val float: Int,
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_paid")
            val isPaid: Boolean,
            @SerializedName("is_printed")
            val isPrinted: Boolean,
            @SerializedName("item_id")
            val itemId: Int,
            @SerializedName("item_name")
            val itemName: String,
            @SerializedName("note")
            val note: String,
            @SerializedName("order_id")
            val orderId: Int,
            @SerializedName("order_item_modifiers")
            val orderItemModifiers: List<OrderItemModifier>,
            @SerializedName("order_item_original_modifiers")
            var orderItemOriginalModifiers: List<OrderItemModifier>,
            @SerializedName("order_item_taxes")
            val orderItemTax: List<OrderItemTax>,
            @SerializedName("price")
            var price: Double,
            @SerializedName("quantity")
            val quantity: Int,
            @SerializedName("timestamp")
            val timestamp: String,
            @SerializedName("total_price")
            var totalPrice: Double,
            @SerializedName("order_item_variation")
            val order_item_variation: OrderItemVariationAttribute?,

            ) {
            data class OrderItemModifier(
                @SerializedName("category_id")
                val categoryId: Any,
                @SerializedName("created_at")
                val createdAt: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("is_modifier")
                val isModifier: Boolean,
                @SerializedName("item_id")
                val itemId: Any,
                @SerializedName("modifier_id")
                val modifierId: Int,
                @SerializedName("modifier_set_id")
                val modifierSetId: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("order_id")
                val orderId: Any,
                @SerializedName("order_item_id")
                val orderItemId: Int,
                @SerializedName("price")
                val price: Double,
                @SerializedName("quantity")
                val quantity: Int,
                @SerializedName("modifier_quantity")
                val modifier_quantity: Int,
                @SerializedName("timestamp")
                val timestamp: Any,
                @SerializedName("updated_at")
                val updatedAt: String
            )

            data class OrderItemTax(
                @SerializedName("amount")
                val amount: Any,
                @SerializedName("created_at")
                val createdAt: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("is_default")
                val isDefault: Boolean,
                @SerializedName("is_tax_removed")
                val isTaxRemoved: Boolean,
                @SerializedName("name")
                val name: String,
                @SerializedName("order_id")
                val orderId: Int,
                @SerializedName("order_item_id")
                val orderItemId: Int,
                @SerializedName("order_item_modifier_id")
                val orderItemModifierId: Any,
                @SerializedName("rate")
                val rate: Double,
                @SerializedName("tax_id")
                val taxId: Int,
                @SerializedName("tax_total_amount")
                val taxTotalAmount: Double,
                @SerializedName("tax_type")
                val taxType: String,
                @SerializedName("updated_at")
                val updatedAt: String
            )
        }

        data class OrderServiceCharge(
            @SerializedName("amount")
            val amount: Double,
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("order_id")
            val orderId: Int,
            @SerializedName("rate")
            val rate: Double,
            @SerializedName("service_charge_id")
            val serviceChargeId: Int,
            @SerializedName("updated_at")
            val updatedAt: String?,
            @SerializedName("min_guest_count")
            val min_guest_count: Int? = null,
            @SerializedName("max_guest_count")
            val max_guest_count: Int? = null,
            @SerializedName("order_type")
            val order_type: String
        )

//        data class Payment(
//            @SerializedName("amount")
//            val amount: Double,
//            @SerializedName("card_name")
//            val cardName: String,
//            @SerializedName("card_number")
//            val cardNumber: String,
//            @SerializedName("card_type")
//            val cardType: String,
//            @SerializedName("cash_discount_or_surcharge")
//            val cashDiscount: Double,
//            @SerializedName("created_at")
//            val createdAt: String,
//            @SerializedName("employee_id")
//            val employeeId: Int,
//            @SerializedName("id")
//            val id: Int,
//            @SerializedName("offline_id")
//            val offlineId: String,
//            @SerializedName("order_id")
//            val orderId: Int,
//            @SerializedName("payable_id")
//            val payableId: Int,
//            @SerializedName("payable_type")
//            val payableType: String,
//            @SerializedName("payment_type")
//            val paymentType: String,
//            @SerializedName("service_charge_amount")
//            val serviceChargeAmount: Double,
//            @SerializedName("sub_total")
//            val subTotal: Double,
//            @SerializedName("tax_amount")
//            val taxAmount: Double,
//            @SerializedName("terminal_id")
//            val terminalId: Int,
//            @SerializedName("tips")
//            val tips: Double,
//            @SerializedName("tips_adjusted")
//            val tipsAdjusted: Boolean,
//            @SerializedName("total_discount")
//            val totalDiscount: Double,
//            @SerializedName("transaction_id")
//            val transactionId: String,
//            @SerializedName("updated_at")
//            val updatedAt: String,
//            @SerializedName("pax_data")
//            val pax_data: String
//        )

        data class Payment(
            @SerializedName("amount")
            val amount: Double,
            @SerializedName("card_name")
            val cardName: String,
            @SerializedName("card_number")
            val cardNumber: String,
            @SerializedName("card_type")
            val cardType: String,
            @SerializedName("cash_discount_or_surcharge")
            val cashDiscount: Double,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("id")
            val id: Int,
            @SerializedName("offline_id")
            val offlineId: String,
            @SerializedName("order_id")
            val orderId: Int,
            @SerializedName("payable_id")
            val payableId: Int,
            @SerializedName("payable_type")
            val payableType: String,
            @SerializedName("payment_type")
            val paymentType: String,
            @SerializedName("service_charge_amount")
            val serviceChargeAmount: Double,
            @SerializedName("sub_total")
            val subTotal: Double,
            @SerializedName("tax_amount")
            val taxAmount: Double,
            @SerializedName("terminal_id")
            val terminalId: Int,
            @SerializedName("tips")
            val tips: Double,
            @SerializedName("tips_adjusted")
            val tipsAdjusted: Boolean,
            @SerializedName("total_discount")
            val totalDiscount: Double,
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("pax_data")
            val pax_data: String,

            // New fields added
            @SerializedName("dynamic_payment_id")
            val dynamicPaymentId: String?,
            @SerializedName("tip_setting_id")
            val tipSettingId: String?,
            @SerializedName("loyalty_program_id")
            val loyaltyProgramId: String?,
            @SerializedName("loyalty_amount")
            val loyaltyAmount: Double,
            @SerializedName("is_loyalty_applied")
            val isLoyaltyApplied: Boolean,
            @SerializedName("used_reward_points")
            val usedRewardPoints: Int,
            @SerializedName("cash_discount_type")
            val cashDiscountType: String?,
            @SerializedName("deleted_at")
            val deletedAt: String?,
            @SerializedName("magensa_response")
            val magensaResponse: String,
            @SerializedName("is_captured")
            val isCaptured: Boolean,
            @SerializedName("gift_card_redeemed_amount")
            val giftCardRedeemedAmount: Double,
            @SerializedName("signature")
            val signature: String?,
            @SerializedName("loyalty_balance")
            val loyaltyBalance: Double,
            @SerializedName("captured_at")
            val capturedAt: String?,
            @SerializedName("gift_card_id")
            val giftCardId: String?,
            @SerializedName("capture_response")
            val captureResponse: String?,
            @SerializedName("tip_with_surcharge_percentage")
            val tipWithSurchargePercentage: Double,
            @SerializedName("global_uniq_id")
            val globalUniqId: String,
            @SerializedName("ref_num")
            val refNum: String,
            @SerializedName("ext_data")
            val extData: String,
            @SerializedName("ecr_ref_num")
            val ecrRefNum: String,
            @SerializedName("pax_transaction_token")
            val paxTransactionToken: String?
        )



        data class Customer(

            @SerializedName("birth_date")
            val birthDate: String,
            @SerializedName("company")
            val company: String,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("email")
            val email: String,
            @SerializedName("first_name")
            val firstName: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("last_name")
            val lastName: String,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("note")
            val note: String,
            @SerializedName("updated_at")
            val updatedAt: String,
            @SerializedName("addresses")
            val addresses: List<Address>,
            @SerializedName("phones")
            val phones: List<Phone>,
            @SerializedName("enroll_to_loyalty")
            val enroll_to_loyalty: Boolean?,
            @SerializedName("final_reward")
            val final_reward: Int? = 0,
        ) {
            @Parcelize
            data class Address(
                @SerializedName("address1")
                val address1: String,
                @SerializedName("address2")
                val address2: String,
                @SerializedName("city")
                val city: String,
                @SerializedName("country")
                val country: String?,
                @SerializedName("full_address")
                val fullAddress: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("latitude")
                val latitude: String?,
                @SerializedName("longitude")
                val longitude: String?,
                @SerializedName("postcode")
                val postcode: String,
                @SerializedName("state")
                val state: String,
                @SerializedName("street")
                val street: String,
                @SerializedName("type_of_address")
                val typeOfAddress: String
            ) : Parcelable {}

            @Parcelize
            data class Phone(
                @SerializedName("id")
                val id: Int,
                @SerializedName("phone_number")
                val phoneNumber: String
            ) : Parcelable {}
        }

        data class Employee(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("email")
            val email: String,
            @SerializedName("first_name")
            val firstName: String,
            @SerializedName("hourly_wages")
            val hourlyWages: Double,
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_active")
            val isActive: Boolean,
            @SerializedName("is_clocked_in")
            val isClockedIn: Boolean,
            @SerializedName("last_name")
            val lastName: String,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("loggedin_terminal_id")
            val loggedinTerminalId: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("passcode")
            val passcode: String,
            @SerializedName("phone_number")
            val phoneNumber: String,
            @SerializedName("team_role_id")
            val teamRoleId: Int,
            @SerializedName("updated_at")
            val updatedAt: String
        )

        override fun describeContents(): Int {
            TODO("Not yet implemented")
        }

        override fun writeToParcel(p0: Parcel, p1: Int) {
            TODO("Not yet implemented")
        }
    }

}
