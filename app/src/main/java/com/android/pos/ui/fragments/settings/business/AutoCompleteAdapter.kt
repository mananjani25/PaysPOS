package com.android.pos.ui.fragments.settings.business

import android.R
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.TypeFilter
import java.lang.Exception
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AutoCompleteAdapter internal constructor(
    context: Context?,
    private val placesClient: PlacesClient
) : ArrayAdapter<AutocompletePrediction>(
    context!!, R.layout.simple_expandable_list_item_2, R.id.text1
), Filterable {
    private var mResultList: List<AutocompletePrediction>? = null
    var name = "US"
    override fun getCount(): Int {
        return mResultList!!.size
    }

    override fun getItem(position: Int): AutocompletePrediction {
        return mResultList!![position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)
        val item = getItem(position)
        val textView1 = row.findViewById<TextView>(R.id.text1)
        val textView2 = row.findViewById<TextView>(R.id.text2)
        if (item != null) {
            textView1.text = item.getPrimaryText(null)
            textView2.text = item.getSecondaryText(null)
        }
        return row
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val results = FilterResults()

                // We need a separate list to store the results, since
                // this is run asynchronously.
                var filterData: List<AutocompletePrediction?>? = ArrayList()

                // Skip the autocomplete query if no constraints are given.
                if (charSequence != null && charSequence.isNotEmpty()) {
                    // Query the autocomplete API for the (constraint) search string.
                    filterData = getAutocomplete(charSequence)
                }
                results.values = filterData
                if (filterData != null) {
                    results.count = filterData.size
                } else {
                    results.count = 0
                }
                return results
            }

            override fun publishResults(charSequence: CharSequence, results: FilterResults?) {
                try {
                    if (results != null && results.count > 0) {
                        // The API returned at least one result, update the data.
                        mResultList = results.values as List<AutocompletePrediction>
                        notifyDataSetChanged()
                    } else {
                        // The API did not return any results, invalidate the data set.
                        notifyDataSetInvalidated()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun convertResultToString(resultValue: Any): CharSequence {
                // Override this method to display a readable result in the AutocompleteTextView
                // when clicked.
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getFullText(null)
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }

    private fun getAutocomplete(constraint: CharSequence): List<AutocompletePrediction?>? {
        val requestBuilder = FindAutocompletePredictionsRequest.builder()
            .setQuery(constraint.toString())
            .setCountry(name) //Use only in specific country
            .setTypeFilter(TypeFilter.ADDRESS)


        val results = placesClient.findAutocompletePredictions(requestBuilder.build())


        //Wait to get results.
        try {
            Tasks.await(results, 60, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }
        if (results.isSuccessful) {
            if (results.result != null) {
                return results.result!!.autocompletePredictions
            }
        }
        return null
    }

    fun setCountry(name: String) {
        this.name = name
    }
}