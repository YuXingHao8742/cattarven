package cat.tarven

import cat.tarven.data.model.CharacterCardV2
import cat.tarven.data.model.CharacterCardData
import com.google.gson.Gson
import org.junit.Test
import java.io.File
import org.junit.Assert.*

class CharacterParseTest {
    @Test
    fun testParse() {
        val gson = Gson()
        val jsonFile = File("C:/Users/13779/Desktop/cattarven/app/src/main/java/cat/tarven/data/model/潜意识修改·灵.json")
        val jsonString = jsonFile.readText()

        val cardV2 = gson.fromJson(jsonString, CharacterCardV2::class.java)
        println("Spec: ${cardV2.spec}")
        println("Name: ${cardV2.data.name}")
        
        val characterBook = cardV2.data.characterBook
        println("Has CharacterBook: ${characterBook != null}")
        
        if (characterBook != null) {
            println("Entries type: ${characterBook.entries?.javaClass?.name}")
            
            val worldInfo = characterBook.toWorldInfo()
            println("WorldInfo entries count: ${worldInfo.entries.size}")
            
            if (worldInfo.entries.isEmpty()) {
                // Manually try to see what's wrong
                val jsonStr = gson.toJson(characterBook.entries)
                println("Serialized entries: ${jsonStr.take(200)}")
                
                try {
                    val listType = object : com.google.gson.reflect.TypeToken<List<cat.tarven.data.model.RawWorldInfoEntry>>() {}.type
                    val list: List<cat.tarven.data.model.RawWorldInfoEntry> = gson.fromJson(jsonStr, listType)
                    println("List parsed manually: ${list.size}")
                } catch (e: Exception) {
                    println("List parse failed: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}
