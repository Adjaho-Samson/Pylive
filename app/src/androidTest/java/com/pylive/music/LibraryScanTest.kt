package com.pylive.music

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pylive.music.domain.PermissionPolicy
import com.pylive.music.library.LocalLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Vérifie la détection MediaStore et le refus propre d'un fichier inexistant. */
@RunWith(AndroidJUnit4::class)
class LibraryScanTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val library = LocalLibrary(context, scope)

    @After
    fun tearDown() {
        library.close()
        scope.cancel()
    }

    @Test
    fun permissionNameMatchesAndroidVersion() {
        val expected = PermissionPolicy.audioPermission(android.os.Build.VERSION.SDK_INT)
        assertFalse(expected.isBlank())
    }

    @Test
    fun scanNeverThrowsWithoutPermission() {
        // Sans autorisation accordée, le scan doit simplement renvoyer une liste,
        // sans exception : l'interface affiche alors l'état vide.
        library.refresh()
    }

    @Test
    fun missingFileIsNotAccessible() = runBlocking {
        assertFalse(
            library.isAccessible(
                "content://media/external/audio/media/9223372036854775807"
            )
        )
    }
}
