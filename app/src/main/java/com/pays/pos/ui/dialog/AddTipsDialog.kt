package com.pays.pos.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DailogAddTipsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.DialogTipsListAdapter
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.AmountTextWatcher
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.google.gson.Gson
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.payment.PaymentBoldPosFragment
import com.pays.pos.ui.fragments.transactions.TransactionDetailsFragment
import com.pays.pos.ui.fragments.transactions.TransactionFragment
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AddTipsDialog : DialogFragment(), DialogTipsListAdapter.DiscountInterface {

    private var rate: Double = 0.00
    private var tipID: Int? = null
    private var clickOnSave : Boolean = false   // used for checking tipID when clicked on save/continue
    private var clickOnSaveId : Int = -1        // used for saving tipID when clicked on save/continue
    private var totalTip: Double = 0.00
    private var totalPrice: Double = 0.0
    private lateinit var binding: DailogAddTipsBinding
    private val viewModel by activityViewModels<TipListViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddDiscountDialog"
    private lateinit var tipsListAdapter: DialogTipsListAdapter
    private var tipModel: GetTipReponse.Data? = null
    var selectedListPos: Int = -1
    var splitCount = 1
    private var llkeypadClicked : Boolean = false
    var isAmountWiseSplit = false
    var amountWiseSplit = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider
    private var isFromDetails = false
    private var isFromTransaction = false

//    companion object {
//        fun newInstance() = AddTipsDialog()
//        //Added By Rahul Pandit to solve PA1-I792 *Start*
//        fun clearSavedTip(context: Context) {
//            val sharedPreferences = context.getSharedPreferences("TipPrefs", Context.MODE_PRIVATE)
//            sharedPreferences.edit().remove("selected_tip_id").apply()
//        }//Added By Rahul Pandit to solve PA1-I792 *End*
//
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dashboardViewModel.isAmountWiseSplit.value?.let {
            if (it)
                isAmountWiseSplit = true
        }

        dashboardViewModel.amountWiseSplit.value?.let {
            if (it > 0.0)
                amountWiseSplit = it
        }

        if (arguments != null) {
            if (arguments?.getDouble("totalPrice") != null) totalPrice =
                requireArguments().getDouble("totalPrice")
            if (arguments?.getDouble("totalTip") != null) totalTip =
                requireArguments().getDouble("totalTip")

            if (arguments?.getInt("splitCount") != null) {
                splitCount =
                    requireArguments().getInt("splitCount")
            }

            if (arguments?.getBoolean("isFromTransaction") != null) {
                isFromTransaction = requireArguments().getBoolean("isFromTransaction", false)
            }
        }

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 80, 150, 80)
        dialog?.window?.setBackgroundDrawable(inset);

        binding.txtTitle.text = getString(R.string.add_tips)

        tipsListAdapter = DialogTipsListAdapter(prefProvider)
        binding.rvDiscountList.adapter = tipsListAdapter

        llkeypadClicked = false
        clickOnSave = prefProvider.getValueboolean("save_button_clicked",false)
        clickOnSaveId = prefProvider.getValueInt("save_button_clicked_id",-1)
        if(clickOnSave  && totalTip > 0.0){
            tipID = clickOnSaveId
            saveSelectedTip(clickOnSaveId)
        }else{
            tipID = null
            saveSelectedTip(-1)
        }

        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, true))
        binding.edtAmount.setText("" + MethodUtils.roundOffAmountString(totalTip))



        setDiscountList()
        setupData()
        setKeyPad()
        onClick()

        val currentFragment = parentFragmentManager.fragments.find { it.isVisible }

        currentFragment?.let {
            when (it) {
                is TransactionDetailsFragment, is TransactionFragment -> {
                    binding.txtRemove?.gone()
                }

                else -> {
                    binding.txtRemove?.visible()
                }
            }
        }
    }

    private fun onClick() {


        binding.llKeypad.txt10.setOnClickListener {
            rate = binding.llKeypad.txt10.text.toString().trim()
                .substring(0, binding.llKeypad.txt10.text.toString().length - 1).toDouble()

            resetDialogTipsList()

            var price = 0.0
            price = if (isFromTransaction) {
                MethodUtils.percentageCalculation(
                    totalPrice, rate
                )
            } else {
                MethodUtils.percentageCalculation(
                    if (isAmountWiseSplit) amountWiseSplit else
                    prefProvider.getValue(
                        Constants.WHOLE_AMOUNT,
                        "0.0"
                    ).toDouble()  / splitCount, rate
                )
            }

            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
            saveSelectedTip(-1)
            tipID = null
            resetDialogTipsList()
        }
        binding.llKeypad.txt20.setOnClickListener {
            rate = binding.llKeypad.txt20.text.toString().trim()
                .substring(0, binding.llKeypad.txt20.text.toString().length - 1).toDouble()
            resetDialogTipsList()//Added by Rahul Pandit to solve PA1-I870
            var price = 0.0
            price = if (isFromTransaction) {
                MethodUtils.percentageCalculation(
                    totalPrice, rate
                )
            } else {
                MethodUtils.percentageCalculation(
                    if (isAmountWiseSplit) amountWiseSplit else
                    prefProvider.getValue(
                        Constants.WHOLE_AMOUNT,
                        "0.0"
                    ).toDouble() / splitCount, rate
                )
            }
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
            saveSelectedTip(-1)
            tipID = null
            resetDialogTipsList()
        }
        binding.llKeypad.txt30.setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {
                rate = binding.llKeypad.txt30.text.toString().trim()
                    .substring(0, binding.llKeypad.txt30.text.toString().length - 1).toDouble()
                resetDialogTipsList()//Added by Rahul Pandit to solve PA1-I870

                var price = 0.0
                price = if (isFromTransaction) {
                    MethodUtils.percentageCalculation(
                        totalPrice, rate
                    )
                } else {
                    MethodUtils.percentageCalculation(
                        if (isAmountWiseSplit) amountWiseSplit else
                        prefProvider.getValue(
                            Constants.WHOLE_AMOUNT,
                            "0.0"
                        ).toDouble() / splitCount, rate
                    )
                }
                binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
                saveSelectedTip(-1)
                tipID = null
                resetDialogTipsList()
            }
        })

