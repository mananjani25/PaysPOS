package com.android.pos.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.pos.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.parent_activity)
    }
}