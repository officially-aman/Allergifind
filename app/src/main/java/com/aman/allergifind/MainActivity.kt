package com.aman.allergifind

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.aman.allergifind.adapters.OnboardingAdapter
import com.aman.allergifind.adapters.OnboardingItem
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = ContextCompat.getColor(this, R.color.dark)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)

        // Initialize adapter with onboarding content and pass context
        val adapter = OnboardingAdapter(
            listOf(
                OnboardingItem(
                    "Welcome to Allergified!",
                    "Discover the ingredients in packed products and learn whether they are good for your health. Stay informed and make healthier choices.",
                    R.drawable.allergified // Your app logo
                ),
                OnboardingItem(
                    "Track Ingredients with Ease",
                    "Scan product labels and instantly get detailed information about each ingredient's health impact. Stay organized and manage your food choices effortlessly.",
                    R.drawable.camera // Camera icon
                )
            ), this
        )
        viewPager.adapter = adapter

        // Attach TabLayout to ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
    }
}

