package www.com.petsitternow_app.util

import android.content.Context
import www.com.petsitternow_app.BuildConfig

/**
 * Configuration for Mapbox SDK.
 * Token should be set in local.properties as: MAPBOX_ACCESS_TOKEN=your_token_here
 * Or in build.gradle.kts as: buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"your_token_here\"")
 */
object MapboxConfig {
    /**
     * Get Mapbox access token.
     * For now, returns a placeholder. In production, this should be:
     * 1. Set in local.properties: MAPBOX_ACCESS_TOKEN=your_token
     * 2. Read via BuildConfig or resources
     * 3. Or use the same token as the web app (NEXT_PUBLIC_MAPBOX_TOKEN)
     */
    @Suppress("UNUSED_PARAMETER")
    fun getAccessToken(context: Context): String {
        return BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
