package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import android.widget.TextView
import com.android.pos.R
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.CustomSwipeLayout.SwipeLayout
import com.android.pos.utils.callback.CustomCallback
import com.android.pos.utils.sticky_recycler.SectioningAdapter
import java.util.*

/**
 * Adapter for Person items. Sorts them by last name into sections starting with the
 * first letter of the last name.
 */
class TeamsAdapter : SectioningAdapter(), Filterable {
    private var isSelectedPos: Int = -1
    private val locale = Locale.getDefault()


    private lateinit var mCallback: CustomCallback
    fun setCallback(callback: CustomCallback) {
        mCallback = callback
    }

    inner class Section {
        var alpha: String? = null

        var people: MutableList<EmployeeListResponse.Data.Employee?>? =
            emptyList<EmployeeListResponse.Data.Employee>().toMutableList()
    }

    inner class ItemViewHolder internal constructor(itemView: View) :
        SectioningAdapter.ItemViewHolder(itemView), SwipeLayout.OnSwipeItemClickListener {
        init {
            (itemView as SwipeLayout).setOnSwipeItemClickListener(this)
        }

        var personNameTextView: TextView = itemView.findViewById(R.id.txtName)
        var personNumberTextView: TextView = itemView.findViewById(R.id.txtNumber)
        var tvInitialName: TextView = itemView.findViewById(R.id.tvInitialName)
        var layout: LinearLayout = itemView.findViewById(R.id.layout)


        //var txtId: TextView = itemView.findViewById(R.id.txtId)


        override fun onSwipeItemClick(left: Boolean, p1: Int) {


//            AlertUtils.showConfirmAlert(
//                itemView.context, itemView.context.getString(R.string.delete_customer_message)
//            ) { _, _ ->
//
//                val id = txtId.text.toString().trim().toInt()
////                val data = DataManager(itemView.context).customerById(id)
////                if (data != null) {
////                    mCallback.onItemClickListener(null, data)
////                }
//
//            }

        }


    }

    inner class HeaderViewHolder internal constructor(itemView: View) :
        SectioningAdapter.HeaderViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.txtHeader)

    }

    private var people: MutableList<EmployeeListResponse.Data.Employee>? = null

    private val sections = emptyList<Section>().toMutableList()
    private var sectionSortedList = emptyList<Section>().toMutableList()

    fun getList(): MutableList<Section> {
        return sectionSortedList
    }

    fun getPeople(): List<EmployeeListResponse.Data.Employee?>? {
        return people
    }

    fun setPeople(people: MutableList<EmployeeListResponse.Data.Employee>) {
        this.people = people
        sections.clear()
        sectionSortedList.clear()

        // sort people into buckets by the first letter of last name
        var alpha = 0.toChar()
        var currentSection: Section? = null
        for (person in people) {

            val sss = person.firstName?.uppercase(Locale.ROOT)?.get(0)
            Log.e("alpha 1", sss.toString())
            if (sss != alpha) {
                if (currentSection != null) {
                    sections.add(currentSection)
                    sectionSortedList.add(currentSection)
                }
                currentSection = Section()
                if (sss != null) {
                    alpha = sss
                }
                Log.e("alpha 2", alpha.toString())
                currentSection.alpha = alpha.toString()
            }
            currentSection?.people?.add(person)
        }
        if (currentSection != null) {
            sections.add(currentSection)
            sectionSortedList.add(currentSection)
        }
        notifyAllSectionsDataSetChanged()
    }

    override fun getNumberOfSections(): Int {
        return sectionSortedList.size
    }

    override fun getNumberOfItemsInSection(sectionIndex: Int): Int {
        return sectionSortedList[sectionIndex].people?.size!!
    }

    override fun doesSectionHaveHeader(sectionIndex: Int): Boolean {
        return true
    }

    override fun doesSectionHaveFooter(sectionIndex: Int): Boolean {
        return false
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, itemType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.recycler_view_item, parent, false)
        return ItemViewHolder(v)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup, headerType: Int): HeaderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.view_team_header, parent, false)
        return HeaderViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindItemViewHolder(
        viewHolder: SectioningAdapter.ItemViewHolder,
        sectionIndex: Int,
        itemIndex: Int,
        itemType: Int
    ) {
        val s = sectionSortedList[sectionIndex]
        val ivh = viewHolder as ItemViewHolder
        val person = s.people?.get(itemIndex) as EmployeeListResponse.Data.Employee


        //ivh.txtId.text = person.id.toString()

        ivh.personNumberTextView.text = AlertUtils.usNumberFormat(person.phoneNumber)

        if (person.firstName != null && person.lastName != null) {
            ivh.personNameTextView.text = person.firstName + " " + person.lastName
            ivh.tvInitialName.text =
                person.firstName.first().toString() + person.lastName.first().toString()
        } else {
            ivh.personNameTextView.text = person.firstName
            ivh.tvInitialName.text =
                person.firstName?.subSequence(0, 2)
        }

        if (isSelectedPos == ivh.layoutPosition) {
            ivh.layout.background = ivh.itemView.context.getDrawable(R.color.btnColorDark_)
        } else ivh.layout.background = ivh.itemView.context.getDrawable(R.color.white)



        (ivh.itemView as SwipeLayout).setItemState(SwipeLayout.ITEM_STATE_COLLAPSED, false)

        viewHolder.itemView.setOnClickListener {
            mCallback.onItemClickListener(it, person)

            isSelectedPos = ivh.layoutPosition
            notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindHeaderViewHolder(
        viewHolder: SectioningAdapter.HeaderViewHolder,
        sectionIndex: Int,
        headerType: Int
    ) {
        val s = sectionSortedList[sectionIndex]
        val hvh = viewHolder as HeaderViewHolder
        if (USE_DEBUG_APPEARANCE) {
            hvh.itemView.setBackgroundColor(0x55ffffff)
            hvh.titleTextView.text = pad(sectionIndex * 2) + s.alpha
        } else {
            hvh.titleTextView.text = s.alpha
        }
    }

    private fun capitalize(s: String?): String {
        return if (s != null && s.isNotEmpty()) {
            s.substring(0, 1).uppercase(locale) + s.substring(1)
        } else ""
    }

    private fun pad(spaces: Int): String {
        val b = StringBuilder()
        for (i in 0 until spaces) {
            b.append(' ')
        }
        return b.toString()
    }

    companion object {
        private const val USE_DEBUG_APPEARANCE = false
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    sectionSortedList = sections
                } else {
                    val fList = ArrayList<Section>()
                    for (row in sections) {

                        row.people?.forEach { user ->

                            var fName = user?.firstName
                            var lName = user?.lastName

                            if (fName == null)
                                fName = ""

                            if (lName == null)
                                lName = ""

                            val name = "$fName $lName"



                            if (name.lowercase(Locale.getDefault())
                                    .contains(charString.lowercase(Locale.getDefault()))
                            ) {
                                fList.add(row)
                            }
                        }


                    }

                    sectionSortedList.clear()

                    sectionSortedList = fList
                }

                val filterResults = FilterResults()
                filterResults.values = sectionSortedList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {


                if (results != null && results.count > 0) {
                    sectionSortedList = results.values as MutableList<Section>
                }

                notifyAllSectionsDataSetChanged()
            }
        }
    }


}