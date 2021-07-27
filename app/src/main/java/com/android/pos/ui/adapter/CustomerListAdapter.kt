package com.android.pos.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CustomerModel
import com.android.pos.databinding.ViewCustomerListBinding


class CustomerListAdapter(
    var context: Context,
    var list: ArrayList<CustomerModel>,
    val listner: CustomerInteface
) :
    RecyclerView.Adapter<CustomerListAdapter.MyViewHolder>(), Filterable {
    private var filterList: ArrayList<CustomerModel> = arrayListOf()

    inner class MyViewHolder(private val binding: ViewCustomerListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: CustomerModel) {
            binding.model = model
            binding.executePendingBindings()

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
        return if (list != null) {
            filterList.size
        } else {
            0
        }
    }


    interface CustomerInteface {
        fun onCustomerSelect(pos: Int)
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
                    var filterList: ArrayList<CustomerModel> = arrayListOf()
                    for (model in list) {
                        Log.e("FilterProcess", "ModelName ${model.name.toLowerCase()}")
                        Log.e("FilterProcess", "Character ${charString.toLowerCase()}")
                        if (model.name.toLowerCase().contains(charString.toLowerCase())) {
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

                    filterList = results?.values as ArrayList<CustomerModel>
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

    fun setList(context: Context, list: ArrayList<CustomerModel>) {
        this.filterList = list
        this.context = context


    }


}