package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.AVKit.*
import platform.AVFoundation.*
import platform.Foundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class IOSVideoPlayerRenderer {
    fun render(url: String, autoplay: Boolean = false, controls: Boolean = true): AVPlayerViewController {
        val videoUrl = NSURL.URLWithString(url)
        val player = AVPlayer.playerWithURL(videoUrl!!)

        return AVPlayerViewController().apply {
            this.player = player
            showsPlaybackControls = controls

            view.frame = CGRectMake(0.0, 0.0, 375.0, 210.0)

            if (autoplay) {
                player.play()
            }
        }
    }
}
