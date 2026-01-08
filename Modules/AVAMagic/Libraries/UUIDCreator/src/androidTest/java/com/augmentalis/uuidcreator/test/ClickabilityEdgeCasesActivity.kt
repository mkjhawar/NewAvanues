package com.augmentalis.uuidcreator.test

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.cardview.widget.CardView
import com.augmentalis.uuidcreator.R

/**
 * Synthetic test app for ClickabilityDetector edge case testing
 *
 * Tests 5 critical edge cases from LearnApp VUID Creation Fix Phase 2:
 *
 * 1. **LinearLayout Tab** (isClickable=false)
 *    - Should CREATE VUID: isFocusable=true + resourceId="tab_*" → score 0.5
 *
 * 2. **CardView** (isClickable=false)
 *    - Should CREATE VUID: isFocusable=true + ACTION_CLICK → score 0.7
 *
 * 3. **FrameLayout Wrapper** (isClickable=false)
 *    - Should CREATE VUID: Single clickable child → clickableContainer → score 0.3+
 *
 * 4. **Decorative ImageView** (isClickable=false)
 *    - Should NOT CREATE VUID: No text, no description → decorative element
 *
 * 5. **Empty View Divider** (isClickable=false)
 *    - Should NOT CREATE VUID: No children, no text → decorative element
 *
 * ## Expected Results
 * - Total elements detected: 5
 * - VUIDs created: 3 (cases 1, 2, 3)
 * - VUIDs filtered: 2 (cases 4, 5)
 * - Creation rate: 60% (3/5)
 *
 * ## Usage
 * 1. Run LearnApp exploration on this app
 * 2. Check VUID creation metrics
 * 3. Verify exactly 3 VUIDs created for cases 1, 2, 3
 *
 * @since 2025-12-08 (Phase 2: Smart Detection)
 */
class ClickabilityEdgeCasesActivity : Activity() {

    companion object {
        const val TAG = "ClickabilityEdgeCases"

        /**
         * Resource IDs for test verification
         */
        const val ID_TAB_CPU = 100001  // Edge Case 1: LinearLayout tab
        const val ID_CARD_TESTS = 100002  // Edge Case 2: CardView
        const val ID_WRAPPER = 100003  // Edge Case 3: FrameLayout wrapper
        const val ID_DECORATIVE_IMAGE = 100004  // Edge Case 4: Decorative image
        const val ID_DIVIDER = 100005  // Edge Case 5: Divider
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Main container
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        // Title
        mainLayout.addView(TextView(this).apply {
            text = "Clickability Edge Cases Test"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 32)
        })

        // Edge Case 1: LinearLayout Tab (should get VUID)
        mainLayout.addView(createEdgeCase1_LinearLayoutTab())

        // Spacer
        mainLayout.addView(createSpacer(16))

        // Edge Case 2: CardView (should get VUID)
        mainLayout.addView(createEdgeCase2_CardView())

        // Spacer
        mainLayout.addView(createSpacer(16))

        // Edge Case 3: FrameLayout Wrapper (should get VUID)
        mainLayout.addView(createEdgeCase3_FrameLayoutWrapper())

        // Spacer
        mainLayout.addView(createSpacer(16))

        // Edge Case 4: Decorative ImageView (should NOT get VUID)
        mainLayout.addView(createEdgeCase4_DecorativeImage())

        // Spacer
        mainLayout.addView(createSpacer(16))

        // Edge Case 5: Empty View Divider (should NOT get VUID)
        mainLayout.addView(createEdgeCase5_Divider())

