package com.android.pos.ui.fragments.loginscreen

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.AUTH_TOKEN
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.databinding.FragmentLoginBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.HostSelectionInterceptor
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject


@AndroidEntryPoint
class LoginFragment : Fragment() {


    private lateinit var binding: FragmentLoginBinding

    private val viewModel by viewModels<LoginViewModel>()

    var device_token: String = ""

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    @set:Inject
    var hostSelectionInterceptor: HostSelectionInterceptor? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.e("LoginFragment", "onCreateView")

        if (prefProvider?.getValue(AUTH_TOKEN, "").toString().isNotEmpty()) {
            if (!prefProvider?.getValueboolean(IS_CLOCKOUT, false)!!) {
                findNavController().navigate(R.id.action_login_to_passcode)
            } else {
                if (prefProvider?.getValueboolean("clockOutFromNoti", false) == true) {
                    findNavController().navigate(R.id.action_login_to_passcode, arguments)
                } else {
                    findNavController().navigate(R.id.action_login_to_dashboardCategoryBoldPOS)
                }

            }

        }

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        binding.lifecycleOwner = this
        binding.loginViewModel = viewModel

        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.txtForgotPass.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPasswordFragment)
        }

        binding.txtTerminalTitle.setOnClickListener {

            copy()
        }
        binding.terminalId.setOnClickListener {
            copy()
        }

        prefProvider?.setUniqueId((requireActivity() as MainActivity).getDeviceId())
        binding.terminalId.text = prefProvider?.getUniqueId()

        return binding.root
    }


    private fun firebaseToken() {

        
      /*  FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FirebaseMessaging", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            device_token = task.result
            prefProvider?.setValue("device_token", device_token)
            Log.d("FirebaseMessaging Token", device_token)
        })*/
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

        firebaseToken()
        binding.txtSignIn.setOnClickListener {
            prefProvider?.setBaseUrl(BASE_URL)
            hostSelectionInterceptor?.setHostBaseUrl()

            viewModel.submit(device_token)
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
                    // hostSelectionInterceptor?.setHostBaseUrl()
                    findNavController().navigate(R.id.action_login_to_passcode, bundle)
                }else{
                    prefProvider?.setValue(Constants.BASE_URL_NEW, BASE_URL)
                    hostSelectionInterceptor?.setHostBaseUrl()

                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }


}