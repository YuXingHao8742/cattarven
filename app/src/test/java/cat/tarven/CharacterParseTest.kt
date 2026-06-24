package cat.tarven

import cat.tarven.data.model.CharacterCardV2
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class CharacterParseTest {
    @Test
    fun testParseV2CharacterCardAndWorldInfo() {
        val gson = Gson()
        val jsonString = """
            {
              "spec": "chara_card_v2",
              "spec_version": "2.0",
              "data": {
                "name": "Test Character",
                "description": "desc",
                "character_book": {
                  "entries": {
                    "0": {
                      "keys": ["forest", "night"],
                      "content": "A hidden forest lore",
                      "enabled": true
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val cardV2 = gson.fromJson(jsonString, CharacterCardV2::class.java)
        assertEquals("chara_card_v2", cardV2.spec)
        assertEquals("Test Character", cardV2.data.name)

        val worldInfo = cardV2.data.characterBook?.toWorldInfo()
        assertNotNull(worldInfo)
        assertEquals(1, worldInfo!!.entries.size)
        assertEquals(listOf("forest", "night"), worldInfo.entries.first().keys)
        assertEquals("A hidden forest lore", worldInfo.entries.first().content)
    }
}
