package com.pays.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName


data class KioskOrderResponse(
    @SerializedName("data"    ) var data    : Data?   = Data(),
    @SerializedName("type"    ) var type    : String? = null,
    @SerializedName("status"  ) var status  : Int?    = null,
    @SerializedName("message" ) var message : String? = null

) {

    data class Data (

        @SerializedName("id"                         ) var id                      : Int?                  = null,
        @SerializedName("creation_time_on_terminal"  ) var creationTimeOnTerminal  : String?               = null,
        @SerializedName("offline_id"                 ) var offlineId               : String?               = null,
        @SerializedName("employee_id"                ) var employeeId              : Int?                  = null,
        @SerializedName("terminal_id"                ) var terminalId              : Int?                  = null,
        @SerializedName("location_id"                ) var locationId              : Int?                  = null,
        @SerializedName("date"                       ) var date                    : String?               = null,
        @SerializedName("index_of_date"              ) var indexOfDate             : String?               = null,
        @SerializedName("note"                       ) var note                    : String?               = null,
        @SerializedName("order_type_id"              ) var orderTypeId             : Int?                  = null,
        @SerializedName("customer_id"                ) var customerId              : String?               = null,
        @SerializedName("open_order_type_id"         ) var openOrderTypeId         : String?               = null,
        @SerializedName("integer"                    ) var integer                 : String?               = null,
        @SerializedName("service_charge_enabled"     ) var serviceChargeEnabled    : Boolean?              = null,
        @SerializedName("tax_enabled"                ) var taxEnabled              : Boolean?              = null,
        @SerializedName("cash_discount_or_surcharge" ) var cashDiscountOrSurcharge : Double?               = null,
        @SerializedName("sub_total"                  ) var subTotal                : Int?                  = null,
        @SerializedName("total_discount"             ) var totalDiscount           : Int?                  = null,
        @SerializedName("discount_type_id"           ) var discountTypeId          : String?               = null,
        @SerializedName("payment_status"             ) var paymentStatus           : String?               = null,
        @SerializedName("order_status"               ) var orderStatus             : String?               = null,
        @SerializedName("total_tips"                 ) var totalTips               : Int?                  = null,
        @SerializedName("total_tax_amount"           ) var totalTaxAmount          : Double?               = null,
        @SerializedName("future_delivery_date"       ) var futureDeliveryDate      : String?               = null,
        @SerializedName("is_edited"                  ) var isEdited                : Boolean?              = null,
        @SerializedName("edited_order_timestamp"     ) var editedOrderTimestamp    : String?               = null,
        @SerializedName("edit_order_count"           ) var editOrderCount          : String?               = null,
        @SerializedName("future_delivery_time"       ) var futureDeliveryTime      : String?               = null,
        @SerializedName("total_service_charges"      ) var totalServiceCharges     : Int?                  = null,
        @SerializedName("open_order_type"            ) var openOrderType           : String?               = null,
        @SerializedName("delivery_type"              ) var deliveryType            : String?               = null,
        @SerializedName("delivery_employee_id"       ) var deliveryEmployeeId      : String?               = null,
        @SerializedName("discount_id"                ) var discountId              : String?               = null,
        @SerializedName("created_at"                 ) var createdAt               : String?               = null,
        @SerializedName("updated_at"                 ) var updatedAt               : String?               = null,
        @SerializedName("loyalty_program_id"         ) var loyaltyProgramId        : String?               = null,
        @SerializedName("loyalty_amount"             ) var loyaltyAmount           : Int?                  = null,
        @SerializedName("is_loyalty_applied"         ) var isLoyaltyApplied        : Boolean?              = null,
        @SerializedName("loyalty_balance"            ) var loyaltyBalance          : Int?                  = null,
        @SerializedName("used_reward_points"         ) var usedRewardPoints        : Int?                  = null,
        @SerializedName("cash_discount_type"         ) var cashDiscountType        : String?               = null,
        @SerializedName("pick_up_time"               ) var pickUpTime              : String?               = null,
        @SerializedName("is_accepted"                ) var isAccepted              : Boolean?              = null,
        @SerializedName("preparation_time"           ) var preparationTime         : Int?                  = null,
        @SerializedName("magensa_response"           ) var magensaResponse         : String?               = null,
        @SerializedName("custom_order_id"            ) var customOrderId           : Int?                  = null,
        @SerializedName("order_items"                ) var orderItems              : ArrayList<OrderItems> = arrayListOf(),
        @SerializedName("order_service_charges"      ) var orderServiceCharges     : ArrayList<String>     = arrayListOf(),
        @SerializedName("payments"                   ) var payments                : ArrayList<Payments>   = arrayListOf(),
        @SerializedName("employee"                   ) var employee                : Employee?             = Employee(),
        @SerializedName("customer"                   ) var customer                : Customer               = Customer(),
        @SerializedName("order_type"                 ) var orderType               : String?               = null,
        @SerializedName("venue_details"              ) var venueDetails            : VenueDetails?         = VenueDetails(),
        @SerializedName("refund_detail"              ) var refundDetail            : RefundDetail?         = RefundDetail(),
        @SerializedName("digital_receipt_url"        ) var digitalReceiptUrl       : String?               = null,
        @SerializedName("total_amount"               ) var totalAmount             : Double?               = null,
        @SerializedName("floor_plan_table"           ) var floorPlanTable          : String?               = null,
        @SerializedName("guest_attributes"           ) var guestAttributes         : ArrayList<String>     = arrayListOf(),
        @SerializedName("merged_table_nos"           ) var mergedTableNos          : String?               = null,
        @SerializedName("dine_in_order_detail"       ) var dineInOrderDetail       : String?               = null,
        @SerializedName("merged_order_ids"           ) var mergedOrderIds          : ArrayList<String>     = arrayListOf(),
        @SerializedName("terminal_name"              ) var terminalName            : String?               = null,
        @SerializedName("base_url"                   ) var baseUrl                 : String?               = null,
        @SerializedName("order_type_name"            ) var orderTypeName           : String?               = null

    ){



        data class RefundDetail (

            @SerializedName("refunded_amount" ) var refundedAmount : Int? = null

        )

        data class VenueDetails (

            @SerializedName("venue_name"           ) var venueName         : String? = null,
            @SerializedName("venue_phone_number"   ) var venuePhoneNumber  : String? = null,
            @SerializedName("venue_phone_number_2" ) var venuePhoneNumber2 : String? = null,
            @SerializedName("venue_website"        ) var venueWebsite      : String? = null,
            @SerializedName("venue_address"        ) var venueAddress      : String? = null

        )


        data class Customer(
            @SerializedName("id") var id: Int? = null,
            @SerializedName("first_name") var firstName: String? = null,
            @SerializedName("last_name") var lastName: String? = null,
            @SerializedName("birth_date") var birthDate: String? = null,
            @SerializedName("email") var email: String? = null,
            @SerializedName("company") var company: String? = null,
            @SerializedName("enroll_to_loyalty") var enrollToLoyalty: Boolean? = null,
            @SerializedName("final_reward") var finalReward: Int? = null,
            @SerializedName("same_as_billing_address") var sameAsBillingAddress: Boolean? = null,
            @SerializedName("phones") var phones: ArrayList<Phones> = arrayListOf(),
            @SerializedName("addresses") var addresses: ArrayList<Address> = arrayListOf()

        ) {
            data class Phones(
                @SerializedName("id") var id: Int? = null,
                @SerializedName("phone_number") var phoneNumber: String? = null
            )

            data class Address(
                @SerializedName("address1") var address1: String? = null,
                @SerializedName("address2") var address2: String? = null,
                @SerializedName("city") var city: String? = null,
                @SerializedName("country") var country: String? = null,
                @SerializedName("full_address") var fullAddress: String? = null,
                @SerializedName("id") var id: Int? = null,
                @SerializedName("latitude") var latitude: String? = null,
                @SerializedName("longitude") var longitude: String? = null,
                @SerializedName("postcode") var postcode: String? = null,
                @SerializedName("state") var state: String? = null,
                @SerializedName("street") var street: String? = null,
                @SerializedName("type_of_address") var typeOfAddress: String? = null
            )
        }

        data class Employee (

            @SerializedName("id"                   ) var id                 : Int?     = null,
            @SerializedName("name"                 ) var name               : String?  = null,
            @SerializedName("email"                ) var email              : String?  = null,
            @SerializedName("phone_number"         ) var phoneNumber        : String?  = null,
            @SerializedName("location_id"          ) var locationId         : Int?     = null,
            @SerializedName("passcode"             ) var passcode           : String?  = null,
            @SerializedName("is_active"            ) var isActive           : Boolean? = null,
            @SerializedName("created_at"           ) var createdAt          : String?  = null,
            @SerializedName("updated_at"           ) var updatedAt          : String?  = null,
            @SerializedName("loggedin_terminal_id" ) var loggedinTerminalId : Int?     = null,
            @SerializedName("is_clocked_in"        ) var isClockedIn        : Boolean? = null,
            @SerializedName("first_name"           ) var firstName          : String?  = null,
            @SerializedName("last_name"            ) var lastName           : String?  = null,
            @SerializedName("team_role_id"         ) var teamRoleId         : Int?     = null,
            @SerializedName("hourly_wages"         ) var hourlyWages        : Int?     = null,
            @SerializedName("phone_country"        ) var phoneCountry       : String?  = null,
            @SerializedName("deleted_at"           ) var deletedAt          : String?  = null

        )

        data class Payments (

            @SerializedName("id"                            ) var id                         : Int?     = null,
            @SerializedName("payable_type"                  ) var payableType                : String?  = null,
            @SerializedName("payable_id"                    ) var payableId                  : Int?     = null,
            @SerializedName("amount"                        ) var amount                     : Double?  = null,
            @SerializedName("tips"                          ) var tips                       : Int?     = null,
            @SerializedName("offline_id"                    ) var offlineId                  : String?  = null,
            @SerializedName("order_id"                      ) var orderId                    : Int?     = null,
            @SerializedName("transaction_id"                ) var transactionId              : String?  = null,
            @SerializedName("card_type"                     ) var cardType                   : String?  = null,
            @SerializedName("card_name"                     ) var cardName                   : String?  = null,
            @SerializedName("card_number"                   ) var cardNumber                 : String?  = null,
            @SerializedName("employee_id"                   ) var employeeId                 : Int?     = null,
            @SerializedName("cash_discount_or_surcharge"    ) var cashDiscountOrSurcharge    : Double?  = null,
            @SerializedName("tax_amount"                    ) var taxAmount                  : Double?  = null,
            @SerializedName("service_charge_amount"         ) var serviceChargeAmount        : Int?     = null,
            @SerializedName("terminal_id"                   ) var terminalId                 : Int?     = null,
            @SerializedName("tips_adjusted"                 ) var tipsAdjusted               : Boolean? = null,
            @SerializedName("total_discount"                ) var totalDiscount              : Int?     = null,
            @SerializedName("sub_total"                     ) var subTotal                   : Int?     = null,
            @SerializedName("created_at"                    ) var createdAt                  : String?  = null,
            @SerializedName("updated_at"                    ) var updatedAt                  : String?  = null,
            @SerializedName("payment_type"                  ) var paymentType                : String?  = null,
            @SerializedName("dynamic_payment_id"            ) var dynamicPaymentId           : String?  = null,
            @SerializedName("tip_setting_id"                ) var tipSettingId               : String?  = null,
            @SerializedName("loyalty_program_id"            ) var loyaltyProgramId           : String?  = null,
            @SerializedName("loyalty_amount"                ) var loyaltyAmount              : Int?     = null,
            @SerializedName("is_loyalty_applied"            ) var isLoyaltyApplied           : Boolean? = null,
            @SerializedName("used_reward_points"            ) var usedRewardPoints           : Int?     = null,
            @SerializedName("cash_discount_type"            ) var cashDiscountType           : String?  = null,
            @SerializedName("deleted_at"                    ) var deletedAt                  : String?  = null,
            @SerializedName("magensa_response"              ) var magensaResponse            : String?  = null,
            @SerializedName("is_captured"                   ) var isCaptured                 : Boolean? = null,
            @SerializedName("gift_card_redeemed_amount"     ) var giftCardRedeemedAmount     : Int?     = null,
            @SerializedName("signature"                     ) var signature                  : String?  = null,
            @SerializedName("loyalty_balance"               ) var loyaltyBalance             : Int?     = null,
            @SerializedName("captured_at"                   ) var capturedAt                 : String?  = null,
            @SerializedName("gift_card_id"                  ) var giftCardId                 : String?  = null,
            @SerializedName("capture_response"              ) var captureResponse            : String?  = null,
            @SerializedName("tip_with_surcharge_percentage" ) var tipWithSurchargePercentage : Int?     = null,
            @SerializedName("global_uniq_id"                ) var globalUniqId               : String?  = null,
            @SerializedName("ref_num"                       ) var refNum                     : String?  = null,
            @SerializedName("ext_data"                      ) var extData                    : String?  = null,
            @SerializedName("ecr_ref_num"                   ) var ecrRefNum                  : String?  = null,
            @SerializedName("pax_transaction_token"         ) var paxTransactionToken        : String?  = null,
            @SerializedName("pax_data"                      ) var paxData                    : String?  = null,
            @SerializedName("guest_index_for_dine_in"       ) var guestIndexForDineIn        : String?  = null

        )

        data class OrderItems (

            @SerializedName("id"                      ) var id                  : Int?                      = null,
            @SerializedName("order_id"                ) var orderId             : Int?                      = null,
            @SerializedName("item_id"                 ) var itemId              : Int?                      = null,
            @SerializedName("category_id"             ) var categoryId          : Int?                      = null,
            @SerializedName("employee_id"             ) var employeeId          : String?                   = null,
            @SerializedName("discount_id"             ) var discountId          : String?                   = null,
            @SerializedName("item_name"               ) var itemName            : String?                   = null,
            @SerializedName("price"                   ) var price               : Double?                      = null,
            @SerializedName("quantity"                ) var quantity            : Int?                      = null,
            @SerializedName("discount_amount"         ) var discountAmount      : Int?                      = null,
            @SerializedName("total_price"             ) var totalPrice          : Double?                      = null,
            @SerializedName("float"                   ) var float               : Int?                      = null,
            @SerializedName("discount_type"           ) var discountType        : String?                   = null,
            @SerializedName("is_printed"              ) var isPrinted           : Boolean?                  = null,
            @SerializedName("is_paid"                 ) var isPaid              : Boolean?                  = null,
            @SerializedName("completed_in_kitchen"    ) var completedInKitchen  : Boolean?                  = null,
            @SerializedName("timestamp"               ) var timestamp           : String?                   = null,
            @SerializedName("refunded_quantity"       ) var refundedQuantity    : String?                   = null,
            @SerializedName("refunded_amount"         ) var refundedAmount      : Int?                      = null,
            @SerializedName("is_fired"                ) var isFired             : Boolean?                  = null,
            @SerializedName("is_edited"               ) var isEdited            : Boolean?                  = null,
            @SerializedName("custom_item_id"          ) var customItemId        : String?                   = null,
            @SerializedName("sort"                    ) var sort                : String?                   = null,
            @SerializedName("guest_index_for_dine_in" ) var guestIndexForDineIn : String?                   = null,
            @SerializedName("is_item_edited"          ) var isItemEdited        : Boolean?                  = null,
            @SerializedName("note"                    ) var note                : String?                   = null,
            @SerializedName("order_item_modifiers"    ) var orderItemModifiers  : ArrayList<OrderItemModifiers>         = arrayListOf(),
            @SerializedName("order_item_taxes"        ) var orderItemTaxes      : ArrayList<OrderItemTaxes> = arrayListOf(),
            @SerializedName("order_item_variation"    ) var orderItemVariation  : String?                   = null

        ){

            data class OrderItemModifiers(
                @SerializedName("id") var id: Int? = null,
                @SerializedName("order_item_id") var orderItemId: Int? = null,
                @SerializedName("name") var name: String? = null,
                @SerializedName("price") var price: Double? = null,
                @SerializedName("quantity") var quantity: Int? = null,
                @SerializedName("order_id") var orderId: Int? = null,
                @SerializedName("is_modifier") var isModifier: Boolean? = null,
                @SerializedName("item_id") var itemId: String? = null,
                @SerializedName("category_id") var categoryId: String? = null,
                @SerializedName("timestamp") var timestamp: String? = null,
                @SerializedName("modifier_set_id") var modifierSetId: Int? = null,
                @SerializedName("modifier_id") var modifierId: Int? = null,
                @SerializedName("modifier_quantity") var modifierQuantity: Int? = null,
                @SerializedName("order_item_taxes") var orderItemTaxes: ArrayList<String> = arrayListOf()
            )

            data class OrderItemTaxes(
                @SerializedName("id") var id: Int? = null,
                @SerializedName("tax_id") var taxId: Int? = null,
                @SerializedName("name") var name: String? = null,
                @SerializedName("amount") var amount: String? = null,
                @SerializedName("rate") var rate: Double? = null,
                @SerializedName("is_default") var isDefault: Boolean? = null,
                @SerializedName("is_tax_removed") var isTaxRemoved: String? = null,
                @SerializedName("created_at") var createdAt: String? = null,
                @SerializedName("updated_at") var updatedAt: String? = null,
                @SerializedName("order_id") var orderId: Int? = null,
                @SerializedName("order_item_modifier_id") var orderItemModifierId: String? = null,
                @SerializedName("order_item_id") var orderItemId: Int? = null,
                @SerializedName("tax_total_amount") var taxTotalAmount: Double? = null,
                @SerializedName("tax_type") var taxType: String? = null,
                @SerializedName("deleted_at") var deletedAt: String? = null
            )
        }
    }
}
