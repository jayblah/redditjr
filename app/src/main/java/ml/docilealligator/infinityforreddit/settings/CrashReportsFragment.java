package ml.docilealligator.infinityforreddit.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.crazylegend.crashyreporter.CrashyReporter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import ml.docilealligator.infinityforreddit.BuildConfig;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.activities.LinkResolverActivity;
import ml.docilealligator.infinityforreddit.activities.SettingsActivity;
import ml.docilealligator.infinityforreddit.adapters.CrashReportsRecyclerViewAdapter;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.utils.Utils;

public class CrashReportsFragment extends Fragment {

    @Inject
    CustomThemeWrapper mCustomThemeWrapper;
    private SettingsActivity mActivity;

    public CrashReportsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_crash_reports, container, false);

        if (mActivity.isImmersiveInterfaceRespectForcedEdgeToEdge()) {
            ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                    Insets allInsets = Utils.getInsets(insets, false, mActivity.isForcedImmersiveInterface());
                    recyclerView.setPadding(allInsets.left, 0, allInsets.right, allInsets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                }
            });
        }

        ((Infinity) mActivity.getApplication()).getAppComponent().inject(this);

        setHasOptionsMenu(true);

        List<String> crashReports = CrashyReporter.INSTANCE.getLogsAsStrings();
        recyclerView.setAdapter(new CrashReportsRecyclerViewAdapter(mActivity,
                crashReports != null ? crashReports : new ArrayList<>()));

        recyclerView.setBackgroundColor(mActivity.customThemeWrapper.getBackgroundColor());

        return recyclerView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.crash_reports_fragment, menu);
        applyMenuItemTheme(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_logs_crash_reports_fragment) {
            CrashyReporter.INSTANCE.purgeLogs();
            Toast.makeText(mActivity, R.string.crash_reports_deleted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_export_logs_crash_reports_fragment) {
            return createGithubIssueWithLogs();
        }
        return false;
    }

    /**
     * Fetch the logs from CrashyReporter and open browser to create GitHub issue page.
     * Issue will have logs, device model, app version, and Android version prefilled.
     * <p>
     * Each parameter name below is a field {@code id} in
     * {@code .github/ISSUE_TEMPLATE/bug_report.yml} — that per-field prefill is what an issue
     * form supports, and it is why {@code template} can be sent here at all. (A Markdown template
     * would take a whole-body {@code body} parameter instead, which GitHub treats as mutually
     * exclusive with {@code template}.) The form declares its own label, but a URL carrying query
     * parameters does not pick up template defaults, so {@code labels} is sent explicitly.
     * @return if successful
     */
    // URLEncoder.encode(String, Charset) needs API 33; minSdk is 24, so the String overload stays.
    @SuppressWarnings("JdkObsolete")
    private boolean createGithubIssueWithLogs() {
        Intent intent = new Intent(getContext(), LinkResolverActivity.class);
        String logs, device, version, androidVersion, title, description;
        try {
            List<String> logLines = CrashyReporter.INSTANCE.getLogsAsStrings();
            if (logLines == null) {
                return false;
            }
            String rawLogs = String.join("\n", logLines);
            // Limit size to avoid `414 URI Too Long`. The title and description below take a
            // few hundred characters of the same URL, so the logs give up that much.
            logs = URLEncoder.encode("```\n" + (rawLogs.isEmpty()
                    ? "No logs found." : rawLogs.substring(0, Math.min(6000, rawLogs.length()))) + "\n```", "UTF-8");
            title = URLEncoder.encode(crashTitle(logLines), "UTF-8");
            description = URLEncoder.encode("The app crashed. The logs and device details are "
                    + "filled in below.\n\nWhat I was doing when it crashed:\n", "UTF-8");
            device = URLEncoder.encode(Build.MANUFACTURER + " " + Build.MODEL, "UTF-8");
            version = URLEncoder.encode(BuildConfig.VERSION_NAME, "UTF-8");
            androidVersion = URLEncoder.encode(Build.VERSION.RELEASE, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        Uri githubIssueUri = Uri.parse(String.format(
                "https://github.com/jayblah/redditjr/issues/new?template=bug_report.yml&labels=bug"
                        + "&title=%s&description=%s"
                        + "&device=%s&android_version=%s&version=%s&logs=%s",
                title, description, device, androidVersion, version, logs));
        intent.setData(githubIssueUri);
        startActivity(intent);
        return true;
    }

    /**
     * An issue title from the newest crash report: its exception line and, when there is one, the
     * first frame, e.g. {@code Crash: NullPointerException at ExoPlayerViewHelper.getPlayer}.
     */
    private static String crashTitle(List<String> logLines) {
        String fallback = "Crash report";
        if (logLines.isEmpty()) {
            return fallback;
        }
        String[] lines = logLines.get(0).split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("Stacktrace") || lines[i].contains("END")) {
                continue;
            }
            if (i + 1 >= lines.length || lines[i + 1].trim().isEmpty()) {
                return fallback;
            }
            // "java.lang.NullPointerException: message..." -> "NullPointerException"
            String exception = lines[i + 1].trim().split(":")[0];
            exception = exception.substring(exception.lastIndexOf('.') + 1);
            String title = "Crash: " + exception;
            if (i + 2 < lines.length && lines[i + 2].trim().startsWith("at ")) {
                // "at pkg.Class.method(File.java:12)" -> "Class.method"
                String frame = lines[i + 2].trim().substring(3).split("\\(")[0];
                String[] parts = frame.split("\\.");
                if (parts.length >= 2) {
                    title += " at " + parts[parts.length - 2] + "." + parts[parts.length - 1];
                }
            }
            return title;
        }
        return fallback;
    }

    @SuppressLint("RestrictedApi")
    protected boolean applyMenuItemTheme(Menu menu) {
        if (mCustomThemeWrapper != null) {
            int size = Math.min(menu.size(), 2);
            for (int i = 0; i < size; i++) {
                MenuItem item = menu.getItem(i);
                if (((MenuItemImpl) item).requestsActionButton()) {
                    MenuItemCompat.setIconTintList(item, ColorStateList
                            .valueOf(mCustomThemeWrapper.getToolbarPrimaryTextAndIconColor()));
                }
                Utils.setTitleWithCustomFontToMenuItem(mActivity.typeface, item, null);
            }
        }
        return true;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (SettingsActivity) context;
    }
}