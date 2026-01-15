package www.com.petsitternow_app.util

import android.content.Context
import org.json.JSONObject

object DogBreedsLoader {
    
    private var cachedBreeds: List<String>? = null
    
    fun getBreeds(context: Context): List<String> {
        cachedBreeds?.let { return it }
        
        val json = context.assets.open("dog_breeds.json")
            .bufferedReader()
            .use { it.readText() }
        
        val jsonObject = JSONObject(json)
        val breedsArray = jsonObject.getJSONArray("breeds")
        
        val breeds = mutableListOf<String>()
        for (i in 0 until breedsArray.length()) {
            breeds.add(breedsArray.getString(i))
        }
        
        cachedBreeds = breeds
        return breeds
    }
}
