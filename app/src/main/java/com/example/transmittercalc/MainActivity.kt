package com.example.transmittercalc

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.loadPrefs(this)
        setContentView(R.layout.activity_main)

        val pager: ViewPager2 = findViewById(R.id.pager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> CalcFragment()
                1 -> CalibFragment()
                2 -> RealtimeFragment()
                else -> SettingsFragment()
            }
        }

        TabLayoutMediator(findViewById<TabLayout>(R.id.tabs), pager) { tab, position ->
            tab.text = arrayOf("计算", "校准", "实时", "设置")[position]
        }.attach()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                AppState.savePrefs(this@MainActivity)
                if (position == 2) {
                    Toast.makeText(this@MainActivity, "实时模式：内置模拟数据源（演示用）", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onStop() {
        AppState.savePrefs(this)
        super.onStop()
    }
}