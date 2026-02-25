package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.WebKit.*
import platform.Foundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class IOSWebViewRenderer {
    fun render(url: String? = null, html: String? = null): WKWebView {
        val config = WKWebViewConfiguration()
        return WKWebView(frame = CGRectMake(0.0, 0.0, 375.0, 600.0), configuration = config).apply {
            when {
                url != null -> {
                    val nsUrl = NSURL.URLWithString(url)
                    val request = NSURLRequest.requestWithURL(nsUrl!!)
                    loadRequest(request)
                }
                html != null -> {
                    loadHTMLString(html, null)
                }
            }

            allowsBackForwardNavigationGestures = true
            scrollView.bounces = true
        }
    }
}
