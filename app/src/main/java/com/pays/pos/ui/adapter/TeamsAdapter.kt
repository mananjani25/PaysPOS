package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.callback.CustomCallback
import com.pays.pos.utils.callback.OperationCallback
import com.pays.pos.utils.extensions.getColorCompat
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.sticky_recycler.SectioningAdapter
import java.util.*

/**
 * Adapter for Person items. Sorts them by last name into sections starting with the
 * first letter of the last name.
 */
class TeamsAdapter : _root_ide_package_.com.pays.pos.utils.sticky_recycler.SectioningAdapter(), Filterable {
    private var isSelectedPos: Int = 0
    private val locale = Locale.getDefault()
    private lateinit var requireActivity: FragmentActivity


    private lateinit var mCallback: CustomCallback
    fun setCallback(callback: CustomCallback) {
        mCallback = callback
    }

    private lateinit var operationCallback: OperationCallback
    fun setOperationCallback(callback: OperationCallback) {
        operationCallback = callback
    }

    inner class Section {
        var alpha: String? = null

        var people: MutableList<Employee?>? =
            emptyList<Employee>().toMutableList()
    }

    inner class ItemViewHolder internal constructor(itemView: View) :
        _root_ide_package_.com.pays.pos.utils.sticky_recycler.SectioningAdapter.ItemViewHolder(itemView) {


        var personNameTextView: TextView = itemView.findViewById(R.id.txtName)
        var personNumberTextView: TextView = itemView.findViewById(R.id.txtNumber)
        var tvInitialName: TextView = itemView.findViewById(R.id.tvInitialName)
        var layout: LinearLayout = itemView.findViewById(R.id.layout)
        var view: View = itemView.findViewById(R.id.view_line)
        var menuOption: ImageView = itemView.findViewById(R.id.imgOrderMenu)

    }

    @SuppressLint("ClickableViewAccessibility")
    inner class HeaderViewHolder internal constructor(itemView: View) :
        _root_ide_package_.com.pays.pos.utils.sticky_recycler.SectioningAdapter.HeaderViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.txtHeader)
    }

    private var people: MutableList<Employee>? = null

    private val sections = emptyList<Section>().toMutableList()
    private var sectionSortedList = emptyList<Section>().toMutableList()

    fun getList(): MutableList<Section> {
        return sectionSortedList
    }

    fun getPeople(): MutableList<Employee>? {
        return people
    }

    fun removeItem(
        empObject: Employee,
        requireActivity: FragmentActivity
    ) {

        people?.remove(empObject)
        people?.let { it1 -> setPeople(it1, requireActivity) }

    }

    fun setPeople(
        people: MutableList<Employee>,
        requireActivity: FragmentActivity
    ) {
        this.people = people
        this.requireActivity = requireActivity
        sections.clear()
        sectionSortedList.clear()

        people.sortBy {
            it.firstName?.lowercase()
        }
        // sort people into buckets by the first letter of last name
        var alpha = 0.toChar()
        var currentSection: Section? = null
        for (person in people) {

            if (!TextUtils.isEmpty(person.firstName)) {
                val sss = person.firstName?.uppercase(Locale.ROOT)?.get(0)
                if (sss != alpha) {
                    if (currentSection != null) {
                        sections.add(currentSection)
                        sectionSortedList.add(currentSection)
                    }
                    currentSection = Section()
                    if (sss != null) {
                        alpha = sss
                    }
                    currentSection.alpha = alpha.toString()
                }
                currentSection?.people?.add(person)
            }
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
        val v: View = inflater.inflate(R.layout.view_team_item, parent, false)
        val holder = ItemViewHolder(v)
        return holder
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup, headerType: Int): HeaderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.view_team_header, parent, false)
        val holder = HeaderViewHolder(v)
        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindItemViewHolder(
        viewHolder: _root_ide_package_.com.pays.pos.utils.sticky_recycler.SectioningAdapter.ItemViewHolder,
        sectionIndex: Int,
        itemIndex: Int,
        itemType: Int
    ) {
        val s = sectionSortedList[sectionIndex]
        val ivh = viewHolder as ItemViewHolder
        val person = s.people?.get(itemIndex) as Employee


        //ivh.txtId.text = person.id.toString()

        ivh.personNumberTextView.text = person.phoneNumber?.let { AlertUtils.usNumberFormat(it) }

        if (person.lastName != null && person.lastName.isNotEmpty() && !person.lastName.equals(
                "null",
                ignoreCase = true
            )
        ) {
            var final_string =
                person.firstName.toString().substring(0,1).toUpperCase() + person.firstName.toString()
                    .substring(1, person.firstName.toString().length) + " " +
                        person.lastName.toString().substring(0,1)
                            .toUpperCase() + person.lastName.toString()
                    .substring(1, person.lastName.toString().length)
            ivh.personNameTextView.text = final_string
            ivh.tvInitialName.text =
                person.firstName?.first().toString() + person.lastName.first().toString()
        } else {
            var final_string =
                person.firstName.toString().substring(0,1).toUpperCase() + person.firstName.toString()
                    .substring(1, person.firstName.toString().length)
            ivh.personNameTextView.text = final_string
            ivh.tvInitialName.text =
                person.firstName?.subSequence(0, 2)
        }

        ivh.menuOption.setTag(R.string.tv_order_id, person)

        ivh.itemView.tag = "normal"


        val lastindex = s.people!!.size - 1

        if (lastindex == itemIndex)
            ivh.view.visibility = View.GONE
        else
            ivh.view.visibility = View.VISIBLE


        if (isSelectedPos == person.id) {
            ivh.layout.setBackgroundColor(ivh.itemView.context.getColorCompat(R.color.btnColor))
            ivh.tvInitialName.background =
                ivh.itemView.context.getDrawable(R.drawable.bg_circle_orange)
            ivh.tvInitialName.setTextColor(ivh.itemView.context.getColorCompat(R.color.btnColor))
            ivh.personNameTextView.setTextColor(ivh.itemView.context.getColorCompat(R.color.white))
            ivh.personNumberTextView.setTextColor(ivh.itemView.context.getColorCompat(R.color.txtColor))
        } else {
            ivh.layout.setBackgroundColor(ivh.itemView.context.getColorCompat(R.color.bg_color))
            ivh.personNameTextView.setTextColor(ivh.itemView.context.getColorCompat(R.color.txtColor))
            ivh.personNumberTextView.setTextColor(ivh.itemView.context.getColorCompat(R.color.txtColorGray))
            ivh.tvInitialName.background =
                ivh.itemView.context.getDrawable(R.drawable.bg_circle_gray)
            ivh.tvInitialName.setTextColor(ivh.itemView.context.getColorCompat(R.color.txtColor))
        }


        // (ivh.itemView as SwipeLayout).setItemState(SwipeLayout.ITEM_STATE_COLLAPSED, false)

        ivh.itemView.setOnClickListener {
            isSelectedPos = person.id
            notifyDataSetChanged()

            mCallback.onItemClickListener(it, person)


        }
        ivh.menuOption.setOnSingleClickListener {
            mCallback.onOptionClickListener(it, ivh)
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onBindHeaderViewHolder(
        viewHolder: _root_ide_package_.com.pays.pos.utils.sticky_recycler.SectioningAdapter.HeaderViewHolder,
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
        hvh.itemView.tag = "header";
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

    fun setSelected(selectedPos: Int) {
        isSelectedPos = selectedPos
    }


}