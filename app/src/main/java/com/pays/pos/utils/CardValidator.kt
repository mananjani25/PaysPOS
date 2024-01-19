package com.pays.pos.utils

import android.util.Log
import java.lang.Exception
import java.util.*
import java.util.regex.Pattern

/**
 * Class containing verification methods about cards numbers, CVV, expiry dates, and useful constants about card types
 */
object CardValidator {
    /* Regular expression containing the default format for displaying a card's number */
    private const val DEFAULT_CARD_FORMAT = "(\\d{1,4})"

    /* Regular expression used for sanitizing the card's name */
    const val CARD_NAME_REPLACE_PATTERN = "[^A-Z\\s]"

    /*
     * Test if the string is composed exclusively of digits
     * @param entry String to be tested
     * @return result of the test
     */
    private fun isDigit(entry: String): Boolean {
        return Pattern.matches("^\\d+$", entry)
    }

    /*
     * Sanitizes the card's name using the regular expression above
     * @param name String to be cleaned
     * @return cleaned string
     */
    private fun sanitizeName(name: String): String {
        return name.uppercase(Locale.getDefault()).replace(CARD_NAME_REPLACE_PATTERN.toRegex(), "")
    }

    /**
     * Sanitizes any string given as a parameter
     * @param entry String to be cleaned
     * @param isNumber boolean, if set, the method removes all non digit characters, otherwise only the - and spaces
     * @return cleaned string
     */
    fun sanitizeEntry(entry: String, isNumber: Boolean): String {
        return if (isNumber) entry.replace(
            "\\D".toRegex(),
            ""
        ) else entry.replace("\\s+|-".toRegex(), "")
    }

    /**
     * Returns the Cards element corresponding to the given number
     * @param num String containing the card's number
     * @return Cards element corresponding to num or null if it was not recognized
     */
    fun getCardType(num: String): Cards? {
        var num = num
        num = sanitizeEntry(num, true)
        if (Pattern.matches("^(54)", num) && num.length > 16) {
            return Cards.MAESTRO
        }
        val cards = Cards.values()
        for (i in cards.indices) {
            if (Pattern.matches(cards[i].pattern, num)) {
                return cards[i]
            }
        }
        return null
    }

    /*
     * Applies the Luhn Algorithm to the given card number
     * @param num String containing the card's number to be tested
     * @return boolean containing the result of the computation
     */
    private fun validateLuhnNumber(num: String): Boolean {
        var num = num
        if (num == "") return false
        var nCheck = 0
        var nDigit = 0
        var bEven = false
        num = sanitizeEntry(num, true)
        for (i in num.length - 1 downTo 0) {
            nDigit = num[i].toString().toInt()
            if (bEven) {
                if (2.let { nDigit *= it; nDigit } > 9) nDigit -= 9
            }
            nCheck += nDigit
            bEven = !bEven
        }
        return nCheck % 10 == 0
    }

    /**
     * Checks if the card's number is valid by identifying the card's type and checking its conditions
     * @param num String containing the card's code to be verified
     * @return boolean containing the result of the verification
     */
    fun validateCardNumber(num: String): Boolean {
        var num = num
        if (num == "") return false
        num = sanitizeEntry(num, true)
        if (Pattern.matches("^\\d+$", num)) {
            val c = getCardType(num)
            if (c != null) {
                var len = false
                for (i in c.cardLength.indices) {
                    if (c.cardLength[i] == num.length) {
                        len = true
                        break
                    }
                }
                return len && (!c.luhn || validateLuhnNumber(num))
            }
        }
        return false
    }

    /**
     * Checks if the card is still valid
     * @param month String containing the expiring month of the card
     * @param year String containing the expiring year of the card
     * @return boolean containing the result of the verification
     */
    fun validateExpiryDate(month: String, year: String): Boolean {
        val iMonth: Int
        val iYear: Int
        val current_year = Calendar.getInstance().get(Calendar.YEAR) % 100
        val current_month = Calendar.getInstance().get(Calendar.MONTH)+1
        Log.d(TAG, "validateExpiryDate: year  $current_month")
        Log.d(TAG, "validateExpiryDate: month $current_year")
        if (year.isEmpty() || month.isEmpty())
            return false
        return if(current_year==year.toInt()){
            month.toInt() in current_month..12
        }else{
            (month.toInt()<=12) && current_year<=year.toInt()
        }
    }

