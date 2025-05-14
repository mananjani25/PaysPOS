package com.pays.pos.data.model.requestModel


import android.os.Parcelable
import com.pays.pos.data.model.DineInOrderDetailAttributes
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


class OrderAttributeRequestModel {

    @SerializedName("customer_attributes")
    var customerAttributes: CustomerAttributes? = null

    @SerializedName("date")
    var date: String = ""


    @SerializedName("delivery_type")
    var deliveryType: String = ""

    @SerializedName("employee_id")
    var employeeId: Int = 0

    @SerializedName("mac_address")
    var macAddress: String = ""


    @SerializedName("future_delivery_date")
    var futureDeliveryDate: String = ""

    @SerializedName("future_delivery_time")
    var futureDeliveryTime: String = ""

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("location_id")
    var locationId: Int = 0

    @SerializedName("note")
    var note: String = ""

    @SerializedName("offline_id")
    var offlineId: String? = null


    @SerializedName("send_payment_link")
    var send_payment_link: Boolean = false


    @SerializedName("open_order_type")
    var openOrderType: String = ""

    @SerializedName("order_type_name")
    var orderTypeName: String = ""

    @SerializedName("order_items_attributes")
    var orderItemsAttributes: List<OrderItemsAttribute> = emptyList()

    @SerializedName("guests_attributes")
    var guestsAttributes: List<GuestsAttributes> = emptyList()

    @SerializedName("dine_in_order_detail_attributes")
    var dineInOrderDetailsAttr: DineInOrderDetailAttributes? = null

    @SerializedName("order_service_charges_attributes")
    var orderServiceChargesAttributes: List<OrderServiceChargesAttribute> = emptyList()

    @SerializedName("order_type_id")
    var orderTypeId: Int = 0

    @SerializedName("payment_attributes")
    var paymentAttributes: PaymentAttributes? = null

    @SerializedName("payment_status")
    var paymentStatus: Int = 0

    @SerializedName("service_charge_enabled")
    var serviceChargeEnabled: Boolean = false

    @SerializedName("sub_total")
    var subTotal: Double = 0.0

    @SerializedName("tax_enabled")
    var taxEnabled: Boolean = false

    @SerializedName("terminal_id")
    var terminalId: Int = 0

    @SerializedName("total_amount")
    var totalAmount: Double = 0.0

    @SerializedName("magensa_response")
    var magensaResponse: MagensaResponse? = null


    @SerializedName("tax_bifurcation_data")
    var tax_bifurcation_data: String = ""

    @SerializedName("cash_discount_or_surcharge")
    var cash_discount_or_surcharge: Double = 0.0

    @SerializedName("cash_discount_type")
    var cash_discount_type: String = ""

    @SerializedName("total_discount")
    var totalDiscount: Double = 0.0

    @SerializedName("total_service_charges")
    var totalServiceCharges: Double = 0.0

    @SerializedName("total_tax_amount")
    var totalTaxAmount: Double = 0.0

    @SerializedName("total_tips")
    var totalTips: Double = 0.0

    @SerializedName("customer_id")
    var customer_id: String? = null

    @SerializedName("discount_id")
    var discount_id: Int? = null

    @SerializedName("loyalty_program_id")
    var loyalty_program_id: String = ""

    @SerializedName("loyalty_amount")
    var loyalty_amount: Double? = 0.0

    @SerializedName("used_reward_points")
    var used_reward_points: Int? = 0

    @SerializedName("merged_order_ids")
    var mergedOrderIds: ArrayList<Int> = arrayListOf()

    @SerializedName("is_loyalty_applied")
    var is_loyalty_applied: Boolean? = false

    @SerializedName("merged_table_nos")
    var mergedTableNumbers: Int? = null

    @SerializedName("deleted_guest_items")
    var deletedGuestItems: ArrayList<Int> = arrayListOf()
}


