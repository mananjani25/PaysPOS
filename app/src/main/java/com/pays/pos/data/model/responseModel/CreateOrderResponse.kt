package com.pays.pos.data.model.responseModel


import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


data class CreateOrderResponse(

    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {


    data class Data(
        @SerializedName("order")
        val order: Order
    ) : Parcelable {

        data class Order(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("creation_time_on_terminal")
            val creationTimeOnTerminal: Any,
            @SerializedName("customer")
            val customer: Customer,
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
            val employee: Employee,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("future_delivery_date")
            val futureDeliveryDate: Any,
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
            var orderItems: List<OrderItem>,
            @SerializedName("order_service_charges")
            val orderServiceCharges: List<OrderServiceCharge>,
            @SerializedName("order_type")
            val orderType: String,
            @SerializedName("order_type_id")
            val orderTypeId: Int,
            @SerializedName("payment_status")
            val paymentStatus: String,
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
            val totalCashDiscountFee: Double,
            @SerializedName("cash_discount_type")
            val cash_discount_type: String = "",
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
            @SerializedName("digital_receipt_url")
            val digital_receipt_url: String,
            @SerializedName("guest_attributes")
            var guestAttributes: List<GuestAttributes>,
            @SerializedName("venue_website")
            val venue_website: String,
            @SerializedName("loyalty_amount")
            val loyaltyAmount: Double,
            @SerializedName("order_type_name")
            val orderTypeName: String,
        ) {

            @Parcelize
            data class GuestAttributes(
                @SerializedName("id") var id: Int,
                @SerializedName("order_id") var orderId: Int,
                @SerializedName("name") var name: String,
                @SerializedName("is_paid") var isPaid: Boolean,
                @SerializedName("total_amount") var totalAmount: Double,
                @SerializedName("cash_discount_or_surcharge") var cashDiscount: Double,
                @SerializedName("total_discount") var totalDiscount: Double,
                @SerializedName("total_service_charge") var totalServiceCharge: Double,
                @SerializedName("sub_total") var subTotal: Double,
                @SerializedName("total_tax") var totalTax: Double,
                @SerializedName("total_tips") var totalTips: Double,
                @SerializedName("customer_id") var customerId: Int,
                @SerializedName("guest_item_attributes") var guestItemAttributes: List<GuestItemAttributes>

            ) : Parcelable {
                @Parcelize
                data class GuestItemAttributes(
                    @SerializedName("id") var id: Int,
                    @SerializedName("order_id") var orderId: Int,
                    @SerializedName("order_item_id") var orderItemId: Int?,
                    @SerializedName("quantity") var quantity: Int,
                    @SerializedName("item_id") var itemId: Int,
                    @SerializedName("amount") var amount: Double,
                    @SerializedName("is_paid") var isPaid: Boolean,
                    @SerializedName("guest_id") var guestId: Int,
                    @SerializedName("created_at") var createdAt: String,
                    @SerializedName("updated_at") var updatedAt: String,
                    @SerializedName("item_type") var itemType: String,
                    @SerializedName("timestamp") var timestamp: String,
                    @SerializedName("is_fired") var is_fired: Boolean,
                    @SerializedName("sort") var sort: Int? = null,


                    ) : Parcelable
            }

            data class Customer(

                @SerializedName("id") var id: Int,
                @SerializedName("first_name") var firstName: String,
                @SerializedName("last_name") var lastName: String,
                @SerializedName("company") var company: String,
                @SerializedName("location_id") var locationId: Int,
                @SerializedName("created_at") var createdAt: String,
                @SerializedName("enroll_to_loyalty") var enroll_to_loyalty: Boolean,
                @SerializedName("final_reward") var final_reward: String,
                @SerializedName("updated_at") var updatedAt: String,
                @SerializedName("birth_date") var birthDate: String,
                @SerializedName("email") var email: String,
                @SerializedName("phones") var phones: List<Phones>,
                @SerializedName("addresses") var addresses: List<Addresses>

            ) {
                data class Phones(
                    @SerializedName("id") var id: Int,
                    @SerializedName("phone_number") var phoneNumber: String
                )

                data class Addresses(

                    @SerializedName("id") var id: Int,
                    @SerializedName("address1") var address1: String,
                    @SerializedName("address2") var address2: String,
                    @SerializedName("city") var city: String,
                    @SerializedName("state") var state: String,
                    @SerializedName("country") var country: String,
                    @SerializedName("postcode") var postcode: String,
                    @SerializedName("type_of_address") var typeOfAddress: String,
                    @SerializedName("latitude") var latitude: String,
                    @SerializedName("longitude") var longitude: String,
                    @SerializedName("full_address") var fullAddress: String,
                    @SerializedName("street") var street: String

                )
            }

            @Parcelize
            data class OrderItem(
                @SerializedName("category_id")
                val categoryId: Int = 0,
                @SerializedName("completed_in_kitchen")
                val completedInKitchen: Boolean = false,
                @SerializedName("is_edited")
                var isEdited: Boolean = false,
                @SerializedName("discount_amount")
                val discountAmount: Double = 0.0,
                @SerializedName("discount_id")
                val discountId: Int = 0,
                @SerializedName("discount_type")
                val discountType: String = "",
                @SerializedName("employee_id")
                val employeeId: Int = 0,
                @SerializedName("float")
                val float: Double = 0.0,
                @SerializedName("id")
                val id: Int = 0,
                @SerializedName("is_paid")
                val isPaid: Boolean = false,
                @SerializedName("is_printed")
                val isPrinted: Boolean = false,
                @SerializedName("item_id")
                val itemId: Int = 0,
                @SerializedName("item_name")
                var itemName: String = "",
                @SerializedName("note")
                val note: String = "",
                @SerializedName("order_id")
                val orderId: Int = 0,
                @SerializedName("order_item_modifiers")
                val orderItemModifiers: List<OrderItemModifiers> = listOf(),
                @SerializedName("price")
                var price: Double = 0.0,
                @SerializedName("quantity")
                var quantity: Int = 0,
                @SerializedName("timestamp")
                val timestamp: String = "",
                @SerializedName("total_price")
                val totalPrice: Double = 0.0,
                @SerializedName("guest_index_for_dine_in")
                var guestIndexForDineIn : Int? = 0
            ) : Parcelable {
                @Parcelize
                data class OrderItemModifiers(

                    @SerializedName("id") var id: Int,
                    @SerializedName("order_item_id") var orderItemId: Int,
                    @SerializedName("name") var name: String,
                    @SerializedName("price") var price: Double,
                    @SerializedName("quantity") var quantity: Int,
                    @SerializedName("order_id") var orderId: Int,
                    @SerializedName("modifier_set_id") var modifierSetId: Int,
                    @SerializedName("is_modifier") var isModifier: Boolean,
                    @SerializedName("created_at") var createdAt: String,
                    @SerializedName("updated_at") var updatedAt: String,
                    @SerializedName("total_price") var totalPrice: Double,
                    @SerializedName("modifier_quantity") var modifierQuantity: Int =0

                ) : Parcelable
            }

            data class Employee(

                @SerializedName("id") var id: Int,
                @SerializedName("name") var name: String,
                @SerializedName("email") var email: String,
                @SerializedName("phone_number") var phoneNumber: String,
                @SerializedName("location_id") var locationId: Int,
                @SerializedName("passcode") var passcode: String,
                @SerializedName("is_active") var isActive: Boolean,
                @SerializedName("created_at") var createdAt: String,
                @SerializedName("updated_at") var updatedAt: String,
                @SerializedName("loggedin_terminal_id") var loggedinTerminalId: Int,
                @SerializedName("is_clocked_in") var isClockedIn: Boolean,
                @SerializedName("first_name") var firstName: String,
                @SerializedName("last_name") var lastName: String,
                @SerializedName("team_role_id") var teamRoleId: Int,
                @SerializedName("hourly_wages") var hourlyWages: Double

            )

            data class OrderServiceCharge(
                @SerializedName("amount")
                val amount: Double,
                @SerializedName("created_at")
                val createdAt: String,
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
                val updatedAt: String
            )

            data class Payment(
                @SerializedName("amount")
                val amount: Double,
                @SerializedName("card_name")
                val cardName: String,
                @SerializedName("card_number")
                val cardNumber: String,
                @SerializedName("card_type")
                val cardType: String,
                @SerializedName("created_at")
                val createdAt: String,
                @SerializedName("cash_discount_or_surcharge")
                val cash_discount_or_surcharge: Double,
                @SerializedName("total_cash_discount")
                val totalcashdiscount: Double,
                @SerializedName("cash_discount_type")
                var cash_discount_type: String = "",
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
                @SerializedName("is_loyalty_applied")
                val isLoyaltyApplied: Boolean = false,
                @SerializedName("used_reward_points")
                val loyaltyUSedPoints: Int = 0

            )
        }


        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {

        }

        companion object CREATOR : Parcelable.Creator<Data> {
            override fun createFromParcel(parcel: Parcel): Data {
                return Data(TODO("Order"))
            }

            override fun newArray(size: Int): Array<Data?> {
                return arrayOfNulls(size)
            }
        }
    }
}