//        binding.llKeypad.txt30.setOnClickListener {
//
//
//
//        }
    }

    private fun resetDialogTipsList() {
        tipsListAdapter.discountList.forEach {
            it.isCheckedInAdapter=false
        }
        tipsListAdapter.notifyDataSetChanged()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddTipsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        return binding.root
    }

    private fun setKeyPad() {
        binding.llKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.llKeypad.tvTwo.setOnClickListener {
            calculateValue("2", false)

        }

        binding.llKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)

        }

        binding.llKeypad.tvFour.setOnClickListener {

            calculateValue("4", false)
        }

        binding.llKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }

        binding.llKeypad.tvSix.setOnClickListener {
            calculateValue("6", false)
        }

        binding.llKeypad.tvSeven.setOnClickListener {
            calculateValue("7", false)
        }

        binding.llKeypad.tvEight.setOnClickListener {
            calculateValue("8", false)
        }

        binding.llKeypad.tvNine.setOnClickListener {
            calculateValue("9", false)
        }

        binding.llKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)
        }

        binding.llKeypad.tvClear.setOnClickListener {
            dashboardViewModel.customerGivenTip.value=false
            calculateValue("", true)
            tipsListAdapter.clearSelectedItem()
            clearSavedTip()
            saveSelectedTip(-1)
            tipID = null
            resetDialogTipsList()
        }
        binding.llKeypad.tvDZero.setOnClickListener {
            calculateValue("00", false)
        }

    }

    private fun setDiscountList() {
        viewModel.getTipActiveList.observe(requireActivity()) {
            LogUtil.logE(TAG, "DiscountList ${Gson().toJson(it)}")
            if (it.data?.isNotEmpty() == true) {

                it.data.forEach {
                    it.isChecked = false
                }
                tipsListAdapter.setList(it.data)
                tipsListAdapter.setListner(this)
            }


        }


    }

    private fun setupData() {

        binding.llKeypad.txtClear.setOnClickListener {
            dashboardViewModel.customerGivenTip.value=false
            binding.edtAmount.setText("0.00")

            tipsListAdapter.clearSelectedItem()
            selectedListPos = -1
//            clearSavedTip()//Added By Rahul Pandit to solve PA1-I792
            saveSelectedTip(-1)
            tipID = null
            resetDialogTipsList()
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSave.setOnClickListener {

            var amount = 0.0

            amount = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()
            if (amount >0.0){
                val result = Bundle().apply {
                    putDouble("tipAmount", amount)
                    putDouble("tipPercent",rate)
                    tipID?.let { putInt("tipId", tipID ?: 0) }

                }
                if (isFromTransaction) {
                    setFragmentResult("request_key_tips", result)
                } else {
                    requireActivity().supportFragmentManager.setFragmentResult(
                        "request_key_tips",
                        result
                    )
                }
                if(llkeypadClicked){
                    tipID = null
                    resetDialogTipsList()
                }
                saveSelectedTip(tipID?:-1)//Added By Rahul Pandit to solve PA1-I792
                saveClickOnSaveTip(true,tipID ?: -1)
                findNavController().navigateUp()
            }else{
                AlertUtils.showCustomAlert(requireContext(), "Please enter tip amount")
                return@setOnClickListener
            }

        }

        binding.txtRemove?.setOnClickListener {




            val result = Bundle().apply {
                putDouble("tipAmount", 0.0)
                putDouble("tipPercent",0.0)
                tipID?.let { putInt("tipId", tipID ?: 0) }

            }
            dashboardViewModel.customerGivenTipBefore.value = false
            if (isFromTransaction) {
                setFragmentResult("request_key_tips", result)
            } else {
                requireActivity().supportFragmentManager.setFragmentResult(
                    "request_key_tips",
                    result
                )
            }

            dashboardViewModel.tipRemovedObserver.value = true
            clearSavedTip()//Added By Rahul Pandit to solve PA1-I792
            saveClickOnSaveTip(false, -1)
            findNavController().navigateUp()
        }
    }


    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)


    }

    override fun selectedItem(model: GetTipReponse.Data, pos: Int) {
        LogUtil.logE(TAG, "SelectedItem:  ${Gson().toJson(model)}")
        if (model.isCheckedInAdapter){
            rate = model.rate
            tipModel.apply { model }
            tipID = model.id
            var tipCalculation = 0.0
            if (isFromTransaction) {
                tipCalculation = (totalPrice * model.rate) / 100
            } else {
                totalPrice =
                    if (isAmountWiseSplit) {
                        amountWiseSplit
                    } else {
                        prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / splitCount
                    }
                tipCalculation = (totalPrice * model.rate) / 100
            }
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(tipCalculation))
            selectedListPos = pos
            saveSelectedTip(model.id ?:-1) //Added By Rahul Pandit to solve PA1-I792
        }else{
            binding.edtAmount.setText("0.00")
        }

    }

    //Added By Rahul Pandit to solve PA1-I792 *Start*
    private fun saveSelectedTip(tipId: Int) {
        //        val sharedPreferences = requireContext().getSharedPreferences("TipPrefs", Context.MODE_PRIVATE)
//        sharedPreferences.edit()
//            .putInt("selected_tip_id", tipId)
//            .apply()
        prefProvider.setValueInt("selected_tip_id", tipId)
    }
    private fun saveClickOnSaveTip(check: Boolean, id: Int?) {
        prefProvider.setValueboolean("save_button_clicked", check)
        id?.let { prefProvider.setValueInt("save_button_clicked_id", it) }
    }
    private fun clearSavedTip() {
        //        val sharedPreferences = requireContext().getSharedPreferences("TipPrefs", Context.MODE_PRIVATE)
//        sharedPreferences.edit().remove("selected_tip_id").apply()
        prefProvider.deleteValue("selected_tip_id")
    }
    //Added By Rahul Pandit to solve PA1-I792 *End*




    private fun calculateValue(number: String, delete: Boolean) {
        tipsListAdapter.clearSelectedItem()
        tipID = null
        selectedListPos = -1
        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))

        } else {
            binding.edtAmount.append(number)
            saveSelectedTip(-1)
            llkeypadClicked = true

//            if(!prefProvider.getValueboolean("save_button_clicked",false)){
//                saveClickOnSaveTip(false,-1)
//                tipID = null
//            }
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }


}