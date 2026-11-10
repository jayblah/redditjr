package ml.docilealligator.infinityforreddit.fragments;

import android.app.job.JobInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;
import ml.docilealligator.infinityforreddit.AppComponent;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.activities.ViewImgurMediaActivity;
import ml.docilealligator.infinityforreddit.databinding.FragmentViewImgurVideoBinding;
import ml.docilealligator.infinityforreddit.post.ImgurMedia;
import ml.docilealligator.infinityforreddit.services.DownloadMediaService;

public class ViewImgurVideoFragment extends ViewVideoFragmentBase {

    public static final String EXTRA_IMGUR_VIDEO = "EIV";

    private ImgurMedia imgurMedia;

    public ViewImgurVideoFragment() {
        // Required empty public constructor
    }

    private ViewImgurMediaActivity imgurActivity() {
        return (ViewImgurMediaActivity) activity;
    }

    @Override
    protected ViewVideoFragmentBindingAdapter inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return new ViewImgurVideoFragmentBindingAdapter(FragmentViewImgurVideoBinding.inflate(inflater, container, false));
    }

    @Override
    protected void inject(@NonNull AppComponent appComponent) {
        appComponent.inject(this);
    }

    @Override
    protected void readVideoArguments(@NonNull Bundle arguments) {
        imgurMedia = Objects.requireNonNull(arguments.getParcelable(EXTRA_IMGUR_VIDEO));
    }

    @Override
    protected String getVideoUrl() {
        return imgurMedia.getLink();
    }

    @Nullable
    @Override
    protected Typeface getTypeface() {
        return imgurActivity().typeface;
    }

    @Override
    protected void setActionBarHidden(boolean hidden) {
        imgurActivity().setActionBarHidden(hidden);
    }

    @Override
    protected int getTitleFormatRes() {
        return R.string.view_imgur_media_activity_video_label;
    }

    @Override
    protected void setUpDownloadAll(@NonNull ImageView downloadAllButton) {
        downloadAllButton.setVisibility(View.VISIBLE);
        // This screen also shows imgchest and imgbb albums, so the button describes itself
        // host-neutrally there rather than naming the wrong site.
        downloadAllButton.setContentDescription(
                getString(imgurActivity().isImageHostAlbum() ? R.string.action_download_all_album_media
                        : R.string.action_download_all_imgur_album_media));
        downloadAllButton.setOnClickListener(view -> imgurActivity().downloadAllImgurAlbumMedia());
    }

    @Override
    protected int getMenuRes() {
        return R.menu.view_imgur_video_fragment;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_download_view_imgur_video_fragment) {
            onDownloadMenuItem();
            return true;
        } else if (item.getItemId() == R.id.action_playback_speed_view_imgur_video_fragment) {
            onPlaybackSpeedMenuItem();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isNsfw(@NonNull Bundle arguments) {
        return arguments.getBoolean(ViewImgurMediaActivity.EXTRA_IS_NSFW);
    }

    @Nullable
    @Override
    protected JobInfo createDownloadJob(@NonNull Bundle arguments, boolean share) {
        String subredditName = arguments.getString(ViewImgurMediaActivity.EXTRA_SUBREDDIT_NAME);
        boolean isNsfw = arguments.getBoolean(ViewImgurMediaActivity.EXTRA_IS_NSFW);
        String title = arguments.getString(ViewImgurMediaActivity.EXTRA_POST_TITLE_KEY);
        return DownloadMediaService.constructJobInfo(activity, 5000000, imgurMedia, subredditName, isNsfw, title, share);
    }
}
