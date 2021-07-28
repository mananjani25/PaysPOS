
package com.android.pos.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.CustomerModel
import com.android.pos.databinding.ViewCustomerListBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.extensions.getColorCompat
import com.google.gson.Gson


class CustomerListAdapter(
    var context: Context,
    var list: ArrayList<com.android.pos.data.model.CustomerListResponse.Data>,
    val listner: CustomerInteface
) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>(), Filterable {
    private var filterList: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> = arrayListOf()
    private val TAG = "CustomerListAdapter"

    private var isSelectedPos: Int = -1


    inner class MyViewHolder(private val binding: ViewCustomerListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: com.android.pos.data.model.CustomerListResponse.Data) {

            if(list.get(layoutPosition).first_name != null && list.get(layoutPosition).last_name != null){
                binding.tvInitialName.setText(""+list.get(layoutPosition).first_name.first()+""+list.get(layoutPosition).last_name.first())

            }
            else{
                binding.tvInitialName.setText("${list.get(layoutPosition).first_name?.subSequence(0,2)}")
            }

            if (list.get(0).email != null){

                binding.txtNumber.setText(""+ AlertUtils.usNumberFormat(list.get(layoutPosition).phones.get(0).phone_number)+" | "+list.get(layoutPosition).email)
            }
            else{
                binding.txtNumber.setText(""+ AlertUtils.usNumberFormat(list.get(layoutPosition).phones.get(0).phone_number))

            }

            binding.model = model
            binding.executePendingBindings()

            if (isSelectedPos == layoutPosition){
                binding.layout.background = context.getDrawable(R.color.txt_color_blue)
                binding.txtName.setTextColor(context.getColorCompat(R.color.white))
                binding.txtNumber.setTextColor(context.getColorCompat(R.color.white))
            }
            else{
                binding.layout.background = context.getDrawable(R.color.white)
                binding.txtName.setTextColor(context.getColorCompat(R.color.txtColor))
                binding.txtNumber.setTextColor(context.getColorCompat(R.color.colorB9))
            }

            /* binding.root.setOnClickListener {

                 Log.e(TAG, "filterSize  ${filterList.size}")

                 //notifyDataSetChanged()
                 listner.onCustomerSelect(layoutPosition, filterList.get(layoutPosition))

             }*/
        }

        init {

            binding.root.setOnClickListener {
                isSelectedPos = layoutPosition
                notifyDataSetChanged()
                listner.onCustomerSelect(layoutPosition,list[layoutPosition])
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
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }


    interface CustomerInteface {
        fun onCustomerSelect(pos: Int, model: com.android.pos.data.model.CustomerListResponse.Data)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filterList.clear()
                    filterList.addAll(list)
                    Log.e("KeyWordEmpty", "KeyWord")
                    notifyDataSetChanged()
                } else {
                    var filterList: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> = arrayListOf()
                    for (model in list) {
                        Log.e("FilterProcess", "ModelName ${model.first_name.toLowerCase()}")
                        Log.e("FilterProcess", "Character ${charString.toLowerCase()}")
                        if (model.first_name.toLowerCase().contains(charString.toLowerCase())) {
                            filterList.add(model)
                        }
                    }

                    this@CustomerListAdapter.filterList = filterList
                }
                val filterResult = FilterResults()
                filterResult.values = filterList
                return filterResult

            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Log.e("FilterList", "countList  ${results?.count}")

                if (results?.values != null) {

                    filterList = results?.values as ArrayList<com.android.pos.data.model.CustomerListResponse.Data>
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

    fun setList(context: Context, list: ArrayList<com.android.pos.data.model.CustomerListResponse.Data>) {
        this.filterList = list
        this.context = context


    }


}