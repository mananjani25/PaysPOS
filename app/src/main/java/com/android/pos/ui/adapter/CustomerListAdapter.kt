package com.android.pos.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.CustomerModel
import com.android.pos.databinding.ViewCustomerListBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.extensions.getColorCompat
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList


class CustomerListAdapter(
    var context: Context,
    var list: ArrayList<com.android.pos.data.model.CustomerListResponse.Data>,
    val listner: CustomerInteface
) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>(), Filterable {
    private var filterList: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> =
        arrayListOf()
    private val TAG = "CustomerListAdapter"

    private var isSelectedPos: Int = -1

    init {

        filterList = list
    }


    inner class MyViewHolder(private val binding: ViewCustomerListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: com.android.pos.data.model.CustomerListResponse.Data) {

            try {
                if (filterList.get(bindingAdapterPosition).first_name != null && filterList.get(
                        bindingAdapterPosition
                    ).last_name != null
                ) {
                    binding.tvInitialName.setText(
                        filterList.get(bindingAdapterPosition).first_name.first() + "" + filterList.get(
                            bindingAdapterPosition
                        ).last_name.first()
                    )

                } else {
                    binding.tvInitialName.setText(
                        "${
                            filterList.get(bindingAdapterPosition).first_name?.subSequence(
                                0,
                                2
                            )
                        }"
                    )
                }

                if (filterList.get(bindingAdapterPosition).email != null && filterList.get(bindingAdapterPosition).phones.size > 0) {

                    binding.txtNumber.setText(
                        "" + AlertUtils.usNumberFormat(
                            filterList.get(bindingAdapterPosition).phones.get(
                                0
                            ).phone_number
                        ) + " | " + filterList.get(bindingAdapterPosition).email
                    )
                } else if (filterList.get(bindingAdapterPosition).phones.size > 0) {
                    binding.txtNumber.setText(
                        "" + AlertUtils.usNumberFormat(
                            filterList.get(bindingAdapterPosition).phones.get(
                                0
                            ).phone_number
                        )
                    )

                }

                binding.model = model
                binding.executePendingBindings()

                if (isSelectedPos == layoutPosition) {
                    binding.layout.background = context.getDrawable(R.color.txt_color_blue)
                    binding.txtName.setTextColor(context.getColorCompat(R.color.white))
                    binding.txtNumber.setTextColor(context.getColorCompat(R.color.white))
                } else {
                    binding.layout.background = context.getDrawable(R.color.white)
                    binding.txtName.setTextColor(context.getColorCompat(R.color.txtColor))
                    binding.txtNumber.setTextColor(context.getColorCompat(R.color.colorB9))
                }

                /* binding.root.setOnClickListener {

                 Log.e(TAG, "filterSize  ${filterList.size}")

                 //notifyDataSetChanged()
                 listner.onCustomerSelect(layoutPosition, filterList.get(layoutPosition))

             }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

     init {

         binding.root.setOnClickListener {
             isSelectedPos = layoutPosition
             notifyDataSetChanged()
             listner.onCustomerSelect(layoutPosition, filterList[layoutPosition])
         }

     }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomerListAdapter.MyViewHolder {
        val binding = ViewCustomerListBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: CustomerListAdapter.MyViewHolder, position: Int) {
        holder.bind(filterList[position])
    }

    override fun getItemCount(): Int {
        if (list.size > 0) {

            return filterList.size

        } else {
            return 0
        }
    }


    interface CustomerInteface {
        fun onCustomerSelect(pos: Int, model: com.android.pos.data.model.CustomerListResponse.Data)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val charString = charSequence.toString()
                /*  if (charString.isEmpty()) {
                      filterList.clear()
                      filterList.addAll(list)
                      Log.e("KeyWordEmpty", "KeyWord")
                      notifyDataSetChanged()
                  } else {*/
                Log.e(TAG, "charSequence:  ${charSequence}")
                /*var filterList: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> =
                    arrayListOf()
                for (model in this@CustomerListAdapter.list) {
                    Log.e("FilterProcess", "ModelName ${model.first_name.toLowerCase()}")
                    Log.e("FilterProcess", "Character ${charString.toLowerCase()}")

                    if (model.first_name.toLowerCase()
                            .contains(charString.toLowerCase()) || model.last_name.toLowerCase()
                            .contains(charString.toLowerCase())
                    ) {
                        filterList.add(model)
                    }
                }


                if (filterList.size != 0) {
                    this@CustomerListAdapter.filterList = filterList
                }
                *//*}*//*
                val filterResult = FilterResults()
                filterResult.values = filterList
                return filterResult*/

                val chatString = charSequence.toString()
                filterList = if (charString.isEmpty()) {
                    list
                } else {
                    val fList = ArrayList<CustomerListResponse.Data>()
                    list.filter {
                        (it.first_name.lowercase(Locale.getDefault())
                            .contains(charString)) || (it.last_name.lowercase(Locale.getDefault())
                            .contains(charString))
                    }.forEach { fList.add(it) }

                    fList
                }

                return FilterResults().apply { values = filterList }


            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Log.e("FilterList", "countList  ${results?.count}")

                if (results?.values != null) {

                    filterList =
                        results?.values as ArrayList<com.android.pos.data.model.CustomerListResponse.Data>
                } else {
                    filterList = list
                }
                notifyDataSetChanged()
            }

        }
    }

    /*fun setMovieList(context: Context?, movieList: ArrayList<CustomerModel>) {
        this.context = context!!
        if (this.list == null) {
            this.list = list
            this.movieListFiltered = list
            notifyItemChanged(0, movieListFiltered?.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return list.size
                }

                override fun getNewListSize(): Int {
                    return movieList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return list.get(oldItemPosition)
                        .name.toLowerCase().startsWith(movieList[newItemPosition].name.toLowerCase())
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val newMovie: CustomerModel = list.get(oldItemPosition)
                    val oldMovie = movieList[newItemPosition]
                    return newMovie.name.toLowerCase().startsWith(oldMovie.name.toLowerCase())
                }
            })
            list = movieList
            this.movieListFiltered = movieList
            result.dispatchUpdatesTo(this)
        }
    }*/

    fun setList(
        context: Context,
        list: ArrayList<com.android.pos.data.model.CustomerListResponse.Data>
    ) {
        this.filterList = list
        this.context = context


    }

    fun getItem(position: Int): com.android.pos.data.model.CustomerListResponse.Data {
        return filterList[position]
    }

}