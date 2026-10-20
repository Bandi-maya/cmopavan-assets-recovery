package com.logybyte.assetsrecovery.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.logybyte.assetsrecovery.R
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.databinding.ActivityMainBinding
import com.logybyte.assetsrecovery.ui.fragment.DashboardFragment
import com.logybyte.assetsrecovery.ui.fragment.GalleryFragment
import com.logybyte.assetsrecovery.ui.fragment.HelpFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupBackPress()

        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment(), getString(R.string.nav_dashboard))
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    replaceFragment(DashboardFragment(), getString(R.string.nav_dashboard))
                    true
                }
                R.id.nav_images -> {
                    replaceFragment(
                        GalleryFragment.newInstance(MediaType.IMAGE),
                        getString(R.string.nav_images)
                    )
                    true
                }
                R.id.nav_videos -> {
                    replaceFragment(
                        GalleryFragment.newInstance(MediaType.VIDEO),
                        getString(R.string.nav_videos)
                    )
                    true
                }
                R.id.nav_recovered -> {
                    replaceFragment(
                        GalleryFragment.newInstance(null, onlyRecovered = true),
                        getString(R.string.nav_recovered)
                    )
                    true
                }
                R.id.nav_help -> {
                    replaceFragment(HelpFragment(), getString(R.string.nav_help))
                    true
                }
                else -> false
            }
        }
    }

    fun navigateToTab(tabId: Int) {
        binding.bottomNavigation.selectedItemId = tabId
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        binding.toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.nav_dashboard) {
                    navigateToTab(R.id.nav_dashboard)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