data class CustomerAttributes(
    @SerializedName("addresses_attributes")
    var addressesAttributes: List<AddressesAttribute> = emptyList(),
    @SerializedName("birth_date")
    var birthDate: String = "",
    @SerializedName("company_name")
    var companyName: String = "",
    @SerializedName("emails_attributes")
    var emailsAttributes: List<EmailsAttribute> = emptyList(),
    @SerializedName("first_name")
    var firstName: String = "",
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("last_name")
    var lastName: String = "",
    @SerializedName("location_id")
    var locationId: Int = 0,
    @SerializedName("note")
    var note: String = "",
    @SerializedName("phones_attributes")
    var phonesAttributes: List<PhonesAttribute> = emptyList()
) {
    data class AddressesAttribute(
        @SerializedName("address_1")
        var address1: String = "",
        @SerializedName("address_2")
        var address2: String = "",
        @SerializedName("address_3")
        var address3: String = "",
        @SerializedName("addressable_id")
        var addressableId: Int? = null,
        @SerializedName("addressable_type")
        var addressableType: String = "",
        @SerializedName("city")
        var city: String = "",
        @SerializedName("country")
        var country: String = "",
        @SerializedName("_destroy")
        var destroy: Boolean = false,
        @SerializedName("id")
        var id: Int? = null,
        @SerializedName("latitude")
        var latitude: Double = 0.0,
        @SerializedName("longitude")
        var longitude: Double = 0.0,
        @SerializedName("postcode")
        var postcode: String = "",
        @SerializedName("state")
        var state: String = "",
        @SerializedName("type_of_address")
        var typeOfAddress: String = ""
    )

    data class EmailsAttribute(
        @SerializedName("customer_id")
        var customerId: Int? = null,
        @SerializedName("_destroy")
        var destroy: Boolean = false,
        @SerializedName("email_address")
        var emailAddress: String = "",
        @SerializedName("id")
        var id: Int? = null
    )

    data class PhonesAttribute(
        @SerializedName("customer_id")
        var customerId: Int? = null,
        @SerializedName("_destroy")
        var destroy: Boolean = false,
        @SerializedName("id")
        var id: Int? = null,
        @SerializedName("phone_number")
        var phoneNumber: String = ""
    )
}

class GuestsAttributes(

    @SerializedName("id") var id: Int? = null,
    @SerializedName("order_id") var orderId: Int? = null,
    @SerializedName("name") var name: String = "",
    @SerializedName("is_paid") var isPaid: Boolean = false,
    @SerializedName("total_amount") var totalAmount: Double? = null,
    @SerializedName("cash_discount") var cashDiscount: Double? = null,
    @SerializedName("total_discount") var totalDiscount: Double? = null,
    @SerializedName("total_service_charge") var totalServiceCharge: Double? = null,
    @SerializedName("sub_total") var subTotal: Double? = null,
    @SerializedName("total_tax") var totalTax: Double? = null,
    @SerializedName("total_tips") var totalTips: Double? = null,
    @SerializedName("customer_id") var customerId: Int? = null,
    @SerializedName("_destroy") var Destroy: Boolean? = null,
    @SerializedName("guest_items_attributes") var guestItemsAttributes: List<GuestItemsAttributes> = emptyList(),
    @SerializedName("customer_attributes") var customerAttributes: CustomerAttributes? = null,
    @SerializedName("is_child_guest") var isChildGuest: Boolean = false,
    @SerializedName("child_merge_id") var childMergeId: Int? = null,


    )

class GuestItemsAttributes(

    @SerializedName("id") var id: Int? = null,
    @SerializedName("order_id") var orderId: Int? = null,
    @SerializedName("order_item_id") var orderItemId: Int? = null,
    @SerializedName("quantity") var quantity: Int? = null,
    @SerializedName("item_id") var itemId: Int? = null,
    @SerializedName("amount") var amount: Double? = null,
    @SerializedName("is_paid") var isPaid: Boolean = false,
    @SerializedName("guest_id") var guestId: Int? = null,
    @SerializedName("_destroy") var Destroy: Boolean = false,
    @SerializedName("percentage") var percentage: Int? = null,
    @SerializedName("timestamp") var timestamp: String? = null,
    @SerializedName("is_fired") var isFired: Boolean = false


)

class OrderItemsAttribute {
    @SerializedName("category_id")
    var category_id: Int = -1

    @SerializedName("custom_item_id")
    var custom_item_id: Int = 0

    @SerializedName("discount_amount")
    var discountAmount: Double = 0.0

    @SerializedName("discount_id")
    var discountId: Int? = null

    @SerializedName("discount_type")
    var discountType: String = ""

    @SerializedName("edit_timestamp")
    var editTimestamp: String? = null

    @SerializedName("employee_id")
    var employeeId: Int = 0

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("is_count")
    var isCount: Int = 0

    @SerializedName("is_edited")
    var isEdited: Boolean = false

