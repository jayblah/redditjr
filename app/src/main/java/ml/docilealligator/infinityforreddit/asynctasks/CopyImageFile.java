package ml.docilealligator.infinityforreddit.asynctasks;

import android.os.Handler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;

/**
 * Copies an image's original bytes into the cache so it can be shared.
 *
 * This replaces decoding the image to a Bitmap and compressing it again, which turned every shared
 * image into a JPEG whatever it was (a PNG lost its transparency, a WebP its format) and recompressed
 * photos that were already compressed, so the receiving app got a worse picture than the one shown.
 */
public class CopyImageFile {

    public static void copyImageFile(Executor executor, Handler handler, File source,
                                     String cacheDirPath, String fileName,
                                     CopyImageFileListener listener) {
        executor.execute(() -> {
            File imageFile = new File(cacheDirPath, fileName);
            try {
                // Reported successful only once the output is closed, so a full disk surfaces
                // from close() instead of handing the listener a truncated file.
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = new FileOutputStream(imageFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                handler.post(() -> listener.saveSuccess(imageFile));
            } catch (IOException e) {
                handler.post(listener::saveFailed);
            }
        });
    }

    public interface CopyImageFileListener {
        void saveSuccess(File imageFile);
        void saveFailed();
    }
}
