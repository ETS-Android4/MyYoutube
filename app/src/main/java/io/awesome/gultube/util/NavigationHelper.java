package io.awesome.gultube.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import io.awesome.gultube.R;
import io.awesome.gultube.activities.MainActivity;
import io.awesome.gultube.download.ui.DownloadActivity;
import io.awesome.gultube.fragments.MainFragment;
import io.awesome.gultube.fragments.detail.VideoDetailFragment;
import io.awesome.gultube.fragments.list.channel.ChannelFragment;
import io.awesome.gultube.fragments.list.playlist.PlaylistFragment;
import io.awesome.gultube.fragments.list.search.SearchFragment;
import io.awesome.gultube.local.feed.FeedFragment;
import io.awesome.gultube.local.playlist.LocalPlaylistFragment;
import io.awesome.gultube.player.BasePlayer;
import io.awesome.gultube.player.MainVideoPlayer;
import io.awesome.gultube.player.PopupVideoPlayer;
import io.awesome.gultube.player.PopupVideoPlayerActivity;
import io.awesome.gultube.player.VideoPlayer;
import io.awesome.gultube.player.playqueue.PlayQueue;
import io.awesome.gultube.settings.SettingsActivity;

public class NavigationHelper {

    public static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";
    public static final String SEARCH_FRAGMENT_TAG = "search_fragment_tag";
    public static final String AUTO_PLAY = "auto_play";

    // Players
    @NonNull
    public static Intent getPlayerIntent(@NonNull final Context context,
                                         @NonNull final Class targetClazz,
                                         @NonNull final PlayQueue playQueue,
                                         @Nullable final String quality) {

        Intent intent = new Intent(context, targetClazz);

        final String cacheKey = SerializedCache.getInstance().put(playQueue, PlayQueue.class);
        if (cacheKey != null) intent.putExtra(VideoPlayer.PLAY_QUEUE_KEY, cacheKey);
        if (quality != null) intent.putExtra(VideoPlayer.PLAYBACK_QUALITY, quality);

        return intent;
    }

    @NonNull
    public static Intent getPlayerIntent(@NonNull final Context context,
                                         @NonNull final Class targetClazz,
                                         @NonNull final PlayQueue playQueue) {

        return getPlayerIntent(context, targetClazz, playQueue, null);
    }

    @NonNull
    public static Intent getPlayerEnqueueIntent(@NonNull final Context context,
                                                @NonNull final Class targetClazz,
                                                @NonNull final PlayQueue playQueue,
                                                final boolean selectOnAppend) {

        return getPlayerIntent(context, targetClazz, playQueue)
                .putExtra(BasePlayer.APPEND_ONLY, true)
                .putExtra(BasePlayer.SELECT_ON_APPEND, selectOnAppend);
    }

    @NonNull
    public static Intent getPlayerIntent(@NonNull final Context context,
                                         @NonNull final Class targetClazz,
                                         @NonNull final PlayQueue playQueue,
                                         final int repeatMode,
                                         final float playbackSpeed,
                                         final float playbackPitch,
                                         final boolean playbackSkipSilence,
                                         @Nullable final String playbackQuality) {

        return getPlayerIntent(context, targetClazz, playQueue, playbackQuality)
                .putExtra(BasePlayer.REPEAT_MODE, repeatMode)
                .putExtra(BasePlayer.PLAYBACK_SPEED, playbackSpeed)
                .putExtra(BasePlayer.PLAYBACK_PITCH, playbackPitch)
                .putExtra(BasePlayer.PLAYBACK_SKIP_SILENCE, playbackSkipSilence);
    }

    public static void playOnMainPlayer(final Context context, final PlayQueue queue) {

        final Intent playerIntent = getPlayerIntent(context, MainVideoPlayer.class, queue);
        playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(playerIntent);
    }

    public static void playOnPopupPlayer(final Context context, final PlayQueue queue) {

        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnableToast(context);
            return;
        }

        Toast.makeText(context, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show();
        startService(context, getPlayerIntent(context, PopupVideoPlayer.class, queue));
    }

    public static void enqueueOnPopupPlayer(final Context context, final PlayQueue queue) {

        enqueueOnPopupPlayer(context, queue, false);
    }

    public static void enqueueOnPopupPlayer(final Context context, final PlayQueue queue, boolean selectOnAppend) {

        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnableToast(context);
            return;
        }

