package ml.docilealligator.infinityforreddit.fragments;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.ui.PlayerView;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;
import ml.docilealligator.infinityforreddit.R;

abstract class ViewVideoFragmentBindingAdapter {
    private final View root;
    private final PlayerView playerView;
    private LinearLayout controller;
    private MaterialButton muteButton;
    private BottomAppBar bottomAppBar;
    private TextView titleTextView;
    private ImageView downloadButton;
    private ImageView shareButton;
    private ImageView playbackSpeedButton;
    private ImageView rotateLeftButton;
    private ImageView rotateRightButton;
    private ImageView downloadAllButton;

    ViewVideoFragmentBindingAdapter(View root, PlayerView playerView) {
        this.root = root;
        this.playerView = playerView;
        controller = root.findViewById(R.id.linear_layout_exo_playback_control_view);
        muteButton = root.findViewById(R.id.mute_exo_playback_control_view);
        bottomAppBar = root.findViewById(R.id.bottom_navigation_exo_playback_control_view);
        titleTextView = root.findViewById(R.id.title_text_view_exo_playback_control_view);
        downloadButton = root.findViewById(R.id.download_image_view_exo_playback_control_view);
        shareButton = root.findViewById(R.id.share_image_view_exo_playback_control_view);
        playbackSpeedButton = root.findViewById(R.id.playback_speed_image_view_exo_playback_control_view);
        rotateLeftButton = root.findViewById(R.id.rotate_left_image_view_exo_playback_control_view);
        rotateRightButton = root.findViewById(R.id.rotate_right_image_view_exo_playback_control_view);
        downloadAllButton = root.findViewById(R.id.download_all_image_view_exo_playback_control_view);
    }

    View getRoot() {
        return root;
    }

    PlayerView getPlayerView() {
        return playerView;
    }

    LinearLayout getController() {
        return controller;
    }

    MaterialButton getMuteButton() {
        return muteButton;
    }

    BottomAppBar getBottomAppBar() {
        return bottomAppBar;
    }

    TextView getTitleTextView() {
        return titleTextView;
    }

    ImageView getDownloadButton() {
        return downloadButton;
    }

    ImageView getShareButton() {
        return shareButton;
    }

    ImageView getPlaybackSpeedButton() {
        return playbackSpeedButton;
    }

    ImageView getRotateLeftButton() {
        return rotateLeftButton;
    }

    ImageView getRotateRightButton() {
        return rotateRightButton;
    }

    ImageView getDownloadAllButton() {
        return downloadAllButton;
    }
}
