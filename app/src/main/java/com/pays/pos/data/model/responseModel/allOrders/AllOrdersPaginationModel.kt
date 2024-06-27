package com.pays.pos.data.model.responseModel.allOrders

import com.google.gson.annotations.SerializedName
import com.pays.pos.data.model.requestModel.OrderItemVariationAttribute
import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel

data class AllOrdersPaginationModel(
    @SerializedName("data") var data: Data? = Data(),
    @SerializedName("type") var type: String? = null,
    @SerializedName("status") var status: Int? = null,
    @SerializedName("message") var message: String? = null
) {

    data class Data(
        @SerializedName("orders") var orders: List<Orders> = arrayListOf(),
        @SerializedName("pagination") var pagination: Pagination? = Pagination()
    )

    data class OrderItems(
        @SerializedName("id") var id: Int,
        @SerializedName("order_id") var orderId: Int,
        @SerializedName("item_id") var itemId: Int=0,
        @SerializedName("category_id") var categoryId: Int=0,
        @SerializedName("employee_id") var employeeId: Int,
        @SerializedName("discount_id") var discountId: Int,
        @SerializedName("item_name") var itemName: String,
        @SerializedName("price") var price: Double,
        @SerializedName("quantity") var quantity: Int,
        @SerializedName("discount_amount") var discountAmount: Double,
        @SerializedName("total_price") var totalPrice: Double,
        @SerializedName("float") var float: Int,
        @SerializedName("discount_type") var discountType: String,
        @SerializedName("is_printed") var isPrinted: Boolean,
        @SerializedName("is_paid") var isPaid: Boolean,
        @SerializedName("completed_in_kitchen") var completedInKitchen: Boolean,
        @SerializedName("timestamp") var timestamp: String="",
        @SerializedName("refunded_quantity") var refundedQuantity: String,
        @SerializedName("refunded_amount") var refundedAmount: Double,
        @SerializedName("is_fired") var isFired: Boolean,
        @SerializedName("is_edited") var isEdited: Boolean,
        @SerializedName("custom_item_id") var customItemId: Int,
        @SerializedName("sort") var sort: String,
        @SerializedName("guest_index_for_dine_in") var guestIndexForDineIn: Int,
        @SerializedName("is_item_edited") var isItemEdited: Boolean,
        @SerializedName("note") var note: String,
        @SerializedName("order_item_modifiers") var orderItemModifiers: ArrayList<OrderItemModifiers> = arrayListOf(),
        @SerializedName("order_item_taxes") var orderItemTaxes: ArrayList<OrderItemTaxes> = arrayListOf(),
        @SerializedName("order_item_variation")
        val order_item_variation: OrderItemVariationAttribute?,
    )

    data class OrderItemTaxes(

        @SerializedName("id") var id: Int,
        @SerializedName("tax_id") var taxId: Int,
        @SerializedName("name") var name: String,
        @SerializedName("amount") var amount: String?="",
        @SerializedName("rate") var rate: Double,
        @SerializedName("is_default") var isDefault: Boolean,
        @SerializedName("is_tax_removed") var isTaxRemoved: Boolean,
        @SerializedName("created_at") var createdAt: String,
        @SerializedName("updated_at") var updatedAt: String,
        @SerializedName("order_id") var orderId: Int,
        @SerializedName("order_item_modifier_id") var orderItemModifierId: String =" ",
        @SerializedName("order_item_id") var orderItemId: Int,
        @SerializedName("tax_total_amount") var taxTotalAmount: Double,
        @SerializedName("tax_type") var taxType: String,
        @SerializedName("deleted_at") var deletedAt: String

    )

    data class Orders(
        @SerializedName("id") var id: Int,
        @SerializedName("creation_time_on_terminal") var creationTimeOnTerminal: String =" ",
        @SerializedName("offline_id") var offlineId: String,
        @SerializedName("employee_id") var employeeId: Int,
        @SerializedName("terminal_id") var terminalId: Int,
        @SerializedName("location_id") var locationId: Int,
        @SerializedName("date") var date: String,
        @SerializedName("index_of_date") var indexOfDate: String=" ",
        @SerializedName("note") var note: String,
        @SerializedName("order_type_id") var orderTypeId: Int,
        @SerializedName("customer_id") var customerId: String =" ",
        @SerializedName("open_order_type_id") var openOrderTypeId: String,
        @SerializedName("integer") var integer: String=" ",
        @SerializedName("service_charge_enabled") var serviceChargeEnabled: Boolean,
        @SerializedName("tax_enabled") var taxEnabled: Boolean,
        @SerializedName("cash_discount_or_surcharge") var cashDiscountOrSurcharge: Double,
        @SerializedName("sub_total") var subTotal: Double,
        @SerializedName("total_discount") var totalDiscount: Double,
        @SerializedName("discount_type_id") var discountTypeId: String =" ",
        @SerializedName("payment_status") var paymentStatus: String =" ",
        @SerializedName("order_status") var orderStatus: String =" ",
        @SerializedName("total_tips") var totalTips: Double,
        @SerializedName("total_tax_amount") var totalTaxAmount: Double,
        @SerializedName("future_delivery_date") var futureDeliveryDate: String =" ",
        @SerializedName("is_edited") var isEdited: Boolean,
        @SerializedName("edited_order_timestamp") var editedOrderTimestamp: String =" ",
        @SerializedName("edit_order_count") var editOrderCount: String =" ",
        @SerializedName("future_delivery_time") var futureDeliveryTime: String =" ",
        @SerializedName("total_service_charges") var totalServiceCharges: Double,
        @SerializedName("open_order_type") var openOrderType: String =" ",
        @SerializedName("delivery_type") var deliveryType: String =" ",
        @SerializedName("delivery_employee_id") var deliveryEmployeeId: Int=0,
        @SerializedName("discount_id") var discountId: Int,
        @SerializedName("created_at") var createdAt: String =" ",
        @SerializedName("updated_at") var updatedAt: String =" ",
        @SerializedName("loyalty_program_id") var loyaltyProgramId: Int,
        @SerializedName("loyalty_amount") var loyaltyAmount: Double,
        @SerializedName("is_loyalty_applied") var isLoyaltyApplied: Boolean,
        @SerializedName("loyalty_balance") var loyaltyBalance: Int,
        @SerializedName("used_reward_points") var usedRewardPoints: Int,
        @SerializedName("cash_discount_type") var cashDiscountType: String =" ",
        @SerializedName("pick_up_time") var pickUpTime: String,
        @SerializedName("is_accepted") var isAccepted: Boolean,
        @SerializedName("preparation_time") var preparationTime: Int,
        @SerializedName("magensa_response") var magensaResponse: String =" ",
        @SerializedName("custom_order_id") var customOrderId: Int,
        @SerializedName("order_items") var orderItems: List<OrderItems> = arrayListOf(),
        @SerializedName("order_service_charges") var orderServiceCharges: ArrayList<OrderServiceCharges> = arrayListOf(),
        @SerializedName("payments") var payments:  List<OnlineOrderResponseModel.Data.Payment>,
        @SerializedName("employee") var employee: OnlineOrderResponseModel.Data.Employee,
        @SerializedName("customer") var customer: OnlineOrderResponseModel.Data.Customer,
        @SerializedName("order_type") var orderType: String =" ",
        @SerializedName("venue_details") var venueDetails: VenueDetails? = VenueDetails(),
        @SerializedName("refund_detail") var refundDetail: RefundDetail? = RefundDetail(),
        @SerializedName("digital_receipt_url") var digitalReceiptUrl: String =" ",
        @SerializedName("total_amount") var totalAmount: Double,
        @SerializedName("floor_plan_table") var floorPlanTable: String =" ",
        @SerializedName("guest_attributes") var guestAttributes: ArrayList<String> = arrayListOf(),
        @SerializedName("merged_table_nos") var mergedTableNos: String =" ",
        @SerializedName("dine_in_order_detail") var dineInOrderDetail: String =" ",
        @SerializedName("merged_order_ids") var mergedOrderIds: ArrayList<String> = arrayListOf(),
        @SerializedName("terminal_name") var terminalName: String =" ",
        @SerializedName("base_url") var baseUrl: String =" ",
        @SerializedName("order_type_name") var orderTypeName: String =" "

    )

    data class OrderItemModifiers(

        @SerializedName("id") var id: Int,
        @SerializedName("order_item_id") var orderItemId: Int,
        @SerializedName("name") var name: String,
        @SerializedName("price") var price: Double,
        @SerializedName("quantity") var quantity: Int,
        @SerializedName("order_id") var orderId: Int,
        @SerializedName("is_modifier") var isModifier: Boolean,
        @SerializedName("item_id") var itemId: Int=0,
        @SerializedName("category_id") var categoryId: String=" ",
        @SerializedName("timestamp") var timestamp: String="",
        @SerializedName("modifier_set_id") var modifierSetId: Int,
        @SerializedName("modifier_id") var modifierId: Int,
        @SerializedName("modifier_quantity") var modifierQuantity: Int,
        @SerializedName("order_item_taxes") var orderItemTaxes: ArrayList<String> = arrayListOf()

    )

    data class OrderServiceCharges(

        @SerializedName("id") var id: Int? = null,
        @SerializedName("created_at")
        val createdAt: String?="",
        @SerializedName("order_id") var orderId: Int? = null,
        @SerializedName("service_charge_id") var serviceChargeId: Int? = null,
        @SerializedName("updated_at")
        val updatedAt: String?="",
        @SerializedName("name") var name: String? = null,
        @SerializedName("amount") var amount: Double? = null,
        @SerializedName("rate") var rate: Double? = null,
        @SerializedName("order_type") var orderType: String? = null,
        @SerializedName("min_guest_count") var minGuestCount: Int? = null,
        @SerializedName("max_guest_count") var maxGuestCount: Int? = null

    )

    data class Employee(

        @SerializedName("id") var id: Int? = null,
        @SerializedName("name") var name: String? = null,
        @SerializedName("email") var email: String? = null,
        @SerializedName("phone_number") var phoneNumber: String? = null,
        @SerializedName("location_id") var locationId: Int? = null,
        @SerializedName("passcode") var passcode: String? = null,
        @SerializedName("is_active") var isActive: Boolean? = null,
        @SerializedName("created_at") var createdAt: String? = null,
        @SerializedName("updated_at") var updatedAt: String? = null,
        @SerializedName("loggedin_terminal_id") var loggedinTerminalId: Int? = null,
        @SerializedName("is_clocked_in") var isClockedIn: Boolean? = null,
        @SerializedName("first_name") var firstName: String? = null,
        @SerializedName("last_name") var lastName: String? = null,
        @SerializedName("team_role_id") var teamRoleId: Int? = null,
        @SerializedName("hourly_wages") var hourlyWages: Double? = null,
        @SerializedName("phone_country") var phoneCountry: String? = null,
        @SerializedName("deleted_at") var deletedAt: String? = null

    )

    data class RefundDetail(
        @SerializedName("refunded_amount") var refundedAmount: Double? = null
    )

    data class VenueDetails(
        @SerializedName("venue_name") var venueName: String? = null,
        @SerializedName("venue_phone_number") var venuePhoneNumber: String? = null,
        @SerializedName("venue_phone_number_2") var venuePhoneNumber2: String? = null,
        @SerializedName("venue_website") var venueWebsite: String? = null,
        @SerializedName("venue_address") var venueAddress: String? = null
    )

    data class Pagination(
        @SerializedName("max_page_size") var maxPageSize: Int? = null,
        @SerializedName("per_page") var perPage: Int? = null

    )
}