    @SerializedName("is_item_edited")
    var isItemEdited: Boolean = false

    @SerializedName("is_paid")
    var isPaid: Boolean = false

    @SerializedName("is_printed")
    var isPrinted: Boolean = false

    @SerializedName("is_tax_removed")
    var isTaxRemoved: Boolean = false

    @SerializedName("item_id")
    var itemId: Int = 0

    @SerializedName("item_name")
    var itemName: String = ""

    @SerializedName("note")
    var note: String = ""

    @SerializedName("order_id")
    var orderId: Int? = null

    @SerializedName("order_item_taxes_attributes")
    var orderItemTaxesAttributes: List<OrderItemTaxesAttribute> = emptyList()

    @SerializedName("price")
    var price: Double = 0.0

    @SerializedName("quantity")
    var quantity: Int = 0

    @SerializedName("terminal_id")
    var terminalId: Int = 0

    @SerializedName("timestamp")
    var timestamp: String = ""

    @SerializedName("total_price")
    var totalPrice: Double = 0.0

    @SerializedName("is_manual_sales")
    var is_manual_sales: Boolean = false

    @SerializedName("order_item_modifiers_attributes")
    var orderItemModifiersAttributes: List<OrderItemModifierAttribute> = emptyList()

    @SerializedName("order_items_variation_attributes")
    var orderItemVariationAttributes: OrderItemVariationAttribute? = null

    @SerializedName("variation_id")
    var variationId: Int? = null

    @SerializedName("_destroy")
    var isDestroy: Boolean = false

    @SerializedName("is_fired")
    var isFired: Boolean = false

    @SerializedName("sort")
    var sort: Int? = null

    @SerializedName("guest_index_for_dine_in")
    var guestIndexForDineIn: Int? = 0
}

class OrderItemTaxesAttribute {
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("is_default")
    var isDefault: Boolean = false

    @SerializedName("is_tax_removed")
    var isTaxRemoved: Boolean = false

    @SerializedName("name")
    var name: String = ""

    @SerializedName("order_id")
    var orderId: Int? = null

    @SerializedName("order_item_id")
    var orderItemId: Int? = null

    @SerializedName("rate")
    var rate: Double = 0.0

    @SerializedName("tax_id")
    var taxId: Int = 0

    @SerializedName("tax_total_amount")
    var taxTotalAmount: Double = 0.0

    @SerializedName("tax_type")
    var taxType: String = ""
}

class OrderModifierTaxesAttribute {
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("order_item_id")
    var order_item_id: Int? = null

    @SerializedName("order_item_modifier_id")
    var order_item_modifier_id: Int? = null

    @SerializedName("order_id")
    var order_id: Int? = null

    @SerializedName("tax_id")
    var tax_id: Int? = null

    @SerializedName("amount")
    var amount: Double = 0.0

    @SerializedName("is_default")
    var isDefault: Boolean = false

    @SerializedName("is_tax_removed")
    var is_tax_removed: Boolean = false

    @SerializedName("is_modifier")
    var is_modifier: Boolean = true

    @SerializedName("category_id")
    var category_id: Int = 0

    @SerializedName("terminal_id")
    var terminal_id: Int = 0

    @SerializedName("modifier_id")
    var modifier_id: Int = 0

    @SerializedName("timestamp")
    var timestamp: String = ""

    @SerializedName("name")
    var name: String = ""

    @SerializedName("tax_total_amount")
    var taxTotalAmount: Double = 0.0

    @SerializedName("tax_type")
    var taxType: String = ""


}

@Parcelize
class OrderItemModifierAttribute : Parcelable {
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("order_item_modifier_id")
    var order_item_modifier_id: Int? = null

    @SerializedName("order_item_id")
    var order_item_id: Int? = null

    @SerializedName("name")
    var name: String = ""

    @SerializedName("order_id")
    var orderId: Int? = null

    @SerializedName("modifier_id")
    var modifier_id: Int? = null

    @SerializedName("price")
    var price: Double = 0.0

    @SerializedName("modifier_quantity")
    var modifier_quantity: Int = 1

    @SerializedName("total_price")
    var totalPrice: Double = 0.0

    @SerializedName("quantity")
    var quantity: Int = 0

    @SerializedName("modifier_set_id")
    var modifier_set_id: Int = 0

    @SerializedName("_destroy")
    var _destroy: Boolean = false

