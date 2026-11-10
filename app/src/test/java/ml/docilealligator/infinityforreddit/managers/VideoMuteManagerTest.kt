package ml.docilealligator.infinityforreddit.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The ranking of the mute settings: a remembered tap first, then Mute Videos or Mute Autoplaying as the start. */
@RunWith(RobolectricTestRunner::class)
class VideoMuteManagerTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var manager: VideoMuteManager

    @Before
    fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("mute_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        manager = VideoMuteManager(prefs)
    }

    private fun set(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).commit()

    @Test
    fun `autoplay starts muted by default and follows its setting`() {
        assertTrue(manager.startsMuted())
        set(SharedPreferencesUtils.MUTE_AUTOPLAYING_VIDEOS, false)
        assertFalse(manager.startsMuted())
    }

    @Test
    fun `Mute Videos starts autoplay muted whatever Mute Autoplaying says`() {
        set(SharedPreferencesUtils.MUTE_AUTOPLAYING_VIDEOS, false)
        set(SharedPreferencesUtils.MUTE_VIDEO, true)
        assertTrue(manager.startsMuted())
    }

    @Test
    fun `a remembered unmute beats Mute Videos and Mute Autoplaying`() {
        set(SharedPreferencesUtils.MUTE_VIDEO, true)
        set(SharedPreferencesUtils.MUTE_AUTOPLAYING_VIDEOS, true)
        manager.rememberMuteOption = true
        assertTrue("before any tap the settings decide", manager.startsMuted())
        manager.isMuted = false
        assertFalse("returning to a video keeps what the reader chose", manager.startsMuted())
    }

    @Test
    fun `a remembered choice replaces the Mute Autoplaying setting without rewriting it`() {
        manager.rememberMuteOption = true
        manager.isMuted = false
        assertFalse(manager.startsMuted())
        assertTrue(prefs.getBoolean(SharedPreferencesUtils.MUTE_AUTOPLAYING_VIDEOS, true))
    }

    @Test
    fun `without Remember Mute a tap changes nothing stored`() {
        manager.isMuted = false
        assertNull(manager.getMasterMutingOption())
        assertTrue(manager.startsMuted())
        assertFalse(prefs.contains(SharedPreferencesUtils.REMEMBERED_MUTE_STATE))
    }

    @Test
    fun `changing the setting or switching Remember Mute on starts from the setting again`() {
        manager.rememberMuteOption = true
        manager.isMuted = false
        manager.forgetRememberedMute()
        assertTrue(manager.isMuted)

        manager.isMuted = false
        manager.rememberMuteOption = true
        assertEquals(true, manager.getMasterMutingOption())
    }
}
