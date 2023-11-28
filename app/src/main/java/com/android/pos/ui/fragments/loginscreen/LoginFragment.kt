package com.android.pos.ui.fragments.loginscreen

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.pos.BuildConfig
import com.android.pos.R
import com.android.pos.data.model.responseModel.PosLinkResult
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.LOGIN_EMAIL
import com.android.pos.data.remote.Constants.LOGIN_PASSWORD
import com.android.pos.data.remote.Constants.LOGIN_REMEMBER
import com.android.pos.databinding.FragmentLoginBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.ApiModule2
import com.android.pos.di.HostSelectionInterceptor
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.utils.AdvertisingInfo
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.getCustomerDisplay
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var presentation: CustomDisplay
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()

    private lateinit var binding: FragmentLoginBinding

    private val viewModel by viewModels<LoginViewModel>()

    var device_token: String = ""

    var isRemember = false

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    @Inject
    lateinit var apiModule2: ApiModule2

    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()

    @set:Inject
    var hostSelectionInterceptor: HostSelectionInterceptor? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

//       determineAdvertisingInfo()
//        paxNetworkCall()

       /* if (prefProvider?.getValue(AUTH_TOKEN, "").toString().isNotEmpty()) {
            if (!prefProvider?.getValueboolean(IS_CLOCKOUT, false)!!) {
                findNavController().navigate(R.id.action_login_to_passcode)
            } else {
                if (prefProvider?.getValueboolean("clockOutFromNoti", false) == true) {
                    findNavController().navigate(R.id.action_login_to_passcode, arguments)
                } else {
                    if (prefProvider?.getValueboolean(ORDER_COMPLETED, false)!!) {
                        findNavController().navigate(R.id.action_login_to_passcode)
                    } else {
                        findNavController().navigate(R.id.action_login_to_dashboardCategoryBoldPOS)
                    }
                }

            }

        }*/ // due to UI glitch issue put in onViewCreated

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        binding.lifecycleOwner = this
        binding.loginViewModel = viewModel

       /* getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel

            )
        }
*/ //due to UI glitch issue put in onViewCreated
       /* versionDisplay()
        setupSnackbar()
        observeShowProgress()
        navigate()*/ //due to UI glitch issue put in onViewCreated.
        if (prefProvider?.getValueForLogin(LOGIN_REMEMBER, "") == LOGIN_REMEMBER) {
            var old_email = prefProvider?.getValueForLogin(LOGIN_EMAIL, "")
            var old_password = prefProvider?.getValueForLogin(LOGIN_PASSWORD, "")
            Log.d("yash", "onCreateView: "+old_email)
            Log.d("yash", "onCreateView: "+old_password)
            viewModel.loginDetails.value?.emailAddress = old_email
            viewModel.loginDetails.value?.password = old_password
//            binding.edtEmail.setText(old_email.toString())
//            binding.edtPassword.setText(old_password.toString())

        }

        binding.txtForgotPass.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPasswordFragment)
        }


        binding.terminalId.setOnClickListener {
            copy()
        }

        binding.imgCheckBox.setOnClickListener {
            isRemember = !isRemember
            if (isRemember) {
                binding.imgCheckBox.setImageResource(R.drawable.ic_check_box)
            } else {
                binding.imgCheckBox.setImageResource(R.drawable.ic_check_box_unchecked)
            }
        }
        prefProvider?.setUniqueId((requireActivity() as MainActivity).getDeviceId())
        binding.terminalId.text = prefProvider?.getUniqueId()
        binding.edtEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    for (span in s!!.getSpans(0, s.toString().length, UnderlineSpan::class.java)) {
                        s.removeSpan(span)
                    }
                } catch (e: Exception) {
                }
            }

        });
        return binding.root
    }

    private fun paxNetworkCall() {
        ProgressUtils.showProgressDialog("Please wait...s", requireActivity())

        var call: Call<PosLinkResult>? =
            apiModule2.getRetrofit2().getPAXDetails("NFAC0YS6", "1850067558", "")
        call!!.enqueue(object : Callback<PosLinkResult> {

            override fun onResponse(
                call: Call<PosLinkResult>,
                response: Response<PosLinkResult>
            ) {
                ProgressUtils.dismissProgressDialog()

                if (response.isSuccessful) {
                    LogUtil.logE("onResponse", response.body().toString() + response.body()!!.ipAddress)
                   var ipAddress = response.body()!!.ipAddress
                   var port = response.body()!!.port
                    Log.d("Pax Params: ", "pax $ipAddress $port")
                }
            }

            override fun onFailure(
                call: Call<PosLinkResult>,
                t: Throwable
            ) {

                ProgressUtils.dismissProgressDialog()

                AlertUtils.showCustomAlert(requireContext(), t.message)
                Log.d("onFailure: ", "Message-> ${t.message}")
            }
        })
    }



    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOut()
        }
    }

    private fun versionDisplay() {

        binding.txtVersion?.text =
            "Version : " + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")"
    }


    private fun firebaseToken() {


        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FirebaseMessaging", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            device_token = task.result
            prefProvider?.setValue("device_token", device_token)
            Log.d("FirebaseMessaging Token", device_token)
        })
    }


    private fun copy() {

        println(binding.terminalId.text.toString().trim())

        val cm: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.text = binding.terminalId.text.toString().trim()
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (prefProvider?.getValue(Constants.AUTH_TOKEN, "").toString().isNotEmpty()) {
            if (!prefProvider?.getValueboolean(Constants.IS_CLOCKOUT, false)!!) {
                findNavController().navigate(R.id.action_login_to_passcode)
            } else {
                if (prefProvider?.getValueboolean("clockOutFromNoti", false) == true) {
                    findNavController().navigate(R.id.action_login_to_passcode, arguments)
                } else {
                    if (prefProvider?.getValueboolean(Constants.ORDER_COMPLETED, false)!!) {
                        findNavController().navigate(R.id.action_login_to_passcode)
                    } else {
                        findNavController().navigate(R.id.action_login_to_dashboardCategoryBoldPOS)
                    }
                }

            }

        }

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

        versionDisplay()
        setupSnackbar()
        observeShowProgress()
        navigate()


        firebaseToken()
        binding.txtSignIn.setOnClickListener {
            prefProvider?.setBaseUrl(BASE_URL)
            hostSelectionInterceptor?.setHostBaseUrl()

            viewModel.submit(
                device_token
            )
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
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
                if (it) {
                    val bundle = Bundle().apply {
                        putBoolean("isLogin", true)
                    }
                    if (isRemember) {
                        prefProvider?.setValueForLogin(LOGIN_EMAIL, binding.edtEmail.text.toString())
                        prefProvider?.setValueForLogin(
                            LOGIN_PASSWORD,
                            binding.edtPassword.text.toString()
                        )
                        prefProvider?.setValueForLogin(LOGIN_REMEMBER, LOGIN_REMEMBER)
                    }else{
                        prefProvider?.setValueForLogin(LOGIN_EMAIL, "")
                        prefProvider?.setValueForLogin(
                            LOGIN_PASSWORD,
                            ""
                        )
                        prefProvider?.setValueForLogin(LOGIN_REMEMBER, "")
                    }
                    // hostSelectionInterceptor?.setHostBaseUrl()
                    findNavController().navigate(R.id.action_login_to_passcode, bundle)
                } else {
                    prefProvider?.setValue(Constants.BASE_URL_NEW, BASE_URL)
                    hostSelectionInterceptor?.setHostBaseUrl()

                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }


    private fun determineAdvertisingInfo() {

        viewLifecycleOwner.lifecycleScope.launch {
            prefProvider?.setUniqueId(AdvertisingInfo(requireContext()).getAdvertisingId().toString())
            binding.terminalId.text = AdvertisingInfo(requireContext()).getAdvertisingId().toString()
            Log.e("onSuccess", AdvertisingInfo(requireContext()).getAdvertisingId().toString())

        }

    }
}