package www.com.petsitternow_app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import www.com.petsitternow_app.domain.model.RouteInfo
import www.com.petsitternow_app.domain.model.WalkLocation
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching walking directions from Mapbox Directions API.
 */
@Singleton
class MapboxDirectionsService @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://api.mapbox.com/directions/v5/mapbox/walking"
    }

    /**
     * Get walking route between two locations.
     * @param from Starting location
     * @param to Destination location
     * @param accessToken Mapbox access token
     * @return RouteInfo with coordinates, distance, and duration, or null if error
     */
    suspend fun getRoute(
        from: WalkLocation,
        to: WalkLocation,
        accessToken: String
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            // Build URL: {lng},{lat};{lng},{lat}?geometries=geojson&access_token=...
            val urlString = "$BASE_URL/${from.lng},${from.lat};${to.lng},${to.lat}?geometries=geojson&access_token=$accessToken"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Mapbox API error: $responseCode")
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            // Check if route exists
            if (json.getString("code") != "Ok" || !json.has("routes") || json.getJSONArray("routes").length() == 0) {
                return@withContext Result.failure(
                    Exception("Impossible de calculer l'itinéraire")
                )
            }

            val routes = json.getJSONArray("routes")
            val route = routes.getJSONObject(0)
            val geometry = route.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            // Convert coordinates from [lng, lat] to List<Pair<Double, Double>>
            val coordinateList = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                val lng = coord.getDouble(0)
                val lat = coord.getDouble(1)
                coordinateList.add(Pair(lng, lat))
            }

            val distance = route.getDouble("distance") // in meters
            val duration = route.getDouble("duration") // in seconds

            Result.success(
                RouteInfo(
                    coordinates = coordinateList,
                    distance = distance,
                    duration = duration
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
