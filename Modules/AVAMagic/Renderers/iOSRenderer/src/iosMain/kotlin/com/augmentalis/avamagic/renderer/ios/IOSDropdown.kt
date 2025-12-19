package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanues.avamagic.ui.core.form.DropdownComponent
import com.augmentalis.avanues.avamagic.ui.core.form.DropdownOption

@OptIn(ExperimentalForeignApi::class)
class IOSDropdownRenderer {
    fun render(component: DropdownComponent): UIView {
        val containerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 200.0)
            backgroundColor = UIColor.systemBackgroundColor
        }

        var yOffset = 0.0

        // Add label if provided
        component.label?.let { labelText ->
            val label = UILabel().apply {
                frame = CGRectMake(16.0, yOffset, 343.0, 24.0)
                text = labelText
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.secondaryLabelColor
            }
            containerView.addSubview(label)
            yOffset += 32.0
        }

        // Use UIPickerView for dropdown
        val pickerView = UIPickerView().apply {
            frame = CGRectMake(0.0, yOffset, 375.0, 150.0)

            // Set selected row
            component.selectedIndex?.let { index ->
                selectRow(index.toLong(), 0, false)
            }

            // Note: In production, implement UIPickerViewDataSource and UIPickerViewDelegate
            // dataSource and delegate would populate options and handle selection
        }

        containerView.addSubview(pickerView)

        return containerView
    }
}
