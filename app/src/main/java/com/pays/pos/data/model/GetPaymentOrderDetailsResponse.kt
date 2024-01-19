package com.pays.pos.data.model

import android.os.Parcelable
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
class GetPaymentOrderDetailsResponse(
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
        @SerializedName("id") val id: Int,
        @SerializedName("payable_type") val payable_type: String,
        @SerializedName("payable_id") val payable_id: Int,
        @SerializedName("payment_type") val payment_type: String,
        @SerializedName("amount") val amount: Double,
        @SerializedName("tips") val tips: Double,
        @SerializedName("offline_id") val offline_id: String,
        @SerializedName("order_id") val order_id: Int,
        @SerializedName("custom_order_id") val custom_order_id: Int,
        @SerializedName("tips_adjusted") val tips_adjusted: Boolean,
        @SerializedName("total_discount") val total_discount: Double,
        @SerializedName("sub_total") val sub_total: Double,
        @SerializedName("transaction_id") val transaction_id: String,
        @SerializedName("card_type") val card_type: String? = null,
        @SerializedName("card_name") val card_name: String,
        @SerializedName("magensa_response") val magensa_response_data: String,
        @SerializedName("card_number") val card_number: String,
        @SerializedName("cash_discount_type") val cash_discount_type: String,
        @SerializedName("employee_id") val employee_id: Int,
        @SerializedName("cash_discount_or_surcharge") val cash_discount_or_surcharge: Double,
        @SerializedName("tax_amount") val tax_amount: Double,
        @SerializedName("service_charge_amount") val service_charge_amount: Double,
        @SerializedName("service_charge_details") val service_charge_details: ArrayList<OrderServiceChargeApplied>,
        @SerializedName("terminal_id") val terminal_id: Int,
        @SerializedName("used_reward_points") val used_reward_points: Int?,
        @SerializedName("loyalty_program_id") val loyalty_program_id: Int?,
        @SerializedName("loyalty_amount") val loyalty_amount: Double?,
        @SerializedName("is_loyalty_applied") val is_loyalty_applied: Boolean?,
        @SerializedName("order") val order: Order,
        @SerializedName("guest_count") val guestCount: Int?,
        @SerializedName("global_uniq_id")
        var global_uniq_id: String = "",
        @SerializedName("ext_data")
        var ext_data: String = "",
        @SerializedName("ecr_ref_num")
        var ecr_ref_num: String = "",
        @SerializedName("pax_transaction_token")
        var pax_transaction_token: String = "",
        @SerializedName("ref_num") var ref_num: String = ""
    ) : Parcelable {
        fun showFormattedValue(value: Double) = "$" + String.format(
            "%.2f",
            value
        )
        @Parcelize
        data class OrderServiceChargeApplied(
            @SerializedName("updated_at")
            var updated_at: String? = null,
            @SerializedName("created_at")
            var created_at: String? = null,
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
        data class Order(
            @SerializedName("id") val id: Int,
            @SerializedName("creation_time_on_terminal") val creation_time_on_terminal: String,
            @SerializedName("offline_id") val offline_id: String,
            @SerializedName("employee_id") val employee_id: Int,
            @SerializedName("terminal_id") val terminal_id: Int,
            @SerializedName("location_id") val location_id: Int,
            @SerializedName("date") val date: String,
            @SerializedName("index_of_date") val index_of_date: String,
            @SerializedName("note") val note: String,
            @SerializedName("order_type_id") val order_type_id: Int,
            @SerializedName("customer_id") val customer_id: String,
            @SerializedName("open_order_type_id") val open_order_type_id: String,
            @SerializedName("integer") val integer: String,
            @SerializedName("service_charge_enabled") val service_charge_enabled: Boolean,
            @SerializedName("tax_enabled") val tax_enabled: Boolean,
            @SerializedName("cash_discount_or_surcharge") val cash_discount_or_surcharge: Double,
            @SerializedName("sub_total") val sub_total: Double,
            @SerializedName("total_discount") val total_discount: Double,
            @SerializedName("discount_type_id") val discount_type_id: String,
            @SerializedName("payment_status") val payment_status: String,
            @SerializedName("total_tips") val total_tips: Double,
            @SerializedName("total_tax_amount") val total_tax_amount: Double,
            @SerializedName("future_delivery_date") val future_delivery_date: String,
            @SerializedName("is_edited") val is_edited: Boolean,
            @SerializedName("edited_order_timestamp") val edited_order_timestamp: String,
            @SerializedName("edit_order_count") val edit_order_count: String,
            @SerializedName("future_delivery_time") val future_delivery_time: String,
            @SerializedName("total_service_charges") val total_service_charges: Double,
            @SerializedName("open_order_type") val open_order_type: String,
            @SerializedName("delivery_type") val delivery_type: String,
            @SerializedName("order_split_type") val order_split_type: String,
            @SerializedName("delivery_employee_id") val delivery_employee_id: String,
            @SerializedName("discount_id") val discount_id: String,
            @SerializedName("created_at") val created_at: String,
            @SerializedName("updated_at") val updated_at: String,
            @SerializedName("order_items") val order_items: List<GetOrderDetailsResponse.Data.OrderItem>,
            @SerializedName("employee") val employee: String,
            @SerializedName("customer") val customer: Customer,
            @SerializedName("order_type") val order_type: String,
            @SerializedName("venue_website") val venue_website: String,
            @SerializedName("refund_detail") val refund_detail: Refund_detail,
            @SerializedName("digital_receipt_url") val digital_receipt_url: String,
            @SerializedName("total_amount") val total_amount: Double,
            @SerializedName("order_type_name") val order_type_name: String,
        ) : Parcelable {
            @Parcelize
            data class Order_items(
                @SerializedName("id") val id: Int,
                @SerializedName("order_id") val order_id: Int,
                @SerializedName("item_id") val item_id: Int,
                @SerializedName("category_id") val category_id: Int,
                @SerializedName("employee_id") val employee_id: Int,
                @SerializedName("discount_id") val discount_id: String,
                @SerializedName("item_name") val item_name: String,
                @SerializedName("price") val price: Double,
                @SerializedName("quantity") val quantity: Int,
                @SerializedName("discount_amount") val discount_amount: Double,
                @SerializedName("total_price") val total_price: Double,
                @SerializedName("float") val float: Double,
                @SerializedName("discount_type") val discount_type: String,
                @SerializedName("is_printed") val is_printed: Boolean,
                @SerializedName("is_paid") val is_paid: Boolean,
                @SerializedName("completed_in_kitchen") val completed_in_kitchen: Boolean,
                @SerializedName("note") val note: String,
                @SerializedName("timestamp") val timestamp: String,
                @SerializedName("refunded_quantity") val refunded_quantity: String,
                @SerializedName("refunded_amount") val refunded_amount: Double,
                @SerializedName("order_item_modifiers") val order_item_modifiers: List<OrderItemModifiers>,
                @SerializedName("order_item_taxes") val order_item_taxes: List<Order_item_taxes>,
                @SerializedName("order_item_variation") val order_item_variation: OrderItemVariationAttribute,
                var isChecked: Boolean = false
            ) : Parcelable {
                @Parcelize
                data class Order_item_taxes(
                    @SerializedName("id") val id: Int,
                    @SerializedName("tax_id") val tax_id: Int,
                    @SerializedName("name") val name: String,
                    @SerializedName("amount") val amount: String,
                    @SerializedName("rate") val rate: Double,
                    @SerializedName("is_default") val is_default: Boolean,
                    @SerializedName("is_tax_removed") val is_tax_removed: Boolean,
                    @SerializedName("created_at") val created_at: String,
                    @SerializedName("updated_at") val updated_at: String,
                    @SerializedName("order_id") val order_id: Int,
                    @SerializedName("order_item_modifier_id") val order_item_modifier_id: String,
                    @SerializedName("order_item_id") val order_item_id: Int,
                    @SerializedName("tax_total_amount") val tax_total_amount: Double,
                    @SerializedName("tax_type") val tax_type: String
                ) : Parcelable
            }

            @Parcelize
            class OrderItemVariationAttribute : Parcelable {
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
                @SerializedName("total_price") var totalPrice: Int

            ) : Parcelable
        }

        @Parcelize
        data class Customer(
            @SerializedName("id") var id: Int,
            @SerializedName("first_name") var firstName: String,
            @SerializedName("last_name") var lastName: String,
            @SerializedName("company") var company: String,
            @SerializedName("location_id") var locationId: Int,
            @SerializedName("created_at") var createdAt: String,
            @SerializedName("updated_at") var updatedAt: String,
            @SerializedName("birth_date") var birthDate: String,
            @SerializedName("email") var email: String,
            @SerializedName("addresses")
            val addresses: List<OpenOrderResponse.Data.Order.Customer.Address>,
            @SerializedName("phones")
            val phones: List<OpenOrderResponse.Data.Order.Customer.Phone>,
        ) : Parcelable {

        }

        @Parcelize
        data class Refund_detail(
            @SerializedName("refunded_amount") val refunded_amount: Double
        ) : Parcelable {
        }
    }


}

