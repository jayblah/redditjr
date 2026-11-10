package ml.docilealligator.infinityforreddit.adapters;

import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import java.util.function.IntSupplier;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.thing.SaveThing;
import ml.docilealligator.infinityforreddit.account.Account;
import ml.docilealligator.infinityforreddit.activities.BaseActivity;
import ml.docilealligator.infinityforreddit.comment.AnonymousSavedComments;
import ml.docilealligator.infinityforreddit.comment.Comment;
import ml.docilealligator.infinityforreddit.localsaved.LocalSaved;
import ml.docilealligator.infinityforreddit.utils.SavedCommentCacheNotifier;
import retrofit2.Retrofit;

/** The save button on a comment row, shared by the two comment adapters. */
final class CommentSaveToggle {

    private CommentSaveToggle() {
    }

    /**
     * Saves or unsaves {@code comment}: kept on the device when logged out, otherwise sent to Reddit
     * with the button and the comment optimistically flipped and put back if the request fails.
     * {@code currentPosition} says where the row is now, so a row that has been recycled for another
     * comment by the time the reply lands is left alone.
     */
    static void toggleSave(BaseActivity activity, String accountName, Retrofit oauthRetrofit,
                           @Nullable String accessToken, Comment comment, MaterialButton saveButton,
                           int position, IntSupplier currentPosition) {
        if (accountName.equals(Account.ANONYMOUS_ACCOUNT)) {
            // Logged out there is no account to save to, so the save is kept on the
            // device and nothing is sent.
            boolean saved = AnonymousSavedComments.toggle(activity, accountName, comment);
            saveButton.setIconResource(saved
                    ? R.drawable.ic_bookmark_grey_24dp : R.drawable.ic_bookmark_border_grey_24dp);
            Toast.makeText(activity, saved ? R.string.comment_saved_success
                    : R.string.comment_unsaved_success, Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.isSaved()) {
            comment.setSaved(false);
            SaveThing.unsaveThing(oauthRetrofit, accessToken, comment.getFullName(), new SaveThing.SaveThingListener() {
                @Override
                public void success() {
                    comment.setSaved(false);
                    LocalSaved.onUnsaved(activity, accountName, comment.getFullName());
                    SavedCommentCacheNotifier.onSavedCommentChanged();
                    if (currentPosition.getAsInt() == position) {
                        saveButton.setIconResource(R.drawable.ic_bookmark_border_grey_24dp);
                    }
                    Toast.makeText(activity, R.string.comment_unsaved_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void failed() {
                    comment.setSaved(true);
                    if (currentPosition.getAsInt() == position) {
                        saveButton.setIconResource(R.drawable.ic_bookmark_grey_24dp);
                    }
                    Toast.makeText(activity, R.string.comment_unsaved_failed, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            comment.setSaved(true);
            SaveThing.saveThing(oauthRetrofit, accessToken, comment.getFullName(), new SaveThing.SaveThingListener() {
                @Override
                public void success() {
                    comment.setSaved(true);
                    LocalSaved.onSaved(activity, oauthRetrofit, accessToken,
                            accountName, comment.getFullName(), comment.getLinkId());
                    SavedCommentCacheNotifier.onSavedCommentChanged();
                    if (currentPosition.getAsInt() == position) {
                        saveButton.setIconResource(R.drawable.ic_bookmark_grey_24dp);
                    }
                    Toast.makeText(activity, R.string.comment_saved_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void failed() {
                    comment.setSaved(false);
                    if (currentPosition.getAsInt() == position) {
                        saveButton.setIconResource(R.drawable.ic_bookmark_border_grey_24dp);
                    }
                    Toast.makeText(activity, R.string.comment_saved_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
