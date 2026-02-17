package com.dayushmand.pathsense.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Runtime debug check using ApplicationInfo.FLAG_DEBUGGABLE.
 * This works reliably regardless of BuildConfig generation.
 */
internal var pathSenseDebugOverride: Boolean? = null

private var cachedAppContext: Application? = null

internal fun initDebugContext(app: Application) {
    cachedAppContext = app
}

internal fun initDebugContextFrom(context: Context) {
    if (cachedAppContext == null) {
        cachedAppContext = context.applicationContext as? Application
    }
}

internal fun isDebugBuild(): Boolean {
    pathSenseDebugOverride?.let { return it }
    val app = cachedAppContext ?: return false
    return (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
