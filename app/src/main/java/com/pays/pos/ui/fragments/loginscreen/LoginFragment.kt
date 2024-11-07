package com.pays.pos.ui.fragments.loginscreen

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pays.pos.BuildConfig
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.PosLinkResult
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.LOGIN_EMAIL
import com.pays.pos.data.remote.Constants.LOGIN_PASSWORD
import com.pays.pos.data.remote.Constants.LOGIN_REMEMBER
import com.pays.pos.databinding.FragmentLoginBinding
import com.pays.pos.di.ApiModule.BASE_URL
import com.pays.pos.di.ApiModule2
import com.pays.pos.di.HostSelectionInterceptor
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.utils.AdvertisingInfo
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.getCustomerDisplay
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.utils.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private val PERMISSION_REQUEST_CODE = 123

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.CAMERA,
        Manifest.permission.VIBRATE,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.USE_FULL_SCREEN_INTENT,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                showToast()
            }
        }


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

        try {
            binding.txtBottom.text = resources.getString(
                R.string.tv_pos_2021,
                Calendar.getInstance().get(Calendar.YEAR).toString()
            )
        } catch (e: Exception) {

        }
//        binding.txtSignIn.isEnabled = false

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
            Log.d("yash", "onCreateView: " + old_email)
            Log.d("yash", "onCreateView: " + old_password)
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
        syncDevices()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.zip(grantResults.toTypedArray())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            if (deniedPermissions.isNotEmpty()) {
                checkAndRequestPermissions()
            }
        }
    }

    private fun syncDevices() {

        if (Build.VERSION.SDK_INT >= 23) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else {
            showToast()
        }
    }

    private fun showToast() {
        Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
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
                    LogUtil.logE(
                        "onResponse",
                        response.body().toString() + response.body()!!.ipAddress
                    )
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
            presentation.onLogOutOrClockOut(true)
        }
        dashboardViewModel.loggingOut = false
//        CoroutineScope(Dispatchers.Main).launch{
//            binding.txtSignIn.apply {
//                isClickable = false
//                isEnabled = false
//
//                setBackgroundColor(resources.getColor(R.color.view))
//
//                try {
//                    viewLifecycleOwner.lifecycleScope.async(Dispatchers.IO) {
//                        try {
//                            AppDatabase.getDatabase(requireActivity().applicationContext)
//                                .itemDao().delete()
//                        }catch (e:Exception){Log.e("ClearDataLogoutCrash", e.toString())}
//                    }.await()
//                } catch (e:Exception) {
//                    Log.e("ClearDataLogoutCrash", e.toString())
//                }
//
//
////                dashboardViewModel._showProgress.value = (Event(false))
//                isEnabled = true
//                isClickable = true
//
//                setBackgroundColor(resources.getColor(R.color.btnColor))
//            }
//        }
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
                        prefProvider?.setValueForLogin(
                            LOGIN_EMAIL,
                            binding.edtEmail.text.toString()
                        )
                        prefProvider?.setValueForLogin(
                            LOGIN_PASSWORD,
                            binding.edtPassword.text.toString()
                        )
                        prefProvider?.setValueForLogin(LOGIN_REMEMBER, LOGIN_REMEMBER)
                    } else {
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
            prefProvider?.setUniqueId(
                AdvertisingInfo(requireContext()).getAdvertisingId().toString()
            )
            binding.terminalId.text =
                AdvertisingInfo(requireContext()).getAdvertisingId().toString()
            Log.e("onSuccess", AdvertisingInfo(requireContext()).getAdvertisingId().toString())

        }

    }
}