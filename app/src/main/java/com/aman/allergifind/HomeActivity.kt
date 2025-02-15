package com.aman.allergifind

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aman.allergifind.Fragment.AboutFragment
import com.aman.allergifind.Fragment.BarcodeFragment
import com.aman.allergifind.Fragment.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Create fragments
        val homeFragment = HomeFragment()
        val barcodeFragment = BarcodeFragment()
        val aboutFragment = AboutFragment()

        // Set the default fragment to show when the app starts
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_wrapper, homeFragment)
                .commit()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> makeCurrentFragment(homeFragment)
                R.id.barcode -> makeCurrentFragment(barcodeFragment)
                R.id.about -> makeCurrentFragment(aboutFragment)
            }
            true
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // Check if the fragment is already added
        if (!fragment.isAdded) {
            // If not added, use add() instead of replace() to avoid recreation
            fragmentTransaction.replace(R.id.fl_wrapper, fragment)
        }

        fragmentTransaction.commit()
    }
}

