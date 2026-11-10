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
import ml.docilealligator.infinityforreddit.activities.ViewRedditGalleryActivity;
import ml.docilealligator.infinityforreddit.databinding.FragmentViewRedditGalleryVideoBinding;
import ml.docilealligator.infinityforreddit.post.Post;
import ml.docilealligator.infinityforreddit.services.DownloadMediaService;

public class ViewRedditGalleryVideoFragment extends ViewVideoFragmentBase {

    public static final String EXTRA_REDDIT_GALLERY_VIDEO = "EIV";
    public static final String EXTRA_SUBREDDIT_NAME = "ESN";
    public static final String EXTRA_IS_NSFW = "EIN";

    private Post.Gallery galleryVideo;

    public ViewRedditGalleryVideoFragment() {
        // Required empty public constructor
    }

    private ViewRedditGalleryActivity galleryActivity() {
        return (ViewRedditGalleryActivity) activity;
    }

    @Override
    protected ViewVideoFragmentBindingAdapter inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return new ViewRedditGalleryVideoFragmentBindingAdapter(FragmentViewRedditGalleryVideoBinding.inflate(inflater, container, false));
    }

    @Override
    protected void inject(@NonNull AppComponent appComponent) {
        appComponent.inject(this);
    }

    @Override
    protected void readVideoArguments(@NonNull Bundle arguments) {
        galleryVideo = Objects.requireNonNull(arguments.getParcelable(EXTRA_REDDIT_GALLERY_VIDEO));
    }

    @Override
    protected String getVideoUrl() {
        return galleryVideo.url;
    }

    @Nullable
    @Override
    protected Typeface getTypeface() {
        return galleryActivity().typeface;
    }

    @Override
    protected void setActionBarHidden(boolean hidden) {
        galleryActivity().setActionBarHidden(hidden);
    }

    @Override
    protected int getTitleFormatRes() {
        return R.string.view_reddit_gallery_activity_video_label;
    }

    @Override
    protected void setUpDownloadAll(@NonNull ImageView downloadAllButton) {
        downloadAllButton.setVisibility(View.VISIBLE);
        downloadAllButton.setOnClickListener(view -> galleryActivity().downloadAllGalleryMedia());
    }

    @Override
    protected int getMenuRes() {
        return R.menu.view_reddit_gallery_video_fragment;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_download_view_reddit_gallery_video_fragment) {
            onDownloadMenuItem();
            return true;
        } else if (item.getItemId() == R.id.action_playback_speed_view_reddit_gallery_video_fragment) {
            onPlaybackSpeedMenuItem();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isNsfw(@NonNull Bundle arguments) {
        return arguments.getBoolean(EXTRA_IS_NSFW, false);
    }

    @Nullable
    @Override
    protected JobInfo createDownloadJob(@NonNull Bundle arguments, boolean share) {
        Post parentPost = galleryActivity().getPost();
        if (parentPost == null) {
            return null;
        }
        int galleryIndex = arguments.getInt(EXTRA_INDEX, 0);
        // The overload that takes the Post and the index builds the file name itself.
        return DownloadMediaService.constructJobInfo(activity, 5000000, parentPost, galleryIndex, share);
    }
}
