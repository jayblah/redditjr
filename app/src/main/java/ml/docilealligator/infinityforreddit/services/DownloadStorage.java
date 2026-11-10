package ml.docilealligator.infinityforreddit.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.UriPermission;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import ml.docilealligator.infinityforreddit.utils.MediaFileNameUtils;

/** Where the download services put a file, shared by the media and the Reddit video service. */
final class DownloadStorage {

    private DownloadStorage() {
    }

    /**
     * Returns a destination under {@code topDir} (for example {@link Environment#DIRECTORY_MOVIES}),
     * used when the user's chosen folder is unavailable. On Android Q+ this is a MediaStore
     * {@code RELATIVE_PATH}; on older versions it is an absolute file path (with parent directories
     * created and name collisions resolved).
     */
    static String getDefaultDownloadPath(String topDir, @Nullable String subredditName, String fileName) {
        String subFolder = (subredditName != null && !subredditName.isEmpty()) ? subredditName : null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // MediaStore resolves name collisions on its own for RELATIVE_PATH destinations.
            return subFolder == null ? topDir : topDir + File.separator + subFolder;
        }

        File baseDir = Environment.getExternalStoragePublicDirectory(topDir);
        if (subFolder != null) {
            baseDir = new File(baseDir, subFolder);
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        File outFile = new File(baseDir, fileName);
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);
        int num = 1;
        while (outFile.exists()) {
            outFile = new File(baseDir, baseName + " (" + num + ")" + extension);
            num++;
        }
        return outFile.getAbsolutePath();
    }

    /**
     * Returns true if the app still holds a persisted write grant for the given tree URI. SAF
     * permissions can be lost (revoked by the user, or originally granted to a different app build),
     * so checking before use lets us fall back cleanly instead of hitting a SecurityException.
     */
    static boolean hasPersistedWritePermission(ContentResolver contentResolver, Uri treeUri, String logTag) {
        try {
            for (UriPermission permission : contentResolver.getPersistedUriPermissions()) {
                if (permission.isWritePermission() && permission.getUri().equals(treeUri)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(logTag, "Failed to read persisted URI permissions: " + e.getMessage());
        }
        return false;
    }

    /**
     * Inserts a MediaStore record for {@code contentValues}. A destination that will not take the
     * name's non-ASCII characters gets an ASCII one rather than no file at all.
     */
    static Uri insertMediaStoreRecord(ContentResolver contentResolver, Uri contentUri,
                                      ContentValues contentValues, String destinationFileName) throws IOException {
        Uri uri = contentResolver.insert(contentUri, contentValues);

        if (uri == null) {
            String asciiName = MediaFileNameUtils.toAsciiFilename(destinationFileName);
            if (!asciiName.equals(destinationFileName)) {
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, asciiName);
                uri = contentResolver.insert(contentUri, contentValues);
            }
        }

        if (uri == null) {
            throw new IOException("Failed to create new MediaStore record.");
        }
        return uri;
    }
}
