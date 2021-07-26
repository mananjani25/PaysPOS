
package com.android.pos.ui.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import com.android.pos.R
import com.android.pos.data.model.CategorySearchData
import javax.inject.Inject

class CategorySearchAdapter @Inject constructor(
    val mcon: Context,
    val resourceId: Int,
    val list: ArrayList<CategorySearchData>
) : ArrayAdapter<CategorySearchData>(mcon, resourceId, list) {

    private lateinit var items: List<CategorySearchData>
    private lateinit var tempItems: List<CategorySearchData>
    private lateinit var suggestions: MutableList<CategorySearchData>

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = convertView
        try {
            if (convertView == null) {
                val inflater: LayoutInflater = (mcon as Activity).layoutInflater
                view = inflater.inflate(resourceId, parent, false)
                val model: CategorySearchData = getItem(position)
                val txtCategory: TextView = view.findViewById(R.id.txtCategoryName)
                val imgCategory: ImageView = view.findViewById(R.id.imgCategory)
                txtCategory.setText(model.title)

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return view!!


    }


    override fun getItem(position: Int): CategorySearchData {
        return list.get(position)
    }

    override fun getCount(): Int {
        return list.size
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

        override fun performFiltering(charSequence: CharSequence?): FilterResults {
            return if (charSequence != null) {
                suggestions.clear()
                for (model in tempItems) {
                    if (model.title.toLowerCase()
                            .startsWith(charSequence.toString().toLowerCase())
                    ) {

                        suggestions.add(model)
                    }

                }
                val filterResult = FilterResults()
                filterResult.values= suggestions
                filterResult.count = suggestions.size
                filterResult

            }
            else{
                FilterResults()
            }

        }

        override fun publishResults(charSequence: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                clear()

                val filteredList: ArrayList<CategorySearchData> = results.values as ArrayList<CategorySearchData>
                if (results != null && results.count > 0) {
                    clear()
                    for (c in filteredList) {
                        add(c)
                    }
                    notifyDataSetChanged()
                }
                notifyDataSetChanged()

            } else {
                clear()
                notifyDataSetChanged()
            }

        }

    }

    init {

        this.items = list
        tempItems = ArrayList(items)
        suggestions = ArrayList()
    }


}