package com.android.pos.ui.fragments.loginscreen

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.BuildConfig
import com.android.pos.R
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.TERMINAL_NAME
import com.android.pos.databinding.FragmentPasscodeBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.getCustomerDisplay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class Passcode : Fragment() {

    private lateinit var presentation: CustomDisplay
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()

    private var isLogin: Boolean = false
    private lateinit var binding: FragmentPasscodeBinding
    private val viewModel by viewModels<PasscodeViewModel>()
    private val viewModelDashboard by activityViewModels<DashBoardCategoryViewModel>()
    var isDashboard: Boolean = false
    var isClockOut: Boolean = false
    var isSwap: Boolean = false
    var isExit: Boolean = false
    var validationmsg: String = ""
    var selectedList: ArrayList<TextView> = arrayListOf()

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var prefProvider: PrefProvider

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    if ((isSwap || isDashboard) && !isExit) {
                        findNavController().navigateUp()
                    } else {
                        (requireActivity() as MainActivity).finish()
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_passcode, container, false)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        binding.lifecycleOwner = this
        binding.passcodeViewModel = viewModel

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel

            )
        }

        setTimeAndDate()

        isDashboard = arguments?.getBoolean("isDashboard")!!
        isSwap = arguments?.getBoolean("isSwap")!!
        isExit = arguments?.getBoolean("isExit")!!

        if (isSwap) {
            binding.tvWelcomeTag.text = getString(R.string.tv_clock_in)
        } else {
            if (isDashboard) {
                binding.tvWelcomeTag.text = getString(R.string.tv_clock_out)
                viewModel.isDashboardData(isDashboard)
            }
        }




        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.txtVersion?.text =
            "Snack v." + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")"
        isLogin = arguments?.getBoolean("isLogin") ?: false



        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(this::presentation.isInitialized){
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }
    }

    private fun setTimeAndDate() {

        Log.d("terminalId", "setTimeAndDate: "+prefProvider.getValueInt(TERMINAL_ID,-1))
        viewModel.getTimeDetails(prefProvider.getValueInt(TERMINAL_ID,-1)).observe(requireActivity()) {
            it.data?.let {
                LogUtil.logE("TAG", "timeDetails ${it.data}")
                binding.currentTime.text = it.data.time
                binding.currentDate.text = it.data.date
                binding.txtLocationName?.text = it.data.locationName
                prefProvider.setValue(TERMINAL_NAME,it.data.terminalName)
                if (prefProvider.getValue(TERMINAL_NAME,"").isNotEmpty() && prefProvider.getValue(TERMINAL_NAME,"").isNotBlank()) {
                    binding.llStationName?.visibility  = View.VISIBLE
                    binding.txtStationName?.text = prefProvider.getValue(TERMINAL_NAME,"")
                }
            }
        }
    }


    fun changeBackgroundSelected(
        txtview: TextView,
        selectedList: ArrayList<TextView>,
        isSelected: Boolean
    ) {
//        if (isSelected) {
//            txtview.setBackgroundResource(R.drawable.passcode_viewselected)
//        }
//        for (i in selectedList.indices) {
//            selectedList[i].setBackgroundResource(R.drawable.passcode_viewunselected)
//        }
    }

    private fun onclickPasscode() {
        binding.passcodeView.first.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.first, selectedList, true)
            binding.passcodeView.circlePin.append("1")
        }
        binding.passcodeView.second.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.second, selectedList, true)
            binding.passcodeView.circlePin.append("2")
        }
        binding.passcodeView.third.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.third, selectedList, true)
            binding.passcodeView.circlePin.append("3")
        }
        binding.passcodeView.fourth.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.fourth, selectedList, true)
            binding.passcodeView.circlePin.append("4")
        }
        binding.passcodeView.five.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.five, selectedList, true)
            binding.passcodeView.circlePin.append("5")
        }
        binding.passcodeView.six.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.six, selectedList, true)
            binding.passcodeView.circlePin.append("6")
        }
        binding.passcodeView.seven.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.seven, selectedList, true)
            binding.passcodeView.circlePin.append("7")
        }
        binding.passcodeView.eight.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.eight, selectedList, true)
            binding.passcodeView.circlePin.append("8")
        }
        binding.passcodeView.nine.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.first)
            selectedList.add(binding.passcodeView.zero)
            changeBackgroundSelected(binding.passcodeView.nine, selectedList, true)
            binding.passcodeView.circlePin.append("9")
        }
        binding.passcodeView.zero.setOnClickListener {
            selectedList = ArrayList()
            selectedList.add(binding.passcodeView.second)
            selectedList.add(binding.passcodeView.third)
            selectedList.add(binding.passcodeView.fourth)
            selectedList.add(binding.passcodeView.five)
            selectedList.add(binding.passcodeView.six)
            selectedList.add(binding.passcodeView.seven)
            selectedList.add(binding.passcodeView.eight)
            selectedList.add(binding.passcodeView.nine)
            selectedList.add(binding.passcodeView.first)
            changeBackgroundSelected(binding.passcodeView.zero, selectedList, true)
            binding.passcodeView.circlePin.append("0")
        }
        binding.passcodeView.clear.setOnClickListener {
            clearBackground()
            binding.passcodeView.circlePin.setText("")
        }
        binding.passcodeView.backspace.setOnClickListener {
            clearBackground()
            val text = binding.passcodeView.circlePin.text.toString()
            if (text.isNotEmpty()) {
                val temptext = text.substring(0, text.length - 1)
                binding.passcodeView.circlePin.setText(temptext)
            }
        }
    }

    fun clearBackground() {
        selectedList = ArrayList()
        selectedList.add(binding.passcodeView.second)
        selectedList.add(binding.passcodeView.third)
        selectedList.add(binding.passcodeView.fourth)
        selectedList.add(binding.passcodeView.five)
        selectedList.add(binding.passcodeView.six)
        selectedList.add(binding.passcodeView.seven)
        selectedList.add(binding.passcodeView.eight)
        selectedList.add(binding.passcodeView.nine)
        selectedList.add(binding.passcodeView.first)
        selectedList.add(binding.passcodeView.zero)
        changeBackgroundSelected(binding.passcodeView.zero, selectedList, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()


        val typeface: Typeface? =
            ResourcesCompat.getFont(requireActivity(), R.font.sf_pro_display_regular)
        binding.passcodeView.circlePin.setTypeface(typeface)
        onclickPasscode()
        binding.passcodeView.circlePin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                var value = s.toString()
                if (value.length == 4) {
                    LogUtil.logE("passCodeView", value)
                    viewModel.submit(value)
                }

                Log.d("yash", "afterTextChanged: $value")
            }

        })
        binding.Cancel.setOnClickListener {
            if ((isSwap || isDashboard) && !isExit) {
                findNavController().navigateUp()
            } else {
                (requireActivity() as MainActivity).finish()
            }
        }

    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                clearBackground()
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun navigate() {
        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                validationmsg = it.message
                if (isDashboard) {
                    isDashboard = false
                    isClockOut = true
                    viewModel.isDashboardData(isDashboard)
                    clearBackground()
                    binding.passcodeView.circlePin.setText("")
                    binding.tvWelcomeTag.text = getString(R.string.tv_clock_in)
                    AlertUtils.showCustomAlert(requireContext(), validationmsg)

                    if (prefProvider.getValue(ORDER_TYPE, "").isNotEmpty()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModelDashboard.decreaseOnGoingOrderCounter(false)
                        }
                    }

                } else {
                    prefProvider.setValue(ORDER_TYPE, "")
                    findNavController().navigate(R.id.action_passcode_to_dashboardCategoryBoldPOS)
                }
            }
        }

        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("action_passcode", it.toString())
                findNavController().navigate(R.id.action_passcode_to_login)

            }
        }

    }

    private fun setupSnackbar() {
        viewModel.snackbarText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled().let {
                clearBackground()
                binding.passcodeView.circlePin.setText("")
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    it
                )
            }

        }
    }
}