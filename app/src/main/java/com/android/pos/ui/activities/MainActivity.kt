package com.android.pos.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.pos.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.parent_activity.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var navController: NavController? = null
    private lateinit var listner: NavController.OnDestinationChangedListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = getColor(R.color.txtColorGray)
        }
        setContentView(R.layout.parent_activity)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFrag) as NavHostFragment
        navController = navHostFragment.navController

//        navController = findNavController(R.id.navHostFrag) as NavHostFragment

        listner = NavController.OnDestinationChangedListener { controller, destination, arguments ->

            if (destination.id == R.id.dashboard || destination.id == R.id.dashboardCategory) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            }
        }
        nav_view.setupWithNavController(navController!!)
        val imgBack = nav_view.getHeaderView(0).findViewById<ImageView>(R.id.imgBack)
        imgBack.setOnClickListener {
            closeDrawer()

        }



        nav_view.setNavigationItemSelectedListener {


            when (it.itemId) {
                R.id.menuHome -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuOrders -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuTransactions -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuCashLog -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuReports -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuCustomers -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuTeam -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.memuInventory -> {
                    closeDrawer()
                    navController?.navigate(R.id.action_global_inventory)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuSettings -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuSupport -> {
                    closeDrawer()
                    return@setNavigationItemSelectedListener true
                }

            }

            return@setNavigationItemSelectedListener false
        }
        // setContentView(R.layout.fragment_custom_item)


//         passCodeView.setKeyTextColor(resources.getColor(R.color.white))
//
//         val typeface: Typeface? =
//             ResourcesCompat.getFont(this, R.font.sf_pro_display_regular)
//         passCodeView.setTypeFace(typeface)
    }

    override fun onResume() {
        super.onResume()
        navController?.addOnDestinationChangedListener(listner)
    }

    override fun onPause() {
        super.onPause()
        navController?.removeOnDestinationChangedListener(listner)
    }

    fun openDrawer() {
        drawer_layout.openDrawer(GravityCompat.START)

    }

    fun closeDrawer() {
        drawer_layout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        navController?.navigateUp()
        return super.onSupportNavigateUp()
    }

}