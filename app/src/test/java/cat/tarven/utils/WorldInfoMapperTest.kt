package cat.tarven.utils

import cat.tarven.data.model.RawWorldInfoEntry
import cat.tarven.utils.WorldInfoMapper.isDisabled
import cat.tarven.utils.WorldInfoMapper.toWorldInfoEntry
import org.junit.Assert.*
import org.junit.Test

class WorldInfoMapperTest {

    @Test
    fun `toWorldInfoEntry maps V1 keys correctly`() {
        val raw = RawWorldInfoEntry(
            key = listOf("key1", "key2"),
            content = "test content"
        )
        val result = raw.toWorldInfoEntry()
        assertEquals(listOf("key1", "key2"), result.keys)
        assertEquals("test content", result.content)
    }

    @Test
    fun `toWorldInfoEntry maps V2 keys correctly`() {
        val raw = RawWorldInfoEntry(
            keys = listOf("v2_key1"),
            content = "test content v2"
        )
        val result = raw.toWorldInfoEntry()
        assertEquals(listOf("v2_key1"), result.keys)
        assertEquals("test content v2", result.content)
    }

    @Test
    fun `toWorldInfoEntry handles fallback default values`() {
        val raw = RawWorldInfoEntry()
        val result = raw.toWorldInfoEntry()
        assertEquals(emptyList<String>(), result.keys)
        assertEquals("", result.content)
        assertEquals(0, result.insertionOrder)
        assertEquals(1, result.position)
        assertEquals(4, result.depth)
        assertEquals("system", result.role)
    }

    @Test
    fun `isDisabled returns true when disable is true`() {
        val raw = RawWorldInfoEntry(disable = true)
        assertTrue(raw.isDisabled())
    }

    @Test
    fun `isDisabled returns true when enabled is false`() {
        val raw = RawWorldInfoEntry(enabled = false)
        assertTrue(raw.isDisabled())
    }

    @Test
    fun `isDisabled returns false when enabled is true and disable is null`() {
        val raw = RawWorldInfoEntry(enabled = true)
        assertFalse(raw.isDisabled())
    }

    @Test
    fun `isDisabled prioritizes disable over enabled`() {
        val raw = RawWorldInfoEntry(disable = true, enabled = true)
        assertTrue(raw.isDisabled())
    }
}
