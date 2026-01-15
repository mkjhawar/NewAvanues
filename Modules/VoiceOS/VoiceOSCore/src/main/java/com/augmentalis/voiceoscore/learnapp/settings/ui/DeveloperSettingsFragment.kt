/**
 * DeveloperSettingsFragment.kt - UI for LearnApp Developer Settings
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-05
 *
 * Provides a tabbed interface for configuring all LearnApp developer settings.
 * Settings are organized by category with appropriate input controls.
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.google.android.material.tabs.TabLayout

/**
 * Fragment for configuring LearnApp developer settings.
 *
 * ## Features
 * - Tabbed interface organized by setting category
 * - Number inputs with validation
 * - Toggle switches for boolean settings
 * - Sliders for percentage/threshold values
 * - Reset to defaults button
 *
 * ## Usage
 * ```kotlin
 * // Navigate to developer settings
 * supportFragmentManager.beginTransaction()
 *     .replace(R.id.container, DeveloperSettingsFragment())
 *     .addToBackStack(null)
 *     .commit()
 * ```
 */
class DeveloperSettingsFragment : Fragment() {

    private lateinit var viewModel: DeveloperSettingsViewModel
    private lateinit var adapter: SettingsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout

    private val categories = listOf(
        "Exploration",
        "Navigation",
        "Login & Consent",
        "Scrolling",
        "Click & Interaction",
        "UI Detection",
        "JIT Learning",
        "State Detection",
        "Quality & Processing",
        "UI & Debug"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_developer_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            DeveloperSettingsViewModelFactory(requireContext())
        )[DeveloperSettingsViewModel::class.java]

        setupViews(view)
        setupTabs()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.settings_recycler_view)
        tabLayout = view.findViewById(R.id.settings_tab_layout)

        adapter = SettingsAdapter(
            onSettingChanged = { key, value ->
                viewModel.updateSetting(key, value)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Reset button
        view.findViewById<View>(R.id.reset_button)?.setOnClickListener {
            viewModel.resetToDefaults()
            Toast.makeText(requireContext(), "Settings reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        categories.forEach { category ->
            tabLayout.addTab(tabLayout.newTab().setText(category))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val category = categories[it.position]
                    viewModel.selectCategory(category)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Select first tab by default
        viewModel.selectCategory(categories.first())
    }

    private fun observeViewModel() {
        viewModel.settingsForCategory.observe(viewLifecycleOwner) { settings ->
            adapter.submitList(settings)
        }
    }

    companion object {
        fun newInstance(): DeveloperSettingsFragment {
            return DeveloperSettingsFragment()
        }
    }
}
