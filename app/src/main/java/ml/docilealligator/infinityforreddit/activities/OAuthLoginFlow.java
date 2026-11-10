package ml.docilealligator.infinityforreddit.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.account.FetchMyInfo;
import ml.docilealligator.infinityforreddit.apis.RedditAPI;
import ml.docilealligator.infinityforreddit.asynctasks.ParseAndInsertNewAccount;
import ml.docilealligator.infinityforreddit.events.NewUserLoggedInEvent;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.OAuthLoginHelper;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * What happens once Reddit has redirected back to the app, shared by the WebView login and the
 * Chrome Custom Tab login: check the redirect, trade the code for tokens, fetch the account, store
 * it and close the screen. Every outcome other than success tells the reader and closes the screen.
 */
final class OAuthLoginFlow {

    private final Activity activity;
    private final Retrofit retrofit;
    private final Retrofit oauthRetrofit;
    private final RedditDataRoomDatabase redditDataRoomDatabase;
    private final SharedPreferences currentAccountSharedPreferences;
    private final Executor executor;
    private final Handler handler;

    OAuthLoginFlow(Activity activity, Retrofit retrofit, Retrofit oauthRetrofit,
                   RedditDataRoomDatabase redditDataRoomDatabase,
                   SharedPreferences currentAccountSharedPreferences,
                   Executor executor, Handler handler) {
        this.activity = activity;
        this.retrofit = retrofit;
        this.oauthRetrofit = oauthRetrofit;
        this.redditDataRoomDatabase = redditDataRoomDatabase;
        this.currentAccountSharedPreferences = currentAccountSharedPreferences;
        this.executor = executor;
        this.handler = handler;
    }

    /** Acts on the redirect {@code uri}: only one carrying the state of the attempt is exchanged. */
    void handleRedirect(@NonNull Uri uri) {
        OAuthLoginHelper.RedirectResult redirect = OAuthLoginHelper.classifyRedirect(uri.getQueryParameter("code"),
                OAuthLoginHelper.consumePendingState(activity, uri.getQueryParameter("state")),
                uri.getQueryParameter("error"));
        switch (redirect.action) {
            case EXCHANGE_CODE:
                exchangeCodeForToken(Objects.requireNonNull(redirect.authCode));
                break;
            case ACCESS_DENIED:
                Toast.makeText(activity, R.string.access_denied, Toast.LENGTH_SHORT).show();
                activity.finish();
                break;
            case OAUTH_ERROR:
                Toast.makeText(activity, activity.getString(R.string.oauth_error_reddit_error, redirect.errorValue), Toast.LENGTH_LONG).show();
                activity.finish();
                break;
            case STATE_MISMATCH:
            case NONE:
            default:
                Toast.makeText(activity, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                activity.finish();
                break;
        }
    }

    private void exchangeCodeForToken(String authCode) {
        Map<String, String> params = new HashMap<>();
        params.put(APIUtils.GRANT_TYPE_KEY, "authorization_code");
        params.put("code", authCode);
        params.put(APIUtils.REDIRECT_URI_KEY, APIUtils.REDIRECT_URI);

        RedditAPI api = retrofit.create(RedditAPI.class);
        Call<String> accessTokenCall = api.getAccessToken(APIUtils.getHttpBasicAuthHeader(activity.getApplicationContext()), params);
        accessTokenCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                OAuthLoginHelper.TokenResult result =
                        OAuthLoginHelper.classifyTokenResponse(response);
                if (!result.isSuccess()) {
                    Toast.makeText(activity, OAuthLoginHelper.describeFailure(activity.getApplicationContext(), result), Toast.LENGTH_LONG).show();
                    activity.finish();
                    return;
                }
                String accessToken = Objects.requireNonNull(result.accessToken);
                String refreshToken = Objects.requireNonNull(result.refreshToken);

                FetchMyInfo.fetchAccountInfo(executor, handler, oauthRetrofit,
                        redditDataRoomDatabase, accessToken,
                        new FetchMyInfo.FetchMyInfoListener() {
                            @Override
                            public void onFetchMyInfoSuccess(String name, String profileImageUrl, @Nullable String bannerImageUrl, int karma, boolean isMod) {
                                currentAccountSharedPreferences.edit().putString(SharedPreferencesUtils.ACCESS_TOKEN, accessToken)
                                        .putString(SharedPreferencesUtils.ACCOUNT_NAME, name)
                                        .putString(SharedPreferencesUtils.ACCOUNT_IMAGE_URL, profileImageUrl).apply();
                                currentAccountSharedPreferences.edit().remove(SharedPreferencesUtils.SUBSCRIBED_THINGS_SYNC_TIME).apply();
                                ParseAndInsertNewAccount.parseAndInsertNewAccount(executor, new Handler(), name, accessToken, refreshToken, profileImageUrl, bannerImageUrl,
                                        karma, isMod, authCode, redditDataRoomDatabase.accountDao(),
                                        () -> {
                                            EventBus.getDefault().post(new NewUserLoggedInEvent());
                                            activity.finish();
                                        });
                            }

                            @Override
                            public void onFetchMyInfoFailed(boolean parseFailed) {
                                if (parseFailed) {
                                    Toast.makeText(activity, R.string.parse_user_info_error, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(activity, R.string.cannot_fetch_user_info, Toast.LENGTH_SHORT).show();
                                }

                                activity.finish();
                            }
                        });
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                OAuthLoginHelper.TokenResult result = OAuthLoginHelper.classifyThrowable(t);
                Toast.makeText(activity, OAuthLoginHelper.describeFailure(activity.getApplicationContext(), result), Toast.LENGTH_LONG).show();
                activity.finish();
            }
        });
    }
}
