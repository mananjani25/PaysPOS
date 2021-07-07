package com.android.pos.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.pos.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = getColor(R.color.txtColorGray)
        }
        setContentView(R.layout.parent_activity)


//         passCodeView.setKeyTextColor(resources.getColor(R.color.white))
//
//         val typeface: Typeface? =
//             ResourcesCompat.getFont(this, R.font.sf_pro_display_regular)
//         passCodeView.setTypeFace(typeface)
    }
}