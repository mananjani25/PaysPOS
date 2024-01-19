package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class OnlineOrderUpdateRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("creation_time_on_terminal") val creationTimeOnTerminal: String,
    @SerializedName("offline_id") val offlineId: String,
    @SerializedName("employee_id") val employeeId: Int,
    @SerializedName("terminal_id") val terminalId: Int,
    @SerializedName("location_id") val locationId: Int,
    @SerializedName("date") val date: String,
    @SerializedName("future_delivery_date") val futureDeliveryDate: String,
    @SerializedName("future_delivery_time") val futureDeliveryTime: String,
    @SerializedName("index_of_date") val indexOfDate: String,
    @SerializedName("note") val note: String,
    @SerializedName("order_type_id") val orderTypeId: Int,
    @SerializedName("service_charge_enabled") val serviceChargeEnabled: Boolean,
    @SerializedName("total_discount") val totalDiscount: Int,
    @SerializedName("total_amount") val totalAmount: Int,
    @SerializedName("sub_total") val subTotal: Int,
    @SerializedName("total_cash_discount_fee") val totalCashDiscountFee: Int,
    @SerializedName("cash_discount_fee_id") val cashDiscountFeeId: Int,
    @SerializedName("tax_enabled") val taxEnabled: Boolean,
    @SerializedName("total_service_charges") val totalServiceCharges: Int,
    @SerializedName("total_tips") val totalTips: Int,
    @SerializedName("total_tax_amount") val totalTaxAmount: Int,
    @SerializedName("total_cash_discount") val totalCashDiscount: Int,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("open_order_type") val openOrderType: String,
    @SerializedName("delivery_type") val deliveryType: String,
    @SerializedName("delivery_employee_id") val deliveryEmployeeId: Int,
    @SerializedName("discount_id") val discountId: Int,
    @SerializedName("customer_address_id") val customerAddressId: Int,
    @SerializedName("loyalty_program_id") val loyaltyProgramId: Int,
    @SerializedName("loyalty_amount") val loyaltyAmount: Int,
    @SerializedName("is_loyalty_applied") val isLoyaltyApplied: Boolean,
    @SerializedName("used_reward_points") val usedRewardPoints: Int,
    @SerializedName("merged_order_ids") val mergedOrderIds: List<Int>,
    @SerializedName("order_status") val orderStatus: String,
    @SerializedName("preparation_time") val preparationTime: Int,
    @SerializedName("pick_up_time") val pickUpTime: String,
    @SerializedName("is_accepted") val isAccepted: Boolean,
    @SerializedName("customer_attributes") val customerAttributes: CustomerAttributes,
    @SerializedName("payment_attributes") val paymentAttributes: PaymentAttributes,
    @SerializedName("order_service_charges_attributes") val orderServiceChargesAttributes: List<OrderServiceChargesAttribute>,
    @SerializedName("order_items_attributes") val orderItemsAttributes: List<OrderItemsAttribute>,
    @SerializedName("guests_attributes") val guestsAttributes: List<GuestsAttribute>,
    @SerializedName("dine_in_order_detail_attributes") val dineInOrderDetailAttributes: DineInOrderDetailAttributes
) {
    data class CustomerAttributes(
        @SerializedName("id") val id: Int,
        @SerializedName("first_name") val firstName: String,
        @SerializedName("last_name") val lastName: String,
        @SerializedName("company_name") val companyName: String,
        @SerializedName("note") val note: String,
        @SerializedName("birth_date") val birthDate: String,
        @SerializedName("location_id") val locationId: Int,
        @SerializedName("emails_attributes") val emailsAttributes: List<EmailsAttribute>,
        @SerializedName("phones_attributes") val phonesAttributes: List<PhonesAttribute>,
        @SerializedName("addresses_attributes") val addressesAttributes: List<AddressesAttribute>
    ) {
        data class EmailsAttribute(
            @SerializedName("id") val id: Int,
            @SerializedName("email_address") val emailAddress: String,
            @SerializedName("customer_id") val customerId: Int,
            @SerializedName("_destroy") val destroy: String
        )

        data class PhonesAttribute(
            @SerializedName("id") val id: Int,
            @SerializedName("phone_number") val phoneNumber: String,
            @SerializedName("customer_id") val customerId: Int,
            @SerializedName("_destroy") val destroy: String
        )

        data class AddressesAttribute(
            @SerializedName("id") val id: Int,
            @SerializedName("address_1") val address1: String,
            @SerializedName("address_2") val address2: String,
            @SerializedName("address_3") val address3: String,
            @SerializedName("country") val country: String,
            @SerializedName("state") val state: String,
            @SerializedName("city") val city: String,
            @SerializedName("postcode") val postcode: String,
            @SerializedName("latitude") val latitude: Int,
            @SerializedName("longitude") val longitude: Int,
            @SerializedName("type_of_address") val typeOfAddress: String,
            @SerializedName("addressable_type") val addressableType: String,
            @SerializedName("addressable_id") val addressableId: Int,
            @SerializedName("_destroy") val destroy: String
        )
    }

    data class PaymentAttributes(
        @SerializedName("id") val id: Int,
        @SerializedName("payable_type") val payableType: String,
        @SerializedName("payable_id") val payableId: Int,
        @SerializedName("payment_type") val paymentType: String,
        @SerializedName("amount") val amount: Int,
        @SerializedName("tips") val tips: Int,
        @SerializedName("offline_id") val offlineId: String,
        @SerializedName("order_id") val orderId: Int,
        @SerializedName("transaction_id") val transactionId: String,
        @SerializedName("card_type") val cardType: Int,
        @SerializedName("card_number") val cardNumber: String,
        @SerializedName("card_name") val cardName: String,
        @SerializedName("employee_id") val employeeId: Int,
        @SerializedName("cash_discount") val cashDiscount: Int,
        @SerializedName("tax_amount") val taxAmount: Int,
        @SerializedName("terminal_id") val terminalId: Int,
        @SerializedName("service_charge_amount") val serviceChargeAmount: Int,
        @SerializedName("tips_adjusted") val tipsAdjusted: Boolean,
        @SerializedName("total_discount") val totalDiscount: Int,
        @SerializedName("sub_total") val subTotal: Int,
        @SerializedName("loyalty_program_id") val loyaltyProgramId: Int,
        @SerializedName("loyalty_amount") val loyaltyAmount: Int,
        @SerializedName("is_loyalty_applied") val isLoyaltyApplied: Boolean,
        @SerializedName("used_reward_points") val usedRewardPoints: Int,
        @SerializedName("applied_cash_discount") val appliedCashDiscount: Int
    )

    data class OrderServiceChargesAttribute(
        @SerializedName("id") val id: Int,
        @SerializedName("order_id") val orderId: Int,
        @SerializedName("service_charge_id") val serviceChargeId: Int,
        @SerializedName("name") val name: String,
        @SerializedName("amount") val amount: Int,
        @SerializedName("rate") val rate: Int
    )

    data class OrderItemsAttribute(
        @SerializedName("id") val id: Int,
        @SerializedName("terminal_id") val terminalId: Int,
        @SerializedName("order_id") val orderId: Int,
        @SerializedName("item_id") val itemId: Int,
        @SerializedName("category_id") val categoryId: Int,
        @SerializedName("employee_id") val employeeId: Int,
        @SerializedName("item_name") val itemName: String,
        @SerializedName("price") val price: Int,
        @SerializedName("total_price") val totalPrice: Int,
        @SerializedName("quantity") val quantity: Int,
        @SerializedName("discount_amount") val discountAmount: Int,
        @SerializedName("discount_type") val discountType: String,
        @SerializedName("is_printed") val isPrinted: Boolean,
        @SerializedName("is_paid") val isPaid: Boolean,
        @SerializedName("is_fired") val isFired: Boolean,
        @SerializedName("note") val note: String,
        @SerializedName("is_tax_removed") val isTaxRemoved: Boolean,
        @SerializedName("timestamp") val timestamp: String,
        @SerializedName("is_edited") val isEdited: Boolean,
        @SerializedName("edit_timestamp") val editTimestamp: String,
        @SerializedName("order_item_taxes_attributes") val orderItemTaxesAttributes: List<OrderItemTaxesAttribute>,
        @SerializedName("order_item_modifiers_attributes") val orderItemModifiersAttributes: List<OrderItemModifiersAttribute>
    ) {
        data class OrderItemTaxesAttribute(
            @SerializedName("id") val id: Int,
            @SerializedName("order_item_id") val orderItemId: Int,
            @SerializedName("order_item_modifier_id") val orderItemModifierId: Int,
            @SerializedName("order_id") val orderId: Int,
            @SerializedName("tax_id") val taxId: Int,
            @SerializedName("amount") val amount: Int,
            @SerializedName("is_default") val isDefault: Boolean,
            @SerializedName("name") val name: String,
            @SerializedName("tax_type") val taxType: String,
            @SerializedName("tax_total_amount") val taxTotalAmount: Int
        )

        data class OrderItemModifiersAttribute(
            @SerializedName("id") val id: Int,
            @SerializedName("order_item_id") val orderItemId: Int,
            @SerializedName("name") val name: String,
            @SerializedName("price") val price: Int,
            @SerializedName("quantity") val quantity: Int,
            @SerializedName("order_id") val orderId: Int,
            @SerializedName("modifier_set_id") val modifierSetId: Int,
            @SerializedName("order_item_taxes_attributes") val orderItemTaxesAttributes: List<OrderItemTaxesAttribute>,
            @SerializedName("order_items_variation_attributes") val orderItemsVariationAttributes: List<OrderItemsVariationAttribute>
        ) {
            data class OrderItemTaxesAttribute(
                @SerializedName("id") val id: Int,
                @SerializedName("order_item_id") val orderItemId: Int,
                @SerializedName("order_item_modifier_id") val orderItemModifierId: Int,
                @SerializedName("order_id") val orderId: Int,
                @SerializedName("tax_id") val taxId: Int,
                @SerializedName("amount") val amount: Int,
                @SerializedName("is_default") val isDefault: Boolean,
                @SerializedName("is_tax_removed") val isTaxRemoved: Boolean,
                @SerializedName("is_modifier") val isModifier: Boolean,
                @SerializedName("category_id") val categoryId: Int,
                @SerializedName("terminal_id") val terminalId: Int,
                @SerializedName("modifier_id") val modifierId: Int,
                @SerializedName("timestamp") val timestamp: String,
                @SerializedName("name") val name: String,
                @SerializedName("tax_total_amount") val taxTotalAmount: Int,
                @SerializedName("tax_type") val taxType: String
            )

            data class OrderItemsVariationAttribute(
                @SerializedName("id") val id: Int,
                @SerializedName("order_item_id") val orderItemId: Int,
                @SerializedName("name") val name: String,
                @SerializedName("unit_price") val unitPrice: Int,
                @SerializedName("quantity") val quantity: Int,
                @SerializedName("order_id") val orderId: Int,
                @SerializedName("total_price") val totalPrice: Int,
                @SerializedName("variation_id") val variationId: Int
            )
        }
    }

    data class GuestsAttribute(
        @SerializedName("id") val id: Int,
        @SerializedName("order_id") val orderId: Int,
        @SerializedName("name") val name: Int,
        @SerializedName("is_paid") val isPaid: Int,
        @SerializedName("total_amount") val totalAmount: Int,
        @SerializedName("cash_discount") val cashDiscount: Int,
        @SerializedName("total_discount") val totalDiscount: Int,
        @SerializedName("total_service_charge") val totalServiceCharge: Int,
        @SerializedName("sub_total") val subTotal: Int,
        @SerializedName("total_tax") val totalTax: Int,
        @SerializedName("total_tips") val totalTips: Int,
        @SerializedName("customer_id") val customerId: Int,
        @SerializedName("_destroy") val destroy: Boolean,
        @SerializedName("guest_items_attributes") val guestItemsAttributes: List<GuestItemsAttribute>,
        @SerializedName("customer_attributes") val customerAttributes: CustomerAttributes
    ) {
        data class GuestItemsAttribute(
            @SerializedName("id") val id: Int,
            @SerializedName("order_id") val orderId: Int,
            @SerializedName("order_item_id") val orderItemId: Int,
            @SerializedName("quantity") val quantity: Int,
            @SerializedName("item_id") val itemId: Int,
            @SerializedName("amount") val amount: Int,
            @SerializedName("is_paid") val isPaid: Int,
            @SerializedName("guest_id") val guestId: Int,
            @SerializedName("_destroy") val destroy: Boolean,
            @SerializedName("percentage") val percentage: Int,
            @SerializedName("timestamp") val timestamp: String
        )

        data class CustomerAttributes(
            @SerializedName("id") val id: Int,
            @SerializedName("first_name") val firstName: String,
            @SerializedName("last_name") val lastName: String,
            @SerializedName("company_name") val companyName: String,
            @SerializedName("note") val note: String,
            @SerializedName("birth_date") val birthDate: String,
            @SerializedName("location_id") val locationId: Int,
            @SerializedName("emails_attributes") val emailsAttributes: List<EmailsAttribute>,
            @SerializedName("phones_attributes") val phonesAttributes: List<PhonesAttribute>,
            @SerializedName("addresses_attributes") val addressesAttributes: List<AddressesAttribute>
        ) {
            data class EmailsAttribute(
                @SerializedName("id") val id: Int,
                @SerializedName("email_address") val emailAddress: String,
                @SerializedName("customer_id") val customerId: Int,
                @SerializedName("_destroy") val destroy: String
            )

            data class PhonesAttribute(
                @SerializedName("id") val id: Int,
                @SerializedName("phone_number") val phoneNumber: String,
                @SerializedName("customer_id") val customerId: Int,
                @SerializedName("_destroy") val destroy: String
            )

            data class AddressesAttribute(
                @SerializedName("id") val id: Int,
                @SerializedName("address_1") val address1: String,
                @SerializedName("address_2") val address2: String,
                @SerializedName("address_3") val address3: String,
                @SerializedName("country") val country: String,
                @SerializedName("state") val state: String,
                @SerializedName("city") val city: String,
                @SerializedName("postcode") val postcode: String,
                @SerializedName("latitude") val latitude: Int,
                @SerializedName("longitude") val longitude: Int,
                @SerializedName("type_of_address") val typeOfAddress: String,
                @SerializedName("addressable_type") val addressableType: String,
                @SerializedName("addressable_id") val addressableId: Int,
                @SerializedName("_destroy") val destroy: String
            )
        }
    }

    data class DineInOrderDetailAttributes(
        @SerializedName("id") val id: Int,
        @SerializedName("total_guest_count") val totalGuestCount: Int,
        @SerializedName("order_id") val orderId: Int,
        @SerializedName("floor_plan_id") val floorPlanId: Int,
        @SerializedName("floor_plan_table_id") val floorPlanTableId: Int,
        @SerializedName("table_type") val tableType: Int,
        @SerializedName("table_number") val tableNumber: Int,
        @SerializedName("chair_count") val chairCount: Int,
        @SerializedName("table_name") val tableName: String,
        @SerializedName("floor_plan_name") val floorPlanName: String
    )
}