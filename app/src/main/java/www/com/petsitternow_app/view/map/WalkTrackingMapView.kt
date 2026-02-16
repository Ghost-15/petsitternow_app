package www.com.petsitternow_app.view.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.RouteInfo
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.util.MapboxConfig

/**
 * Custom MapView for displaying real-time walk tracking.
 * Shows owner and petsitter positions with optional route.
 */
class WalkTrackingMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ROUTE_SOURCE_ID = "route-source"
        private const val ROUTE_LAYER_ID = "route-layer"
    }

    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var currentStyle: Style? = null
    private var infoOverlay: TextView? = null

    private var ownerLocation: WalkLocation? = null
    private var petsitterLocation: WalkLocation? = null
    private var route: RouteInfo? = null
    private var walkStatus: WalkStatus? = null
    private var accessToken: String = ""

    init {
        initializeMap()
    }

    private fun initializeMap() {
        // Get Mapbox token
        accessToken = MapboxConfig.getAccessToken(context)
        if (accessToken.isEmpty()) {
            // Show error message
            val errorView = TextView(context).apply {
                text = "Token Mapbox manquant. Configurez MAPBOX_ACCESS_TOKEN dans local.properties"
                setPadding(32, 32, 32, 32)
                setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }
            addView(errorView)
            return
        }

        // Set Mapbox access token BEFORE creating MapView
        MapboxOptions.accessToken = accessToken

        // Initialize MapView
        mapView = MapView(context).apply {
            getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                setupMapStyle(style)
            }
        }
        addView(mapView)

        // Create info overlay - positioned at bottom
        @Suppress("MagicNumber")
        infoOverlay = TextView(context).apply {
            setBackgroundResource(R.drawable.bg_map_info_overlay)
            setPadding(32, 16, 32, 16)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            visibility = View.GONE
            elevation = 4f
        }
        val overlayParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            bottomMargin = 16
        }
        addView(infoOverlay, overlayParams)
    }

    private fun setupMapStyle(style: Style) {
        currentStyle = style

        // Initialize point annotation manager for markers
        pointAnnotationManager = mapView?.annotations?.createPointAnnotationManager()

        // Create custom marker icons
        createMarkerIcons(style)

        // Setup route source using DSL
        style.addSource(
            geoJsonSource(ROUTE_SOURCE_ID) {
                // Empty initial source
            }
        )

        // Setup route layer using DSL
        style.addLayer(
            lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineWidth(4.0)
                visibility(Visibility.NONE)
            }
        )
    }

    private fun createMarkerIcons(style: Style) {
        // Create blue marker for owner
        val ownerBitmap = createMarkerBitmap(Color.parseColor("#3b82f6"), Color.WHITE)
        style.addImage("owner-marker", ownerBitmap)

        // Create green marker for petsitter
        val petsitterBitmap = createMarkerBitmap(Color.parseColor("#10b981"), Color.WHITE)
        style.addImage("petsitter-marker", petsitterBitmap)
    }

    private fun createMarkerBitmap(color: Int, borderColor: Int): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Draw border circle
        paint.color = borderColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw inner circle
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 4, paint)

        return bitmap
    }

    /**
     * Update owner location marker.
     */
    fun setOwnerLocation(location: WalkLocation) {
        ownerLocation = location
        updateMarkers()
        updateCamera()
    }

    /**
     * Update petsitter location marker.
     */
    fun setPetsitterLocation(location: WalkLocation?) {
        petsitterLocation = location
        updateMarkers()
        updateCamera()
    }

    /**
     * Update route display.
     */
    fun setRoute(route: RouteInfo?, status: WalkStatus?) {
        this.route = route
        this.walkStatus = status
        updateRoute()
        updateInfoOverlay()
    }

    private fun updateMarkers() {
        val manager = pointAnnotationManager ?: return
        manager.deleteAll()

        // Add owner marker (blue)
        ownerLocation?.let { location ->
            val ownerPoint = Point.fromLngLat(location.lng, location.lat)
            val ownerAnnotation = PointAnnotationOptions()
                .withPoint(ownerPoint)
                .withIconImage("owner-marker")
                .withIconSize(1.0)

            manager.create(ownerAnnotation)
        }

        // Add petsitter marker (green)
        petsitterLocation?.let { location ->
            val petsitterPoint = Point.fromLngLat(location.lng, location.lat)
            val petsitterAnnotation = PointAnnotationOptions()
                .withPoint(petsitterPoint)
                .withIconImage("petsitter-marker")
                .withIconSize(1.0)

            manager.create(petsitterAnnotation)
        }
    }

    private fun updateRoute() {
        val style = currentStyle ?: return
        val layer = style.getLayerAs<LineLayer>(ROUTE_LAYER_ID) ?: return

        if (route == null || walkStatus == null) {
            // Hide route
            layer.visibility(Visibility.NONE)
            return
        }

        // Show route only for specific statuses
        val shouldShowRoute = walkStatus == WalkStatus.GOING_TO_OWNER ||
                walkStatus == WalkStatus.ASSIGNED ||
                walkStatus == WalkStatus.RETURNING

        if (!shouldShowRoute) {
            layer.visibility(Visibility.NONE)
            return
        }

        // Set route color based on status
        val routeColor = if (walkStatus == WalkStatus.RETURNING) {
            Color.parseColor("#f97316") // Orange for returning
        } else {
            Color.parseColor("#10b981") // Green for going to owner
        }

        // Convert coordinates to LineString
        val coordinates = route!!.coordinates.map { (lng, lat) ->
            Point.fromLngLat(lng, lat)
        }
        val lineString = LineString.fromLngLats(coordinates)
        val feature = Feature.fromGeometry(lineString)

        // Update source data
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.feature(feature)

        // Update layer visibility and color
        layer.lineColor(routeColor)
        layer.visibility(Visibility.VISIBLE)
    }

    private fun updateCamera() {
        val mapboxMap = mapView?.getMapboxMap() ?: return

        // Calculate bounds to include both markers
        val locations = listOfNotNull(ownerLocation, petsitterLocation)
        if (locations.isEmpty()) return

        if (locations.size == 1) {
            // Single location: center on it
            val location = locations[0]
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(com.mapbox.geojson.Point.fromLngLat(location.lng, location.lat))
                    @Suppress("MagicNumber")
                    .zoom(14.0)
                    .build()
            )
        } else {
            // Multiple locations: calculate bounds
            val lngs = locations.map { it.lng }
            val lats = locations.map { it.lat }
            val minLng = lngs.minOrNull() ?: 0.0
            val maxLng = lngs.maxOrNull() ?: 0.0
            val minLat = lats.minOrNull() ?: 0.0
            val maxLat = lats.maxOrNull() ?: 0.0

            val centerLng = (minLng + maxLng) / 2
            val centerLat = (minLat + maxLat) / 2

            // Calculate zoom level based on distance
            val latDiff = maxLat - minLat
            val lngDiff = maxLng - minLng
            val maxDiff = maxOf(latDiff, lngDiff)
            val zoom = when {
                maxDiff > 0.1 -> 11.0
                maxDiff > 0.05 -> 12.0
                maxDiff > 0.01 -> 13.0
                else -> 14.0
            }

            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(com.mapbox.geojson.Point.fromLngLat(centerLng, centerLat))
                    .zoom(zoom)
                    .build()
            )
        }
    }

    private fun updateInfoOverlay() {
        val overlay = infoOverlay ?: return
        val route = this.route ?: return
        val status = this.walkStatus ?: return

        val shouldShow = status == WalkStatus.GOING_TO_OWNER ||
                status == WalkStatus.ASSIGNED ||
                status == WalkStatus.RETURNING

        if (!shouldShow || petsitterLocation == null) {
            overlay.visibility = View.GONE
            return
        }

        val emoji = if (status == WalkStatus.RETURNING) "🏠" else "🚶"
        val distance = RouteInfo.formatDistance(route.distance)
        val duration = RouteInfo.formatDuration(route.duration)
        overlay.text = "$emoji $distance • $duration"
        overlay.visibility = View.VISIBLE
    }

    /**
     * Lifecycle methods - call from fragment/activity
     */
    fun onStart() {
        mapView?.onStart()
    }

    fun onStop() {
        mapView?.onStop()
    }

    fun onDestroy() {
        pointAnnotationManager?.deleteAll()
        mapView?.onDestroy()
    }
}
