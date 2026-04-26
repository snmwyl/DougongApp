package edu.hebut.dougongapp

import android.graphics.Color
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.parseColor("#B5282C"),
                Color.parseColor("#4A6A8A")
            )
        )
        bottomNav.itemTextColor = colorStateList

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_showcase -> {
                    loadFragment(ShowcaseFragment())
                    true
                }
                R.id.nav_game -> {
                    loadFragment(GameFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // 销毁当前 Fragment 的 WebView
        if (currentFragment != null && currentFragment!!.isAdded) {
            supportFragmentManager.beginTransaction()
                .remove(currentFragment!!)
                .commit()
            supportFragmentManager.executePendingTransactions()
        }

        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}