    @SerializedName("order_item_taxes_attributes")
    var order_item_taxes_attributes: List<OrderModifierTaxesAttribute> = emptyList()

}

class OrderItemVariationAttribute {
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("order_item_id")
    var order_item_id: Int? = null

    @SerializedName("name")
    var name: String = ""

    @SerializedName("order_id")
    var orderId: Int? = null

    @SerializedName("unit_price")
    var price: Double = 0.0

    @SerializedName("total_price")
    var totalPrice: Double = 0.0

    @SerializedName("quantity")
    var quantity: Int = 0

    @SerializedName("variation_id")
    var variationId: Int = 0
}

@Parcelize
data class OrderServiceChargesAttribute(
    @SerializedName("amount")
    var amount: Double = 0.0,
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("order_id")
    var orderId: Int? = null,
    @SerializedName("rate")
    var rate: Double = 0.0,
    @SerializedName("service_charge_id")
    var serviceChargeId: Int = 0,
    @SerializedName("order_type")
    var order_type: String? = null,
    @SerializedName("min_guest_count")
    var min_guest_count: Int? = null,
    @SerializedName("max_guest_count")
    var max_guest_count: Int? = null
) : Parcelable


@Parcelize
class MagensaResponse : Parcelable {
    @SerializedName("CustomerTransactionID")
    var customerTransactionID: String = ""

    @SerializedName("DataOutput")
    var dataOutput: DataOutput? = null

}

@Parcelize
class DataOutput : Parcelable {
    @SerializedName("PANLast4")
    var panLast4: String? = ""
}


@Parcelize
class PaymentAttributes : Parcelable {
    @SerializedName("amount")
    var amount: Double = 0.0

    @SerializedName("card_name")
    var cardName: String = ""

    @SerializedName("card_number")
    var cardNumber: String = ""

    @SerializedName("card_type")
    var cardType: Int? = null

    @SerializedName("cash_discount_or_surcharge")
    var cash_discount_or_surcharge: Double = 0.0

    @SerializedName("cash_discount_fee")
    var cashDiscountFee: Double = 0.0


    @SerializedName("cash_discount_type")
    var cash_discount_type: String = ""

    @SerializedName("total_cash_discount")
    var total_cash_discount: Double = 0.0

    @SerializedName("employee_id")
    var employeeId: Int = 0

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("offline_id")
    var offlineId: String = ""

    @SerializedName("payable_type")
    var payableType: String = ""

    @SerializedName("payment_type")
    var paymentType: String? = ""

    @SerializedName("dynamic_payment_id")
    var dynamicPaymentId: String? = ""

    @SerializedName("service_charge_amount")
    var serviceChargeAmount: Double = 0.0

    @SerializedName("sub_total")
    var subTotal: Double = 0.0

    @SerializedName("tip_setting_id")
    var tipId: Int? = null

    @SerializedName("tax_amount")
    var taxAmount: Double = 0.0

    @SerializedName("terminal_id")
    var terminalId: Int = 0

    @SerializedName("order_id")
    var order_id: Int? = null

    @SerializedName("tips")
    var tips: Double = 0.0

    @SerializedName("tip_with_surcharge_percentage")
    var tipWithSurchargePercentage: Double = 0.0

    @SerializedName("tips_adjusted")
    var tipsAdjusted: Boolean = false

    @SerializedName("total_discount")
    var totalDiscount: Double = 0.0

    @SerializedName("transaction_id")
    var transactionId: String = ""

    @SerializedName("loyalty_program_id")
    var loyalty_program_id: String = ""

    @SerializedName("magensa_response")
    var magensa_response: String = ""

    @SerializedName("loyalty_amount")
    var loyalty_amount: Double? = 0.0

    @SerializedName("used_reward_points")
    var used_reward_points: Int? = 0

    @SerializedName("is_loyalty_applied")
    var is_loyalty_applied: Boolean? = false

    @SerializedName("gift_card_redeemed_amount")
    var gift_card_redeemed_amount: Double? = 0.0

    @SerializedName("global_uniq_id")
    var global_uniq_id: String = ""

    @SerializedName("ref_num")
    var ref_num: String = ""

    @SerializedName("ecr_ref_num")
    var ecr_ref_num: String = ""

    @SerializedName("pax_transaction_token")
    var pax_transaction_token: String = ""

    @SerializedName("ext_data")
    var ext_data: String = ""

}
