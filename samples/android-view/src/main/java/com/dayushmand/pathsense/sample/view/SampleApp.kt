package com.dayushmand.pathsense.sample.view

import android.app.Application
import com.dayushmand.pathsense.ui.PathOverlayConfig
import com.dayushmand.pathsense.ui.PathSense
import com.dayushmand.pathsense.ui.PathSenseConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PathSense.init(
            this,
            PathSenseConfig(
                overlayConfig = PathOverlayConfig(showCoordinateHUD = true),
            ),
        )
    }
}
