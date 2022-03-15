package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.databinding.FragmentAddItemBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.VariationDashboardListAdapter
import com.android.pos.ui.adapter.boldpos.VariationListAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddItemFragment(val listner: ItemListner) : Fragment() {
    private lateinit var item: TbItem
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentAddItemBinding
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddItemFragment"
    private lateinit var variationAdapter: VariationListAdapter

    @Inject
    lateinit var prefProvider: PrefProvider
    private var qty = 1

    companion object {
        fun newInstance(
            item: TbItem,
            callback: ItemListner,
            cartListModel: ArrayList<CartModel>
        ): AddItemFragment {
            val bundle: Bundle = Bundle()
            bundle.putParcelable("item", item)
            bundle.putSerializable("cartList", cartListModel)
            val frag = AddItemFragment(callback)
            frag.arguments = bundle
            return frag

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddItemBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        variationAdapter = VariationListAdapter()
        binding.rvVariationList.adapter = variationAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
        onClick()

    }

    private fun onClick() {
        binding.imgMinus.setOnClickListener {
            if (qty == 1) {
                qty = 1
            } else {
                qty -= 1
            }

            binding.txtQuantity.setText("" + qty)

        }
        binding.imgPlus.setOnClickListener {
            qty += 1
            binding.txtQuantity.setText("" + qty)
        }

        binding.txtCancel.setOnClickListener {
            listner.onCancelItemSelected()
        }

        binding.txtDone.setOnClickListener {
            item?.itemQuantity = qty

            viewModel.cartLogic(cartList, item, ADD)
            listner.onCancelItemSelected()

        }

    }

    private fun getData() {
        item = requireArguments().getParcelable<TbItem>("item") ?: TbItem()
        cartList = requireArguments().getSerializable("cartList") as ArrayList<CartModel>
        Log.e(TAG, "getitem  ${Gson().toJson(item)}")
        setData()
    }

    private fun setData() {
        binding.txtItem.setText("" + item?.name)

        viewModel.getItemsbyId(item?.itemId).observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        it.data?.let {
                            binding.rvVariationList.visibility = View.VISIBLE

                            variationAdapter.addVariations(it.variationsAttributes)


                            variationAdapter?.showVariationPriceClick = { it: VariationsAttribute ->
                                if (!MethodUtils.isDoubleClick()) {
                                    Log.e(TAG, "priceType:  ${it.priceType}")
                                    if (it.priceType == "Variable") {
                                        val bundle = Bundle().apply {
                                            putParcelable("variationAttribute", it)
                                        }

                                        if (item.price == 0.0 && item.variationsAttributes.isNotEmpty()) {
                                         /*   AlertUtils.showCustomAlert(
                                                requireActivity(),
                                                "Please enter atleast one price of item"
                                            )

*/                                        } else if (!checkItemQty(item, variationAdapter)) {
                                           /* AlertUtils.showCustomAlert(
                                                requireActivity(),
                                                getString(R.string.qty_validation)
                                            )*/

                                        }


                                        /*  findNavController().navigate(
                                              R.id.action_dashboardCategoryNew_to_addVariablePriceDialog, bundle
                                          )*/
                                    } else if (it.priceType == "Fixed") {
                                        showPriceTitle(it, variationAdapter = null, item, true)

                                    }

                                    val variationList = ArrayList<VariationsAttribute>()
                                    if (item.variationsAttributes.isNotEmpty()) {
                                        val variation = variationAdapter?.getItem()!!
                                        variationList.add(variation)
                                        item.name =
                                            item.name.substringBefore(" (") + " (" + variation.name + ")"
                                        item.variationsAttributes = variationList
                                    }
                                }
                            }


                            if (item.variationsAttributes.isNotEmpty() && item.variationsAttributes[0].id != null) {
                                variationAdapter.selectItem(
                                    item.variationsAttributes[0].id ?: 0
                                )
                            }


                        }

                    }
                    Status.ERROR -> {
                        binding.rvVariationList.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvVariationList.visibility = View.GONE
                    }
                }
            }


        })

    }

    private fun showPriceTitle(
        variationsAttribute: VariationsAttribute?,
        variationAdapter: VariationDashboardListAdapter?,
        data: TbItem,
        /*txtTitle: AppCompatTextView,*/
        isItemClick: Boolean
    ) {
        var variation: VariationsAttribute? = null
        if (variationAdapter != null) {
            variation = variationAdapter.getItem()
        } else if (variationsAttribute != null) {
            variation = variationsAttribute
        }



        if (variation != null) {
            variation.price?.let {
                data.price = it
            }

        } else {
            data.price = data.price
        }
        if (data.discountPrice != 0.0) {
            /*  txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                  "%.2f", (totalPrice(data) - data.discountPrice)
              )*/
        } else {
            /* if (isItemClick) {
                 txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                     "%.2f",
                     data.price
                 )
             } else
                 txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                     "%.2f",
                     totalPrice(data)
                 )*/
        }
    }

    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
    }

    private fun checkItemQty(
        data: TbItem,
        variationAdapter: VariationListAdapter?
    ): Boolean {

        if (data.variationsAttributes.isNotEmpty()) {

            val stockQty = variationAdapter?.getItem()?.stockQty

            return if (stockQty?.isNotEmpty() == true) {

                stockQty.toInt() >= 1

            } else {
                false
            }

        } else {

            return if (data.isManualSales) {
                true
            } else {
                data.quantity >= 1
            }
        }

        return false
    }


}