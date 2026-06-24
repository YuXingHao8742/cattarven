package cat.tarven

import cat.tarven.data.model.CharacterCardV2
import com.google.gson.Gson

fun main() {
    val gson = Gson()
    val jsonString = """
        {
          "spec": "chara_card_v2",
          "spec_version": "2.0",
          "data": {
            "name": "Demo",
            "character_book": {
              "entries": [
                {
                  "keys": ["demo"],
                  "content": "demo content",
                  "enabled": true
                }
              ]
            }
          }
        }
    """.trimIndent()

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
