package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class GetOrderDetailsResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("creation_time_on_terminal")
        val creationTimeOnTerminal: String?,
        @SerializedName("customer")
        val customer: Customer?=null,
        @SerializedName("customer_id")
        val customerId: Int,
        @SerializedName("date")
        val date: String?=null,
        @SerializedName("delivery_employee_id")
        val deliveryEmployeeId: String?,
        @SerializedName("delivery_type")
        val deliveryType: String,
        @SerializedName("digital_receipt_url")
        val digitalReceiptUrl: String,
        @SerializedName("discount_type_id")
        val discountTypeId: String?,
        @SerializedName("dynamic_discount_id")
        val dynamicDiscountId: String?,
        @SerializedName("edit_order_count")
        val editOrderCount: String?,
        @SerializedName("edited_order_timestamp")
        val editedOrderTimestamp: String?,
        @SerializedName("employee")
        val employee: Employee,
        @SerializedName("refund_detail")
        val refundDetails: RefundDetails,
        @SerializedName("employee_id")
        val employeeId: Int,
        @SerializedName("future_delivery_date")
        val futureDeliveryDate: String?,
        @SerializedName("future_delivery_time")
        val futureDeliveryTime: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("custom_order_id")
        val custom_order_id: Int,
        @SerializedName("index_of_date")
        val indexOfDate: String?,
        @SerializedName("integer")
        val integer: String?,
        @SerializedName("is_edited")
        val isEdited: Boolean,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("note")
        val note: String,
        @SerializedName("offline_id")
        val offlineId: String,
        @SerializedName("open_order_type")
        val openOrderType: String?,
        @SerializedName("open_order_type_id")
        val openOrderTypeId: String?,
        @SerializedName("order_items")
        val orderItems: List<OrderItem>,
        @SerializedName("order_service_charges")
        val orderServiceCharges: List<OrderServiceCharge>,
        @SerializedName("order_type")
        val orderType: String,
        @SerializedName("order_type_id")
        val orderTypeId: Int,
        @SerializedName("payment_status")
        val paymentStatus: String,
        @SerializedName("payments")
        var payments: List<Payment>,
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
        @SerializedName("guest_attributes")
        var guestAttributes: List<CreateOrderResponse.Data.Order.GuestAttributes>,
        @SerializedName("floor_plan_table")
        var floorPlanTable: FloorPlanTable,
        @SerializedName("cash_discount_or_surcharge")
        val cash_discount_or_surcharge: Double,
        @SerializedName("total_discount")
        val totalDiscount: Double,
        @SerializedName("total_service_charges")
        val totalServiceCharges: Double,
        @SerializedName("total_tax_amount")
        val totalTaxAmount: Double,
        @SerializedName("total_tips")
        val totalTips: Double,
        @SerializedName("updated_at")
        val updatedAt: String,
        @SerializedName("venue_website")
        val venueWebsite: String,
        @SerializedName("merged_table_nos")
        val mergedTableNos: String = "",
        @SerializedName("order_type_name")
        val orderTypeName: String,

    ) : Parcelable {
        @Parcelize
        data class Customer(
            @SerializedName("addresses")
            val addresses: List<Addresse>,
            @SerializedName("birth_date")
            val birthDate: String,
            @SerializedName("company")
            val company: String,
            @SerializedName("email")
            val email: String,
            @SerializedName("first_name")
            val firstName: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("last_name")
            val lastName: String,
            @SerializedName("phones")
            val phones: List<Phone>,
            @SerializedName("enroll_to_loyalty")
            val enroll_to_loyalty: Boolean?,
            @SerializedName("same_as_billing_address")
            val same_as_billing_address: Boolean?,
            @SerializedName("final_reward")
            val final_reward: Int? = 0
        ) : Parcelable {
            @Parcelize
            data class Addresse(
                @SerializedName("address1")
                val address1: String,
                @SerializedName("address2")
                val address2: String,
                @SerializedName("city")
                val city: String,
                @SerializedName("country")
                val country: String,
                @SerializedName("full_address")
                val fullAddress: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("latitude")
                val latitude: String,
                @SerializedName("longitude")
                val longitude: String,
                @SerializedName("postcode")
                val postcode: String,
                @SerializedName("state")
                val state: String,
                @SerializedName("street")
                val street: String,
                @SerializedName("type_of_address")
                val typeOFAddress: String
            ) : Parcelable

            @Parcelize
            data class Phone(
                @SerializedName("id")
                val id: Int,
                @SerializedName("phone_number")
                val phoneNumber: String
            ) : Parcelable
        }

        @Parcelize
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
        ) : Parcelable

        @Parcelize
        data class FloorPlanTable(
            @SerializedName("id") var id: Int? = null,
            @SerializedName("x_position") var xPosition: Double? = null,
            @SerializedName("y_position") var yPosition: Double? = null,
            @SerializedName("table_name") var tableName: String? = null,
            @SerializedName("table_number") var tableNumber: Int? = null,
            @SerializedName("chair_count") var chairCount: Int? = null,
            @SerializedName("floor_plan_id") var floorPlanId: Int? = null,
            @SerializedName("table_type") var tableType: String? = null,
            @SerializedName("status") var status: String? = null,
            @SerializedName("height") var height: Double? = null,
            @SerializedName("width") var width: Double? = null,
            @SerializedName("style") var style: String? = null,
            @SerializedName("created_at") var createdAt: String? = null,
            @SerializedName("updated_at") var updatedAt: String? = null,
            @SerializedName("merged_floor_plan_table_id") var mergedFloorPlanTableId: String? = null,
            @SerializedName("lock_by_id") var lockById: Int? = null,
            @SerializedName("lock_by_name") var lockByName: String? = null,
            @SerializedName("terminal_id") var terminalId: Int? = null,
            @SerializedName("merged_child_table_details") val merged_child_table_details: List<GetFloorPlanResponse.Data.FloorPlanTable.MergedChildTableDetails>? = null

        ) : Parcelable

        @Parcelize
        data class RefundDetails(
            @SerializedName("refunded_quantity")
            val refundedQuantity: Int,
            @SerializedName("refunded_amount")
            val refundedAmount: Double,
        ) : Parcelable

        @Parcelize
        data class OrderItem(
            @SerializedName("category_id")
            val categoryId: Int,
            @SerializedName("custom_item_id")
            var custom_item_id: Int,
            @SerializedName("completed_in_kitchen")
            val completedInKitchen: Boolean,
            @SerializedName("discount_amount")
            val discountAmount: Double,
            @SerializedName("discount_id")
            val discountId: Int?,
            @SerializedName("discount_type")
            val discountType: String?=null,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("float")
            val float: Double,
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_paid")
            val isPaid: Boolean,
            @SerializedName("is_fired")
            val isFired: Boolean,
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
            var orderItemModifiers: List<OrderItemModifier>,
            @SerializedName("order_item_taxes")
            val orderItemTaxes: List<OrderItemTaxe>,
            @SerializedName("price")
            var price: Double,
            @SerializedName("quantity")
            var quantity: Int,
            @SerializedName("timestamp")
            val timestamp: String,
            @SerializedName("total_price")
            val totalPrice: Double,
            @SerializedName("refunded_quantity")
            val refundedQuantity: Int,
            @SerializedName("refunded_amount")
            val refundedAmount: Double,
            var isChecked: Boolean = false,
            @SerializedName("order_item_variation")
            val order_item_variation: OrderItemVariationAttribute?,
            @SerializedName("sort")
            val sort: Int = 0,
            @SerializedName("guest_index_for_dine_in")
            var guestIndexForDineIn : Int? = 0,
            @SerializedName("deducted_price")
            var deductedPrice: Double=0.0,
        ) : Parcelable {

            @Parcelize
            class OrderItemVariationAttribute() : Parcelable {
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
            data class OrderItemModifier(
                @SerializedName("category_id")
                val categoryId: String?,
                @SerializedName("id")
                val id: Int,
                @SerializedName("modifier_quantity")
                val modifier_quantity: Int?=null,
                @SerializedName("is_modifier")
                val isModifier: Boolean,
                @SerializedName("item_id")
                val itemId: String?,
                @SerializedName("modifier_id")
                val modifierId: Int?,
                @SerializedName("modifier_set_id")
                val modifier_set_id: Int?,
                @SerializedName("name")
                val name: String,
                @SerializedName("order_id")
                val orderId: Int,
                @SerializedName("order_item_id")
                val orderItemId: Int,
                @SerializedName("order_item_taxes")
                val orderItemTaxes: List<OrderItemTaxe>,
                @SerializedName("price")
                val price: Double,
                @SerializedName("quantity")
                val quantity: Int,
                @SerializedName("timestamp")
                val timestamp: String?
            ) : Parcelable {
                @Parcelize
                data class OrderItemTaxe(
                    @SerializedName("amount")
                    val amount: Double,
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
                    val orderItemModifierId: Int,
                    @SerializedName("rate")
                    val rate: Double?,
                    @SerializedName("tax_id")
                    val taxId: Int,
                    @SerializedName("tax_total_amount")
                    val taxTotalAmount: Double,
                    @SerializedName("updated_at")
                    val updatedAt: String,
                    @SerializedName("tax_type")
                    val taxType: String
                ) : Parcelable
            }

            @Parcelize
            data class OrderItemTaxe(
                @SerializedName("amount")
                val amount: Double?,
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
                val orderItemModifierId: Int?,
                @SerializedName("rate")
                val rate: Double,
                @SerializedName("tax_id")
                val taxId: Int,
                @SerializedName("tax_total_amount")
                val taxTotalAmount: Double,
                @SerializedName("updated_at")
                val updatedAt: String,
                @SerializedName("tax_type")
                val taxType: String

            ) : Parcelable
        }

        @Parcelize
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
            val min_guest_count: Int?=null,
            @SerializedName("max_guest_count")
            val max_guest_count: Int?=null,
            @SerializedName("order_type")
            val order_type: String,
        ) : Parcelable

        @Parcelize
        data class Payment(
            @SerializedName("amount")
            val amount: Double,
            @SerializedName("card_name")
            val cardName: String,
            @SerializedName("card_number")
            val cardNumber: String,
            @SerializedName("card_type")
            val cardType: String?,
            @SerializedName("cash_discount_or_surcharge")
            val cash_discount_or_surcharge: Double,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("dynamic_payment_id")
            val dynamicPaymentId: Int?,
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
            val updatedAt: String
        ) : Parcelable

    }
}