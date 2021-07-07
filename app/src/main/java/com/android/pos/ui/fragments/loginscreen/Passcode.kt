package com.android.pos.ui.fragments.loginscreen

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.android.pos.R
import kotlinx.android.synthetic.main.fragment_passcode.*

class Passcode : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        return inflater.inflate(R.layout.fragment_passcode,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        passCodeView.setKeyTextColor(resources.getColor(R.color.white))

        val typeface: Typeface? =
            ResourcesCompat.getFont(requireActivity(), R.font.sf_pro_display_regular)
        passCodeView.setTypeFace(typeface)
    }
}