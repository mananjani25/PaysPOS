package com.pays.pos.ui.fragments.magtek

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.entities.TbCardReader
import com.pays.pos.databinding.FragmentTagtekBinding
import com.pays.pos.di.MagtekModule
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.magtekCallback
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.statusUtils.Status
import com.magtek.mobile.android.mtlib.IMTCardData
import com.magtek.mobile.android.mtlib.MTConnectionState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MagtekFragment : Fragment(), ItemCallback, magtekCallback {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var selectedPos: Int = -1
    private lateinit var mContaxt: Context
    private var adapter: MagtakAdapter? = null
    private lateinit var binding: FragmentTagtekBinding
    private var mScanning = false
    private var m_connectionState = MTConnectionState.Disconnected
    private val viewModel by viewModels<MagtekViewModel>()

    @Inject
    lateinit var magtekModule: MagtekModule


    @SuppressLint("MissingPermission")
    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            //  you will get result here in result.data
            pairedDeviceList()

        }

    }


    @SuppressLint("MissingPermission")
    private fun pairedDeviceList() {

        with(magtekModule) {
            setupInit()
            scanBluetoothDevice(true)
            scanLeDevice(true)
        }
    }


    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device!!.bondState != BluetoothDevice.BOND_BONDED) {
                    if (device != null) {
                        if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                            addCardReader(device)
                        }
                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                mScanning = false

            }
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
                pairedDeviceList()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.fragment_tagtek,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        magtekModule.setCallback(this)


        val bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        mBluetoothAdapter = bluetoothManager.adapter


        setup()
        setupAdapter()
        syncDevices()


        return binding.root
    }

    private fun syncDevices() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startForResult.launch(enableBtIntent)
        }
    }


    private fun setup() {


        binding.txtRefresh.setOnClickListener {
            selectedPos = -1
            adapter?.clear()
            viewModel.deleteTable()

            magtekModule.scanLeDevice(true)
            magtekModule.scanBluetoothDevice(true)
        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_magtekFragment_to_dashboardCategoryBoldPOS)
        }

        mContaxt = requireContext()
        mScanning = false

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        requireContext().registerReceiver(mReceiver, filter)


    }

    private fun setupAdapter() {

        binding.rvDeviceList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = MagtakAdapter()
        adapter?.setCallback(this)
        binding.rvDeviceList.adapter = adapter

        viewModel.cardReaderList().observe(viewLifecycleOwner) {

            if (it.status == Status.SUCCESS && it.data != null) {
                LogUtil.logE("observe", it.data?.name.toString() + "  " + it.data?.status.toString())
                adapter?.add(it.data)
            }
        }

    }


    private fun setState(deviceState: MTConnectionState) {
        m_connectionState = deviceState
        updateDisplay()
    }

    private fun updateDisplay() {
        runOnUiThread {
            when (m_connectionState) {
                MTConnectionState.Connected -> {

                    if (selectedPos != -1) {


                        val cardReader = adapter?.getItem(selectedPos)
                        cardReader?.status = 1
                        if (cardReader != null) {
                            adapter?.update(selectedPos, 1)
                            viewModel.updateCardReader(cardReader)
                        }
                    }
                }
                MTConnectionState.Disconnected -> {
                    LogUtil.logE("Disconnected", true.toString())
                    if (selectedPos != -1) {

                        val cardReader = adapter?.getItem(selectedPos)
                        cardReader?.status = 0
                        if (cardReader != null) {
                            adapter?.update(selectedPos, 0)
                            viewModel.updateCardReader(cardReader)
                        }
                    }
                }
                else -> {
                }
            }
        }
    }


    override fun onItemClickListener(view: View?, pos: Int) {

        selectedPos = pos

        if (adapter?.getItem(pos)?.status == 0) {
            if (adapter?.getItem(pos)?.mcAddress?.let { magtekModule.openDevice(it) } != 0L) {
                LogUtil.logE("onItemClick", "[Failed to connect to the device]")
            }
        } else {
            magtekModule.closeDevice()

            val cardReader = adapter?.getItem(selectedPos)
            cardReader?.status = 0
            if (cardReader != null) {
                adapter?.update(selectedPos, 0)
                viewModel.updateCardReader(cardReader)
            }
        }


    }

    override fun startScanning() {

        ProgressUtils.showProgressDialog(requireActivity())
    }

    override fun processStart(s: String, b: Boolean) {


    }

    override fun stopScanning() {

        ProgressUtils.dismissProgressDialog()
    }

    override fun onConnect(deviceState: MTConnectionState) {

        setState(deviceState)
    }

    override fun onDeviceResponse(response: String) {
        LogUtil.logE("onDeviceResponse", response)
    }

    override fun onDeviceList(bluetoothDevice: BluetoothDevice) {

        addCardReader(bluetoothDevice)


    }

    @SuppressLint("MissingPermission")
    private fun addCardReader(bluetoothDevice: BluetoothDevice) {

        if (view != null)
        viewModel.cardReaderById(bluetoothDevice.address.replace(":", ""))
            .observe(viewLifecycleOwner) {

                if (selectedPos == -1) {
                    LogUtil.logE("addCardReader", it.status.toString())
                    if (it.status == Status.SUCCESS) {
                        if (it.data == null) {
                            LogUtil.logE("addCardReader", "callled")
                            val cardReader = TbCardReader()
                            LogUtil.logE("mcAddress :: ", bluetoothDevice.address.replace(":", ""))
                            cardReader.mcAddress = bluetoothDevice.address.replace(":", "")
                            if (bluetoothDevice.name != null) {
                                cardReader.name = bluetoothDevice.name
                            } else {
                                cardReader.name = "Unknown"
                            }

                            cardReader.type = bluetoothDevice.type
                            viewModel.addCardReader(cardReader)
                        }
                    }
                }

            }


    }

    /**
     * Get Mac address with colon added or removed
     * @author YOLANDA
     * @param mac
     * @return
     */


    override fun OnCardDataReceived(imtCardData: IMTCardData) {
    }

    override fun OnARQCReceived(bytes: ByteArray) {

    }


}