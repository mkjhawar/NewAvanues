package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanues.avamagic.ui.core.feedback.DialogComponent

@OptIn(ExperimentalForeignApi::class)
class IOSDialogRenderer {
    fun render(component: DialogComponent): UIAlertController {
        return UIAlertController.alertControllerWithTitle(
            title = component.title,
            message = component.content,
            preferredStyle = UIAlertControllerStyleAlert
        ).apply {
            // Add confirm button
            val confirmAction = UIAlertAction.actionWithTitle(
                title = component.confirmLabel,
                style = UIAlertActionStyleDefault,
                handler = { /* onConfirm callback */ }
            )
            addAction(confirmAction)

            // Add cancel button if provided
            component.cancelLabel?.let { cancelText ->
                val cancelAction = UIAlertAction.actionWithTitle(
                    title = cancelText,
                    style = UIAlertActionStyleCancel,
                    handler = { /* onCancel callback */ }
                )
                addAction(cancelAction)
            }
        }
    }
}
