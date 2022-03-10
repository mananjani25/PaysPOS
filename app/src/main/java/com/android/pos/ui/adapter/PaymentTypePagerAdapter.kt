package com.android.pos.ui.adapter

import android.content.Context
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.android.pos.ui.fragments.checkout.PayFullAmountFragment
import com.android.pos.ui.fragments.checkout.SplitCustomAmountFragment

class PaymentTypePagerAdapter(
    context: Context,
    fragmentManager: FragmentManager?,
    totalTabs: Int
) :
    FragmentPagerAdapter(fragmentManager!!) {
    var mContext: Context
    var count: Int?=null

    override fun getCount(): Int {
        return count!!
    }

    @NonNull
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> PayFullAmountFragment()
            1 -> SplitCustomAmountFragment()
            else -> PayFullAmountFragment()
        }
    }

    init {
        mContext = context
        count = totalTabs
    }
}