        Toast.makeText(context, R.string.popup_playing_append, Toast.LENGTH_SHORT).show();
        startService(context, getPlayerEnqueueIntent(context, PopupVideoPlayer.class, queue, selectOnAppend));
    }

    public static void startService(@NonNull final Context context, @NonNull final Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @SuppressLint("CommitTransaction")
    private static FragmentTransaction defaultTransaction(FragmentManager fragmentManager) {

        return fragmentManager.beginTransaction().setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out, R.animator.custom_fade_in, R.animator.custom_fade_out);
    }

    public static void gotoMainFragment(FragmentManager fragmentManager) {

        ImageLoader.getInstance().clearMemoryCache();

        boolean popped = fragmentManager.popBackStackImmediate(MAIN_FRAGMENT_TAG, 0);
        if (!popped) openMainFragment(fragmentManager);
    }

    public static void openMainFragment(FragmentManager fragmentManager) {

        InfoCache.getInstance().trimCache();

        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new MainFragment())
                .addToBackStack(MAIN_FRAGMENT_TAG)
                .commit();
    }

    public static boolean hasSearchFragmentInBackstack(FragmentManager fragmentManager) {

        return fragmentManager.popBackStackImmediate(SEARCH_FRAGMENT_TAG, 0);
    }

    public static void openSearchFragment(FragmentManager fragmentManager, int serviceId, String searchString) {

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SearchFragment.getInstance(serviceId, searchString))
                .addToBackStack(SEARCH_FRAGMENT_TAG)
                .commit();
    }

    public static void openVideoDetailFragment(StreamInfoItem streamInfoItem, FragmentManager fragmentManager, int serviceId, String url, String title) {

        openVideoDetailFragment(streamInfoItem, fragmentManager, serviceId, url, title, false);
    }

    public static void openVideoDetailFragment(StreamInfoItem streamInfoItem, FragmentManager fragmentManager, int serviceId, String url, String title, boolean autoPlay) {

        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_holder);
        if (title == null) title = "";

        if (fragment instanceof VideoDetailFragment && fragment.isVisible()) {
            VideoDetailFragment detailFragment = (VideoDetailFragment) fragment;
            detailFragment.selectAndLoadVideo(serviceId, url, title);
            return;
        }

        VideoDetailFragment instance = VideoDetailFragment.getInstance(streamInfoItem, serviceId, url, title);

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, instance)
                .addToBackStack(null)
                .commit();
    }

    public static void openChannelFragment(FragmentManager fragmentManager, int serviceId, String url, String name) {

        if (name == null) name = "";

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, ChannelFragment.getInstance(serviceId, url, name))
                .addToBackStack(null)
                .commit();
    }

    public static void openPlaylistFragment(FragmentManager fragmentManager, int serviceId, String url, String name) {

        if (name == null) name = "";

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, PlaylistFragment.getInstance(serviceId, url, name))
                .addToBackStack(null)
                .commit();
    }

    public static void openWhatsNewFragment(FragmentManager fragmentManager) {

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new FeedFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openLocalPlaylistFragment(FragmentManager fragmentManager, long playlistId, String name) {

        if (name == null) name = "";

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, LocalPlaylistFragment.getInstance(playlistId, name))
                .addToBackStack(null)
                .commit();
    }

    public static void openVideoDetail(Context context, int serviceId, String url, String title, String thumbnailUrl) {

        Intent openIntent = getOpenIntent(context, url, serviceId, thumbnailUrl, StreamingService.LinkType.STREAM);
        if (title != null && !title.isEmpty()) {
            openIntent.putExtra(Constants.KEY_TITLE, title);
        }
        context.startActivity(openIntent);
    }

    public static void openSettings(Context context) {

        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
    
    public static void openDownloads(Activity activity) {
        
        if (!PermissionHelper.checkStoragePermissions(activity, PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
            return;
        }
        Intent intent = new Intent(activity, DownloadActivity.class);
        activity.startActivity(intent);
    }

    public static Intent getPopupPlayerActivityIntent(final Context context) {

        return getServicePlayerActivityIntent(context, PopupVideoPlayerActivity.class);
    }

    private static Intent getServicePlayerActivityIntent(final Context context, final Class clazz) {

        Intent intent = new Intent(context, clazz);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    // Link handling
    private static Intent getOpenIntent(Context context, String url, int serviceId, StreamingService.LinkType type) {

        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_URL, url);
        mIntent.putExtra(Constants.KEY_LINK_TYPE, type);
        return mIntent;
    }

    private static Intent getOpenIntent(Context context, String url, int serviceId, String thumbnailUrl, StreamingService.LinkType type) {

        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_URL, url);
        mIntent.putExtra(Constants.KEY_LINK_TYPE, type);
        mIntent.putExtra(Constants.KEY_THUMBNAIL_URL, thumbnailUrl);
        return mIntent;
    }
    
    public static Intent getIntentByLink(Context context, String url) throws ExtractionException {
        return getIntentByLink(context, NewPipe.getServiceByUrl(url), url);
    }

    public static Intent getIntentByLink(Context context, StreamingService service, String url) throws ExtractionException {

        StreamingService.LinkType linkType = service.getLinkTypeByUrl(url);

        if (linkType == StreamingService.LinkType.NONE) {
            throw new ExtractionException("Url not known to service. service=" + service + " url=" + url);
        }

        Intent rIntent = getOpenIntent(context, url, service.getServiceId(), linkType);

        if (linkType == StreamingService.LinkType.STREAM) {
            rIntent.putExtra(AUTO_PLAY, PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.autoplay_through_intent_key), false));
        }

        return rIntent;
    }

    private static Uri openMarket(String packageName) {

        return Uri.parse("market://details").buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
    }

    private static Uri getGooglePlay(String packageName) {

        return Uri.parse("https://play.google.com/store/apps/details").buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
    }

    public static void openGooglePlayStore(Context context, String packageName) {

        try {
            // Try market:// scheme
            context.startActivity(new Intent(Intent.ACTION_VIEW, openMarket(packageName)));
        } catch (ActivityNotFoundException e) {
            // Fall back to google play URL (don't worry F-Droid can handle it :)
            context.startActivity(new Intent(Intent.ACTION_VIEW, getGooglePlay(packageName)));
        }
    }

    public static void composeEmail(Context context, String subject) {

        //String model = String.format("Model [%s]", Build.MODEL);
        //String os = String.format("OS [%s]", "Android");
        //String os_version = String.format("OS Version [%s]", Build.VERSION.RELEASE);
        //String emailBody = String.format("About Device:\n%s\n%s\n%s", model, os, os_version);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.FEEDBACK_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_message));

        // open mail apps
        try {
            context.startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.msg_no_apps, Toast.LENGTH_SHORT).show();
        }
    }

    public static void recreateActivity(Activity activity) {

        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(activity.getIntent());
        activity.overridePendingTransition(0, 0);
    }
}