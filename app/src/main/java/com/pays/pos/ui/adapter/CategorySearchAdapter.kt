package com.pays.pos.ui.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import com.pays.pos.R
import com.pays.pos.data.model.CategorySearchData
import com.bumptech.glide.Glide
import java.util.*
import javax.inject.Inject

class CategorySearchAdapter @Inject constructor(
    val mcon: Context,
    val resourceId: Int,
    var list: ArrayList<CategorySearchData>
) : ArrayAdapter<CategorySearchData>(mcon, resourceId, list) {

    private var suggestions = ArrayList<CategorySearchData>()

    init {
        suggestions = list
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = convertView
        try {
            if (convertView == null) {
                val inflater: LayoutInflater = (mcon as Activity).layoutInflater
                view = inflater.inflate(resourceId, parent, false)
                val model: CategorySearchData = getItem(position)
                val txtCategory: TextView = view.findViewById(R.id.txtCategoryName)
                val imgCategory: ImageView = view.findViewById(R.id.imgCategory)
                val txtCat: TextView = view.findViewById(R.id.txtCat)
                txtCategory.text = model.title
                txtCat.text = "in " + model.categoryName
                Glide.with(mcon).load(model.imgUrl).centerCrop().into(imgCategory)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return view!!


    }


    override fun getItem(position: Int): CategorySearchData {
        return suggestions.get(position)
    }

    override fun getCount(): Int {
        return suggestions.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getFilter(): Filter {
        return catFilter
    }


    private val catFilter: Filter = object : Filter() {
        override fun convertResultToString(resultValue: Any): CharSequence {
            val model: CategorySearchData = resultValue as CategorySearchData
            return model.title
        }

        override fun performFiltering(charSequence: CharSequence): FilterResults {
            return run {
                suggestions = if (charSequence.isEmpty()) {
                    list
                } else {
                    val fList = ArrayList<CategorySearchData>()
                    for (model in list) {
                        if (model.title.lowercase(Locale.getDefault()).trim()
                                .contains(charSequence.toString().lowercase(Locale.getDefault()).trim())
                        ) {

                            fList.add(model)
                        }

                    }
                    fList
                }
                val filterResult = FilterResults()
                filterResult.values = suggestions
                filterResult

            }

        }

        override fun publishResults(charSequence: CharSequence?, results: FilterResults?) {

            if (results != null && results.count > 0) {
                suggestions = results.values as ArrayList<CategorySearchData>
            }
            notifyDataSetChanged()

        }

    }


}