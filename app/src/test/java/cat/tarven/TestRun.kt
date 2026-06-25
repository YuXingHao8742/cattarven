package cat.tarven

import org.junit.Test
import java.lang.reflect.Modifier

class TestRun {
    @Test
    fun testAnonymousClassVisibility() {
        val myObject = object {
            fun updateHeight(heightPx: Float) {
                println("Updated height: $heightPx")
            }
        }
        
        val clazz = myObject.javaClass
        val isPublic = java.lang.reflect.Modifier.isPublic(clazz.modifiers)
        val isPrivate = java.lang.reflect.Modifier.isPrivate(clazz.modifiers)
        val isProtected = java.lang.reflect.Modifier.isProtected(clazz.modifiers)
        
        println("--------------------------------------------------")
        println("Class name: ${clazz.name}")
        println("Is class public? $isPublic")
        println("Is class private? $isPrivate")
        println("Is class protected? $isProtected")
        
        val isPackagePrivate = !isPublic && !isPrivate && !isProtected
        println("Is class package-private (default visibility)? $isPackagePrivate")
        println("--------------------------------------------------")
    }
}
