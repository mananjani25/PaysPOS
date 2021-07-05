package com.android.pos.ui.ui.activities

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import com.android.pos.R
import kotlinx.android.synthetic.main.fragment_passcode.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_login)

       /* passCodeView.setKeyTextColor(resources.getColor(R.color.white))

        val typeface: Typeface? =
            ResourcesCompat.getFont(this, R.font.sf_pro_display_regular)
        passCodeView.setTypeFace(typeface)*/
    }
}