package com.augmentalis.voiceos.ui.activities

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.augmentalis.voiceos.AccessibilitySetupHelper

/**
 * Visual demonstration of accessibility service setup flow
 */
class AccessibilitySetupActivity : Activity() {

    private lateinit var helper: AccessibilitySetupHelper
    private lateinit var statusCard: LinearLayout
    private lateinit var statusIcon: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusMessage: TextView
    private lateinit var toggleSwitch: Switch
    private lateinit var actionButton: Button
    private lateinit var instructionsCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper = AccessibilitySetupHelper(this)

        // Create the UI
        setContentView(createSetupUI())

        // Check initial status
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun createSetupUI(): View {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Header
        TextView(this).apply {
            text = "VoiceOS Setup"
            textSize = 28f
            setTextColor(Color.parseColor("#1976D2"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
            container.addView(this)
        }

        // Status Card
        statusCard = createStatusCard()
        container.addView(statusCard)

        // Instructions Card
        instructionsCard = createInstructionsCard()
        container.addView(instructionsCard)

        // Action Button
        actionButton = Button(this).apply {
            text = "Open Accessibility Settings"
            textSize = 16f
            setPadding(48, 32, 48, 32)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
            setOnClickListener {
                startActivity(helper.openAccessibilitySettings())
                Toast.makeText(
                    this@AccessibilitySetupActivity,
                    "Look for 'VoiceOS Accessibility' in the list",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        container.addView(actionButton)

        scrollView.addView(container)
        return scrollView
    }

    private fun createStatusCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
            elevation = 8f

            // Status Header Row
            val headerRow = LinearLayout(this@AccessibilitySetupActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Status Icon (emoji as text)
            statusIcon = TextView(this@AccessibilitySetupActivity).apply {
                text = "⚠️"
                textSize = 48f
                setPadding(0, 0, 24, 0)
            }
            headerRow.addView(statusIcon)

            // Status Text Container
            val textContainer = LinearLayout(this@AccessibilitySetupActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            statusTitle = TextView(this@AccessibilitySetupActivity).apply {
                text = "Accessibility Service"
                textSize = 20f
                setTextColor(Color.BLACK)
            }
            textContainer.addView(statusTitle)

            if (helper.isServiceEnabled()) {
                statusMessage = TextView(this@AccessibilitySetupActivity).apply {
                    text = "Running"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    setPadding(0, 4, 0, 0)
                }
                textContainer.addView(statusMessage)
            } else {
                statusMessage = TextView(this@AccessibilitySetupActivity).apply {
                    text = "Not Enabled"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    setPadding(0, 4, 0, 0)
                }
                textContainer.addView(statusMessage)
            }

            headerRow.addView(textContainer)

            // Toggle Switch
            toggleSwitch = Switch(this@AccessibilitySetupActivity).apply {
                isClickable = false
                isFocusable = false
            }
            toggleSwitch.isChecked = helper.isServiceEnabled()
            headerRow.addView(toggleSwitch)

            addView(headerRow)

            // Divider
            View(this@AccessibilitySetupActivity).apply {
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    topMargin = 24
                    bottomMargin = 24
                }
                addView(this)
            }

            // Info Text
            TextView(this@AccessibilitySetupActivity).apply {
                text = "VoiceOS needs accessibility permission to:\n" +
                        "• Control your device with voice commands\n" +
                        "• Navigate apps hands-free\n" +
                        "• Click buttons and interact with UI elements"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                lineHeight = 28
                addView(this)
            }
        }
    }

    private fun createInstructionsCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            setPadding(32, 32, 32, 32)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
            }

            // Instructions Title
            TextView(this@AccessibilitySetupActivity).apply {
                text = "📋 How to Enable"
                textSize = 18f
                setTextColor(Color.parseColor("#1565C0"))
                setPadding(0, 0, 0, 16)
                addView(this)
            }

            // Step by step
            val steps = listOf(
                "1️⃣ Tap 'Open Accessibility Settings' below",
                "2️⃣ Find 'VoiceOS Accessibility' in the list",
                "3️⃣ Tap on it to open settings",
                "4️⃣ Toggle the switch to ON",
                "5️⃣ Tap 'Allow' in the permission dialog"
            )

            steps.forEach { step ->
                TextView(this@AccessibilitySetupActivity).apply {
                    text = step
                    textSize = 14f
                    setTextColor(Color.parseColor("#424242"))
                    setPadding(0, 8, 0, 8)
                    addView(this)
                }
            }
        }
    }

    private fun updateUI() {
        var isEnabled = helper.isServiceEnabled()

        if (isEnabled) {
            // Service is enabled
            statusIcon.text = "✅"
            statusTitle.text = "Accessibility Service"
            statusMessage.text = "Enabled and Running"
            statusMessage.setTextColor(Color.parseColor("#4CAF50"))
            toggleSwitch.isChecked = true

            actionButton.apply {
                text = "Service is Active ✓"
                setBackgroundColor(Color.parseColor("#2196F3"))
                isEnabled = false
            }

            instructionsCard.visibility = View.GONE

            // Auto-close after 2 seconds when enabled
            statusCard.postDelayed({
                finish()
            }, 3000)

        } else {
            // Service is not enabled
            statusIcon.text = "⚠️"
            statusTitle.text = "Accessibility Service"
            statusMessage.text = "Not Enabled - Tap below to setup"
            statusMessage.setTextColor(Color.parseColor("#F44336"))
            toggleSwitch.isChecked = false

            actionButton.apply {
                text = "Open Accessibility Settings"
                setBackgroundColor(Color.parseColor("#4CAF50"))
                isEnabled = true
            }

            instructionsCard.visibility = View.VISIBLE
        }
    }
}