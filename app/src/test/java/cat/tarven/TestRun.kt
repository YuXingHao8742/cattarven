package cat.tarven

import cat.tarven.data.model.CharacterCardV2
import com.google.gson.Gson
import java.io.File

fun main() {
    val gson = Gson()
    val jsonFile = File("C:/Users/13779/Desktop/cattarven/app/src/main/java/cat/tarven/data/model/潜意识修改·灵.json")
    val jsonString = jsonFile.readText()

    val cardV2 = gson.fromJson(jsonString, CharacterCardV2::class.java)
    val characterBook = cardV2.data.characterBook
    
    if (characterBook == null) {
        println("characterBook is NULL")
        return
    }
    
    val entries = characterBook.entries
    println("Entries class: ${entries?.javaClass?.name}")
    
    val worldInfo = characterBook.toWorldInfo()
    println("WorldInfo entries size: ${worldInfo.entries.size}")
}