        setContentView(mainLayout)
    }

    /**
     * EDGE CASE 1: LinearLayout Tab (isClickable=false but should be clickable)
     *
     * Signals:
     * - isClickable: false
     * - isFocusable: true (+0.3)
     * - resourceId: "tab_cpu" (+0.2)
     * - Total Score: 0.5 → LOW confidence → VUID created ✅
     */
    private fun createEdgeCase1_LinearLayoutTab(): LinearLayout {
        return LinearLayout(this).apply {
            id = ID_TAB_CPU
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }

            // NOT explicitly clickable (this is the edge case!)
            isClickable = false

            // BUT has clickability hints
            isFocusable = true

            // Set resource name (for resourceId signal)
            // NOTE: Resource names are normally set via XML, but we simulate it
            // In real apps, this would be android:id="@+id/tab_cpu" in XML

            setBackgroundColor(Color.parseColor("#E3F2FD"))
            setPadding(24, 16, 24, 16)
            gravity = Gravity.CENTER_VERTICAL

            // Tab content
            addView(TextView(this@ClickabilityEdgeCasesActivity).apply {
                text = "CPU"
                textSize = 16f
                setTextColor(Color.parseColor("#1976D2"))
            })

            // Click listener (provides ACTION_CLICK signal)
            setOnClickListener {
                Toast.makeText(
                    this@ClickabilityEdgeCasesActivity,
                    "Edge Case 1: Tab clicked (VUID should exist)",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Description for testing
            contentDescription = "Edge Case 1: LinearLayout tab (isFocusable=true, resourceId hints)"
        }
    }

    /**
     * EDGE CASE 2: CardView (isClickable=false but should be clickable)
     *
     * Signals:
     * - isClickable: false
     * - isFocusable: true (+0.3)
     * - ACTION_CLICK: present (+0.4)
     * - className: CardView (enables clickableContainer: +0.3)
     * - Total Score: 1.0 → HIGH confidence → VUID created ✅
     */
    private fun createEdgeCase2_CardView(): CardView {
        return CardView(this).apply {
            id = ID_CARD_TESTS
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }

            // NOT explicitly clickable (edge case!)
            isClickable = false

            // BUT has clickability hints
            isFocusable = true

            // Card styling
            radius = 8f
            cardElevation = 4f
            setCardBackgroundColor(Color.WHITE)

            // Card content
            val cardContent = LinearLayout(this@ClickabilityEdgeCasesActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)

                addView(TextView(this@ClickabilityEdgeCasesActivity).apply {
                    text = "Tests"
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                })

                addView(TextView(this@ClickabilityEdgeCasesActivity).apply {
                    text = "Edge Case 2: CardView with isFocusable + ACTION_CLICK"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                })
            }

            addView(cardContent)

            // Click listener (provides ACTION_CLICK)
            setOnClickListener {
                Toast.makeText(
                    this@ClickabilityEdgeCasesActivity,
                    "Edge Case 2: Card clicked (VUID should exist)",
                    Toast.LENGTH_SHORT
                ).show()
            }

            contentDescription = "Edge Case 2: CardView (isFocusable + ACTION_CLICK)"
        }
    }

    /**
     * EDGE CASE 3: FrameLayout Wrapper (isClickable=false, single clickable child)
     *
     * Signals:
     * - isClickable: false
     * - childCount: 1 (single clickable child)
     * - clickableContainer: true (+0.3)
     * - Total Score: 0.3 → NONE confidence BUT container pattern
     *
     * NOTE: This may fail threshold (0.5) depending on implementation.
     * If child itself is clickable, wrapper may not need VUID.
     */
    private fun createEdgeCase3_FrameLayoutWrapper(): FrameLayout {
        return FrameLayout(this).apply {
            id = ID_WRAPPER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }

            // NOT clickable (wrapper pattern)
            isClickable = false

            setBackgroundColor(Color.parseColor("#FFF3E0"))
            setPadding(16, 16, 16, 16)

            // Single clickable child (Button)
            addView(Button(this@ClickabilityEdgeCasesActivity).apply {
                text = "Submit"
                isClickable = true  // Child is explicitly clickable
                setOnClickListener {
                    Toast.makeText(
                        this@ClickabilityEdgeCasesActivity,
                        "Edge Case 3: Wrapper clicked (child is clickable)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

            contentDescription = "Edge Case 3: FrameLayout wrapper with single clickable child"
        }
    }

    /**
     * EDGE CASE 4: Decorative ImageView (should NOT get VUID)
     *
     * Signals:
     * - isClickable: false
     * - No text
     * - No contentDescription
     * - className: ImageView
     * → Filtered as decorative element ❌
     */
    private fun createEdgeCase4_DecorativeImage(): ImageView {
        return ImageView(this).apply {
            id = ID_DECORATIVE_IMAGE
            layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                setMargins(0, 8, 0, 8)
                gravity = Gravity.CENTER
            }

            // Decorative image (no text, no description)
            setImageResource(android.R.drawable.ic_dialog_info)
            isClickable = false

            // NO text or contentDescription (intentionally decorative)
            // This should be filtered out
        }
    }

    /**
     * EDGE CASE 5: Empty View Divider (should NOT get VUID)
     *
     * Signals:
     * - isClickable: false
     * - No children
     * - No text
     * - className: View
     * → Filtered as decorative element ❌
     */
    private fun createEdgeCase5_Divider(): android.view.View {
        return android.view.View(this).apply {
            id = ID_DIVIDER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 2).apply {
                setMargins(0, 16, 0, 16)
            }

            setBackgroundColor(Color.GRAY)
            isClickable = false

            // NO text, NO children (decorative divider)
            // This should be filtered out
        }
    }

    /**
     * Create spacer view
     */
    private fun createSpacer(heightDp: Int): android.view.View {
        return android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(heightDp))
        }
    }

    /**
     * Convert DP to pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
