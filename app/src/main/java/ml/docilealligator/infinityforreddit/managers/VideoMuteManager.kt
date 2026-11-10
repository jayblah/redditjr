package ml.docilealligator.infinityforreddit.managers

import android.content.SharedPreferences
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils

/**
 * Decides whether an inline (feed or post detail) video starts silent. The settings rank:
 *
 * 1. **Mute NSFW Videos** on an NSFW post always wins ([forcesMute]). Nothing the reader remembers
 *    lifts it; they can still unmute that one video with its own button.
 * 2. **Remember Mute**, when on, lets the reader's last tap on any mute button decide.
 * 3. Otherwise the starting state is muted if **Mute Videos** or **Mute Autoplaying Videos** says
 *    so. With Remember Mute on they also set the state before the first tap.
 *
 * Mute Videos is a starting state, not a lock: it would otherwise silence a video the reader had
 * just unmuted the next time it came back on screen, which defeats Remember Mute.
 *
 * Holds the preferences rather than values read out of them once: these settings are per-account, so
 * a value cached at construction would keep the previous account's choice for the life of the
 * process.
 */
class VideoMuteManager(private val sharedPreferences: SharedPreferences) {

    /** Where an unremembered video starts: "Mute Autoplaying Videos" or "Mute Videos". */
    private val mutesAutoplayByDefault: Boolean
        get() = sharedPreferences.getBoolean(SharedPreferencesUtils.MUTE_AUTOPLAYING_VIDEOS, true) ||
            sharedPreferences.getBoolean(SharedPreferencesUtils.MUTE_VIDEO, false)

    /**
     * The reader's last mute choice. Kept under a key of its own: it used to be written over the
     * "Mute Autoplaying Videos" setting, so a tap on a mute button silently flipped a switch in
     * Settings > Video. Until there has been a tap it is the starting state.
     */
    var isMuted: Boolean
        get() = sharedPreferences.getBoolean(
            SharedPreferencesUtils.REMEMBERED_MUTE_STATE, mutesAutoplayByDefault)
        set(value) {
            // The post detail's mute button calls this on every tap, unlike the feed, which asks
            // first. Nothing is lost by skipping it with the option off: every read of isMuted is
            // already behind the same condition.
            if (!rememberMuteOption) {
                return
            }
            sharedPreferences.edit()
                .putBoolean(SharedPreferencesUtils.REMEMBERED_MUTE_STATE, value)
                .apply()
        }

    var rememberMuteOption: Boolean
        get() = sharedPreferences.getBoolean(
            SharedPreferencesUtils.REMEMBER_MUTING_OPTION_IN_POST_FEED, false)
        set(value) {
            // Turning it on starts from the setting, not from whatever was tapped long ago.
            sharedPreferences.edit()
                .putBoolean(SharedPreferencesUtils.REMEMBER_MUTING_OPTION_IN_POST_FEED, value)
                .remove(SharedPreferencesUtils.REMEMBERED_MUTE_STATE)
                .apply()
        }

    /** Drops the remembered choice, so [isMuted] follows "Mute Autoplaying Videos" again. */
    fun forgetRememberedMute() {
        sharedPreferences.edit().remove(SharedPreferencesUtils.REMEMBERED_MUTE_STATE).apply()
    }

    fun getMasterMutingOption(): Boolean? = if (rememberMuteOption) isMuted else null

    fun forcesMute(isNsfw: Boolean): Boolean =
        isNsfw && sharedPreferences.getBoolean(SharedPreferencesUtils.MUTE_NSFW_VIDEO, false)

    fun startsMuted(isNsfw: Boolean): Boolean =
        forcesMute(isNsfw) || (getMasterMutingOption() ?: mutesAutoplayByDefault)
}
