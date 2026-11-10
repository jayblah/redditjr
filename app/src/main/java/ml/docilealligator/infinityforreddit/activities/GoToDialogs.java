package ml.docilealligator.infinityforreddit.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.account.Account;
import ml.docilealligator.infinityforreddit.account.AccountScope;
import ml.docilealligator.infinityforreddit.adapters.SubredditAutocompleteRecyclerViewAdapter;
import ml.docilealligator.infinityforreddit.apis.RedditAPI;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.subreddit.ParseSubredditData;
import ml.docilealligator.infinityforreddit.subreddit.SubredditData;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import ml.docilealligator.infinityforreddit.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * The "Go to subreddit" and "Go to user" dialogs, which the main screen and the search results both
 * offer. The subreddit one asks Reddit for suggestions as the reader types, and keeps the request in
 * flight and the pending keystroke here, for the life of the dialog.
 */
final class GoToDialogs {

    private final BaseActivity activity;
    private final ViewGroup parent;
    private final Retrofit retrofit;
    private final Retrofit oauthRetrofit;
    private final Executor executor;
    private final CustomThemeWrapper customThemeWrapper;
    private final SharedPreferences nsfwAndSpoilerSharedPreferences;
    @Nullable
    private Runnable autoCompleteRunnable;
    @Nullable
    private Call<String> subredditAutocompleteCall;

    private GoToDialogs(BaseActivity activity, ViewGroup parent, Retrofit retrofit,
                        Retrofit oauthRetrofit, Executor executor,
                        CustomThemeWrapper customThemeWrapper,
                        SharedPreferences nsfwAndSpoilerSharedPreferences) {
        this.activity = activity;
        this.parent = parent;
        this.retrofit = retrofit;
        this.oauthRetrofit = oauthRetrofit;
        this.executor = executor;
        this.customThemeWrapper = customThemeWrapper;
        this.nsfwAndSpoilerSharedPreferences = nsfwAndSpoilerSharedPreferences;
    }

    /** Shows the "Go to subreddit" dialog, with autocomplete from Reddit as the reader types. */
    static void goToSubreddit(BaseActivity activity, ViewGroup parent, Retrofit retrofit, Retrofit oauthRetrofit,
                              Executor executor, CustomThemeWrapper customThemeWrapper,
                              SharedPreferences nsfwAndSpoilerSharedPreferences) {
        new GoToDialogs(activity, parent, retrofit, oauthRetrofit, executor, customThemeWrapper,
                nsfwAndSpoilerSharedPreferences).showGoToSubreddit();
    }

