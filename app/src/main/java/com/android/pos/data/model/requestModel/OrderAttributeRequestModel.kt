package com.android.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

class OrderAttributeRequestModel {

    @SerializedName("customer_attributes")
    val customerAttributes: CustomerAttributes? = null

    @SerializedName("date")
    var date: String = ""

    @SerializedName("delivery_type")
    var deliveryType: String = ""

    @SerializedName("employee_id")
    var employeeId: Int = 0

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
    var offlineId: String = ""

    @SerializedName("open_order_type")
    var openOrderType: String = ""

    @SerializedName("order_items_attributes")
    var orderItemsAttributes: List<OrderItemsAttribute> = emptyList()

    @SerializedName("order_service_charges_attributes")
    var orderServiceChargesAttributes: List<OrderServiceChargesAttribute> = emptyList()

    @SerializedName("order_type_id")
    var orderTypeId: Int = 0

    @SerializedName("payment_attributes")
    var paymentAttributes: PaymentAttributes? = null

    @SerializedName("payment_status")
    var paymentStatus: String = ""

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

//    @SerializedName("total_cash_discount")
//    var totalCashDiscount: Double = 0.0

    @SerializedName("total_discount")
    var totalDiscount: Double = 0.0

    @SerializedName("total_service_charges")
    var totalServiceCharges: Double = 0.0

    @SerializedName("total_tax_amount")
    var totalTaxAmount: Double = 0.0

    @SerializedName("total_tips")
    var totalTips: Double = 0.0
}


data class CustomerAttributes(
    @SerializedName("addresses_attributes")
    val addressesAttributes: List<AddressesAttribute>,
    @SerializedName("birth_date")
    val birthDate: String,
    @SerializedName("company_name")
    val companyName: String,
    @SerializedName("emails_attributes")
    val emailsAttributes: List<EmailsAttribute>,
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
    @SerializedName("phones_attributes")
    val phonesAttributes: List<PhonesAttribute>
) {
    data class AddressesAttribute(
        @SerializedName("address_1")
        val address1: String,
        @SerializedName("address_2")
        val address2: String,
        @SerializedName("address_3")
        val address3: String,
        @SerializedName("addressable_id")
        val addressableId: Int,
        @SerializedName("addressable_type")
        val addressableType: String,
        @SerializedName("city")
        val city: String,
        @SerializedName("country")
        val country: String,
        @SerializedName("_destroy")
        val destroy: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("latitude")
        val latitude: Int,
        @SerializedName("longitude")
        val longitude: Int,
        @SerializedName("postcode")
        val postcode: String,
        @SerializedName("state")
        val state: String,
        @SerializedName("type_of_address")
        val typeOfAddress: String
    )

    data class EmailsAttribute(
        @SerializedName("customer_id")
        val customerId: Int,
        @SerializedName("_destroy")
        val destroy: String,
        @SerializedName("email_address")
        val emailAddress: String,
        @SerializedName("id")
        val id: Int
    )

    data class PhonesAttribute(
        @SerializedName("customer_id")
        val customerId: Int,
        @SerializedName("_destroy")
        val destroy: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("phone_number")
        val phoneNumber: String
    )
}

class OrderItemsAttribute {
    @SerializedName("category_id")
    var categoryId: Int = 0

    @SerializedName("discount_amount")
    var discountAmount: Double = 0.0

    @SerializedName("discount_total_amount")
    var discountTotalAmount: Double = 0.0

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

    @SerializedName("rate")
    var rate: Double = 0.0

    @SerializedName("tax_id")
    var taxId: Int = 0

    @SerializedName("tax_total_amount")
    var taxTotalAmount: Double = 0.0

    @SerializedName("tax_type")
    var taxType: String = ""
}


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
    var serviceChargeId: Int = 0
)

class PaymentAttributes {
    @SerializedName("amount")
    var amount: Double = 0.0

    @SerializedName("card_name")
    var cardName: String = ""

    @SerializedName("card_number")
    var cardNumber: String = ""

    @SerializedName("card_type")
    var cardType: Int = 0

    @SerializedName("cash_discount")
    var cashDiscount: Double = 0.0

    @SerializedName("cash_discount_fee")
    var cashDiscountFee: Double = 0.0

    @SerializedName("employee_id")
    var employeeId: Int = 0

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("offline_id")
    var offlineId: String = ""

    @SerializedName("payable_type")
    var payableType: String = ""

    @SerializedName("payment_type")
    var paymentType: String = ""

    @SerializedName("service_charge_amount")
    var serviceChargeAmount: Double = 0.0

    @SerializedName("sub_total")
    var subTotal: Double = 0.0

    @SerializedName("tax_amount")
    var taxAmount: Double = 0.0

    @SerializedName("terminal_id")
    var terminalId: Int = 0

    @SerializedName("tips")
    var tips: Double = 0.0

    @SerializedName("tips_adjusted")
    var tipsAdjusted: Boolean = false

    @SerializedName("total_discount")
    var totalDiscount: Double = 0.0

    @SerializedName("transaction_id")
    var transactionId: String = ""
}