    /**
     * Checks if the card is still valid
     * @param month int containing the expiring month of the card
     * @param year int containing the expiring year of the card
     * @return boolean containing the result of the verification
     */
     fun validateExpiryDate(month: Int, year: Int): Boolean {
        if (month < 1 || year < 1) return false
        val cal = Calendar.getInstance()
        val curMonth = cal[Calendar.MONTH] + 1
        var curYear = cal[Calendar.YEAR]
        if (year < 100) curYear -= 2000
        return if (curYear == year) curMonth <= month else curYear < year
    }

    /**
     * Checks if the CVV is valid for a given card's type
     * @param cvv String containing the value of the CVV
     * @param card Cards element containing the card's type
     * @return boolean containing the result of the verification
     */
    fun validateCVV(cvv: String, card: Cards?): Boolean {
        if (cvv == "" || card == null) return false
        for (i in card.cvvLength.indices) {
            if (card.cvvLength[i] == cvv.length) return true
        }
        return false
    }

    /**
     * Checks if the CVV is valid for a given card's type
     * @param cvv int containing the value of the CVV
     * @param card Cards element containing the card's type
     * @return boolean containing the result of the verification
     */
    fun validateCVV(cvv: Int, card: Cards?): Boolean {
        return validateCVV(cvv.toString(), card)
    }

    /**
     * enumeration representing the default cards used by Checkout
     * String name name of the card
     * String pattern regular expression matching the card's code
     * String format default card display format
     * int[] cardLength array containing all the possible lengths of the card's code
     * int[] cvvLength array containing all the possible lengths of the card's CVV
     * boolean luhn does the card's number respects the luhn validation or not
     * boolean supported is this card usable with Checkout services
     */
    enum class Cards(//check supported
        val names: String,
        val pattern: String,
        private val format: String,
        val cardLength: IntArray,
        val cvvLength: IntArray,
        val luhn: Boolean,
        private val supported: Boolean
    ) {
        MAESTRO(
            "maestro",
            "^(5[06-9]|6[37])[0-9]{10,17}$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(12, 13, 14, 15, 16, 17, 18, 19),
            intArrayOf(3),
            true,
            true
        ),
        MASTERCARD(
            "mastercard",
            "^5[0-5][0-9]{14}$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(16, 17),
            intArrayOf(3),
            true,
            true
        ),  //check supported
        DINERSCLUB(
            "dinersclub",
            "^3(?:0[0-5]|[68][0-9])?[0-9]{11}$",
            "(\\d{1,4})(\\d{1,6})?(\\d{1,4})?",
            intArrayOf(14),
            intArrayOf(3),
            true,
            true
        ),  //check supported
        LASER(
            "laser",
            "^(6304|6706|6709|6771)[0-9]{12,15}$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(16, 17, 18, 19),
            intArrayOf(3),
            true,
            false
        ),
        JCB(
            "jcb",
            "^(?:2131|1800|35[0-9]{3})[0-9]{11}$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(16),
            intArrayOf(3),
            true,
            true
        ),  //check supported
        UNIONPAY(
            "unionpay",
            "^(62[0-9]{14,17})$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(16, 17, 18, 19),
            intArrayOf(3),
            false,
            false
        ),
        DISCOVER(
            "discover",
            "^6(?:011|5[0-9]{2})[0-9]{12}$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(16),
            intArrayOf(3),
            true,
            true
        ),  //check supported
        AMEX(
            "amex",
            "^3[47][0-9]{13}$",
            "^(\\d{1,4})(\\d{1,6})?(\\d{1,5})?$",
            intArrayOf(15),
            intArrayOf(4),
            true,
            true
        ),  //check supported
        VISA(
            "visa",
            "^4[0-9]{12}(?:[0-9]{3})?$",
            DEFAULT_CARD_FORMAT,
            intArrayOf(13, 16),
            intArrayOf(3),
            true,
            true
        );
    }
}