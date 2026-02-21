/**
 * TestClickabilityActivity.kt - Synthetic test app for VUID creation validation
 *
 * Tests 7 edge cases for clickability detection:
 * 1. LinearLayout tab (isClickable=false, isFocusable=true, has listener)
 * 2. CardView (isClickable=false, isFocusable=true, has listener)
 * 3. FrameLayout wrapper (has clickable child)
 * 4. Decorative ImageView (should be filtered)
 * 5. Divider View (should be filtered)
 * 6. Button (isClickable=true explicitly)
 * 7. MaterialCardView (Material Design component)
 *
 * Expected: 5/7 VUIDs created (cases 1,2,3,6,7)
 *
 * Date: 2025-12-08
 */

package com.augmentalis.testapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

class TestClickabilityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create root layout
        val rootLayout = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        rootLayout.addView(contentLayout)

        // Title
        contentLayout.addView(createTitle("VUID Creation Test App"))
        contentLayout.addView(createDescription("Testing 7 edge cases for clickability detection"))
        contentLayout.addView(createSpacer(24))

        // CASE 1: LinearLayout tab
        contentLayout.addView(createSectionHeader("Case 1: LinearLayout Tab"))
        contentLayout.addView(createDescription("isClickable=false, isFocusable=true, has listener"))
        contentLayout.addView(createLinearLayoutTab())
        contentLayout.addView(createExpectedResult("Expected: VUID created ✓"))
        contentLayout.addView(createSpacer(16))

        // CASE 2: CardView
        contentLayout.addView(createSectionHeader("Case 2: CardView"))
        contentLayout.addView(createDescription("isClickable=false, isFocusable=true, has listener"))
        contentLayout.addView(createCardView())
        contentLayout.addView(createExpectedResult("Expected: VUID created ✓"))
        contentLayout.addView(createSpacer(16))

        // CASE 3: FrameLayout wrapper
        contentLayout.addView(createSectionHeader("Case 3: FrameLayout Wrapper"))
        contentLayout.addView(createDescription("Has clickable child button"))
        contentLayout.addView(createFrameLayoutWrapper())
        contentLayout.addView(createExpectedResult("Expected: VUID created ✓"))
        contentLayout.addView(createSpacer(16))

        // CASE 4: Decorative ImageView
        contentLayout.addView(createSectionHeader("Case 4: Decorative ImageView"))
        contentLayout.addView(createDescription("No text, no description, no listener"))
        contentLayout.addView(createDecorativeImageView())
        contentLayout.addView(createExpectedResult("Expected: Filtered ✗"))
        contentLayout.addView(createSpacer(16))

        // CASE 5: Divider
        contentLayout.addView(createSectionHeader("Case 5: Divider View"))
        contentLayout.addView(createDescription("Empty view, no children, no text"))
        contentLayout.addView(createDivider())
        contentLayout.addView(createExpectedResult("Expected: Filtered ✗"))
        contentLayout.addView(createSpacer(16))

        // CASE 6: Button
        contentLayout.addView(createSectionHeader("Case 6: Explicit Button"))
        contentLayout.addView(createDescription("isClickable=true explicitly"))
        contentLayout.addView(createButton())
        contentLayout.addView(createExpectedResult("Expected: VUID created ✓"))
        contentLayout.addView(createSpacer(16))

        // CASE 7: MaterialCardView
        contentLayout.addView(createSectionHeader("Case 7: MaterialCardView"))
        contentLayout.addView(createDescription("Material Design component"))
        contentLayout.addView(createMaterialCardView())
        contentLayout.addView(createExpectedResult("Expected: VUID created ✓"))
        contentLayout.addView(createSpacer(24))

        // Summary
        contentLayout.addView(createSectionHeader("Expected Results"))
        contentLayout.addView(createDescription("5/7 VUIDs created (cases 1,2,3,6,7)"))
        contentLayout.addView(createDescription("2/7 elements filtered (cases 4,5)"))

        setContentView(rootLayout)
    }

    // CASE 1: LinearLayout tab
    private fun createLinearLayoutTab(): View {
        return LinearLayout(this).apply {
            id = View.generateViewId()
            isFocusable = true
            // isClickable deliberately NOT set (defaults to false)
            setOnClickListener { showToast("CPU Tab Clicked") }
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Add text content
            addView(TextView(this@TestClickabilityActivity).apply {
                text = "CPU Tab"
                textSize = 18f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            })
        }
    }

    // CASE 2: CardView
    private fun createCardView(): View {
        return CardView(this).apply {
            id = View.generateViewId()
            isFocusable = true
            // isClickable deliberately NOT set
            setOnClickListener { showToast("Tests Card Clicked") }
            radius = 8f
            cardElevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Card content
            val cardContent = LinearLayout(this@TestClickabilityActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            cardContent.addView(TextView(this@TestClickabilityActivity).apply {
                text = "Tests"
                textSize = 20f
                setTextColor(Color.BLACK)
            })

            cardContent.addView(TextView(this@TestClickabilityActivity).apply {
                text = "View test results"
                textSize = 14f
                setTextColor(Color.GRAY)
            })

            addView(cardContent)
        }
    }

    // CASE 3: FrameLayout wrapper
    private fun createFrameLayoutWrapper(): View {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Single clickable child
            addView(Button(this@TestClickabilityActivity).apply {
                text = "Submit"
                setOnClickListener { showToast("Submit Clicked") }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    // CASE 4: Decorative ImageView
    private fun createDecorativeImageView(): View {
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64)
            setImageResource(android.R.drawable.ic_dialog_info)
            // No click listener
            // No text
            // No content description
            // Should be filtered as decorative
        }
    }

    // CASE 5: Divider
    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            setBackgroundColor(Color.GRAY)
            // No children
            // No text
            // No click listener
            // Should be filtered
        }
    }

    // CASE 6: Button
    private fun createButton(): View {
        return Button(this).apply {
            text = "Rate This App"
            setOnClickListener { showToast("Rate App Clicked") }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // isClickable=true by default for Button
        }
    }

    // CASE 7: MaterialCardView
    private fun createMaterialCardView(): View {
        return MaterialCardView(this).apply {
            id = View.generateViewId()
            isFocusable = true
            // isClickable deliberately NOT set
            setOnClickListener { showToast("Material Card Clicked") }
            radius = 12f
            cardElevation = 6f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Card content
            addView(LinearLayout(this@TestClickabilityActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)

                addView(TextView(this@TestClickabilityActivity).apply {
                    text = "Material Card"
                    textSize = 18f
                    setTextColor(Color.BLACK)
                })

                addView(TextView(this@TestClickabilityActivity).apply {
                    text = "Click to interact"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                })
            })
        }
    }

    // Helper functions
    private fun createTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
    }

    private fun createSectionHeader(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.parseColor("#1976D2"))
            setPadding(0, 0, 0, 8)
        }
    }

    private fun createDescription(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 8)
        }
    }

    private fun createExpectedResult(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#388E3C"))
            setPadding(0, 4, 0, 0)
        }
    }

    private fun createSpacer(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