    private void showGoToSubreddit() {
        View rootView = activity.getLayoutInflater().inflate(R.layout.dialog_go_to_thing_edit_text,
                parent, false);
        TextInputEditText thingEditText = rootView.findViewById(R.id.text_input_edit_text_go_to_thing_edit_text);
        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view_go_to_thing_edit_text);
        SubredditAutocompleteRecyclerViewAdapter adapter = new SubredditAutocompleteRecyclerViewAdapter(
                activity, customThemeWrapper, subredditData -> {
            Utils.hideKeyboard(activity);
            Intent intent = new Intent(activity, ViewSubredditDetailActivity.class);
            intent.putExtra(ViewSubredditDetailActivity.EXTRA_SUBREDDIT_NAME_KEY, subredditData.getName());
            activity.startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        thingEditText.requestFocus();
        Utils.showKeyboard(activity, new Handler(), thingEditText);
        thingEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                Utils.hideKeyboard(activity);
                Intent subredditIntent = new Intent(activity, ViewSubredditDetailActivity.class);
                subredditIntent.putExtra(ViewSubredditDetailActivity.EXTRA_SUBREDDIT_NAME_KEY, Objects.requireNonNull(thingEditText.getText()).toString());
                activity.startActivity(subredditIntent);
                return true;
            }
            return false;
        });

        boolean nsfw = nsfwAndSpoilerSharedPreferences.getBoolean(AccountScope.key(activity.accountName, SharedPreferencesUtils.NSFW_BASE), false);
        Handler handler = new Handler();
        thingEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (subredditAutocompleteCall != null && subredditAutocompleteCall.isExecuted()) {
                    subredditAutocompleteCall.cancel();
                }
                if (autoCompleteRunnable != null) {
                    handler.removeCallbacks(autoCompleteRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String currentQuery = editable.toString().trim();
                if (!currentQuery.isEmpty()) {
                    autoCompleteRunnable = () -> {
                        boolean anonymous = activity.accountName.equals(Account.ANONYMOUS_ACCOUNT);
                        Retrofit autocompleteRetrofit = anonymous ? retrofit : oauthRetrofit;
                        Map<String, String> autocompleteHeaders = anonymous ? new HashMap<>() : APIUtils.getOAuthHeader(activity.accessToken);
                        subredditAutocompleteCall = autocompleteRetrofit.create(RedditAPI.class).subredditAutocomplete(autocompleteHeaders,
                                currentQuery, nsfw);
                        subredditAutocompleteCall.enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                subredditAutocompleteCall = null;
                                if (response.isSuccessful() && !call.isCanceled()) {
                                    ParseSubredditData.parseSubredditListingData(executor, handler,
                                            response.body(), nsfw, new ParseSubredditData.ParseSubredditListingDataListener() {
                                                @Override
                                                public void onParseSubredditListingDataSuccess(ArrayList<SubredditData> subredditData, String after) {
                                                    adapter.setSubreddits(subredditData);
                                                }

                                                @Override
                                                public void onParseSubredditListingDataFail() {

                                                }
                                            });
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                subredditAutocompleteCall = null;
                            }
                        });
                    };

                    handler.postDelayed(autoCompleteRunnable, 500);
                }
            }
        });
        new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                .setTitle(R.string.go_to_subreddit)
                .setView(rootView)
                .setPositiveButton(R.string.ok, (dialogInterface, i)
                        -> {
                    Utils.hideKeyboard(activity);
                    Intent subredditIntent = new Intent(activity, ViewSubredditDetailActivity.class);
                    subredditIntent.putExtra(ViewSubredditDetailActivity.EXTRA_SUBREDDIT_NAME_KEY, Objects.requireNonNull(thingEditText.getText()).toString());
                    activity.startActivity(subredditIntent);
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    Utils.hideKeyboard(activity);
                })
                .setOnDismissListener(dialogInterface -> {
                    Utils.hideKeyboard(activity);
                })
                .show();
    }

    /** Shows the "Go to user" dialog. */
    static void goToUser(BaseActivity activity, ViewGroup parent) {
        View rootView = activity.getLayoutInflater().inflate(R.layout.dialog_go_to_thing_edit_text, parent, false);
        TextInputEditText thingEditText = rootView.findViewById(R.id.text_input_edit_text_go_to_thing_edit_text);
        thingEditText.requestFocus();
        Utils.showKeyboard(activity, new Handler(), thingEditText);
        thingEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                Utils.hideKeyboard(activity);
                Intent userIntent = new Intent(activity, ViewUserDetailActivity.class);
                userIntent.putExtra(ViewUserDetailActivity.EXTRA_USER_NAME_KEY, Objects.requireNonNull(thingEditText.getText()).toString());
                activity.startActivity(userIntent);
                return true;
            }
            return false;
        });
        new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                .setTitle(R.string.go_to_user)
                .setView(rootView)
                .setPositiveButton(R.string.ok, (dialogInterface, i)
                        -> {
                    Utils.hideKeyboard(activity);
                    Intent userIntent = new Intent(activity, ViewUserDetailActivity.class);
                    userIntent.putExtra(ViewUserDetailActivity.EXTRA_USER_NAME_KEY, Objects.requireNonNull(thingEditText.getText()).toString());
                    activity.startActivity(userIntent);
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    Utils.hideKeyboard(activity);
                })
                .setOnDismissListener(dialogInterface -> {
                    Utils.hideKeyboard(activity);
                })
                .show();
    }
}
