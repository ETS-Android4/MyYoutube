package io.awesome.gagtube.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;

import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.Decade;
import org.schabi.newpipe.extractor.localization.ContentCountry;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import io.awesome.gagtube.R;

public class Localization {

    private static PrettyTime prettyTime;
    public final static String DOT_SEPARATOR = " • ";
    public static final String PATTERN_DATE_MMMddYYYY = "MMM dd, yyyy";

    private Localization() {
    }

    public static void init() {
        initPrettyTime();
    }

    @NonNull
    public static String concatenateStrings(final String... strings) {
        return concatenateStrings(Arrays.asList(strings));
    }

    @NonNull
    public static String concatenateStrings(final List<String> strings) {
        if (strings.isEmpty()) return "";

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strings.get(0));

        for (int i = 1; i < strings.size(); i++) {
            final String string = strings.get(i);
            if (!TextUtils.isEmpty(string)) {
                stringBuilder.append(DOT_SEPARATOR).append(strings.get(i));
            }
        }

        return stringBuilder.toString();
    }

    public static org.schabi.newpipe.extractor.localization.Localization getPreferredLocalization(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String languageCode = sharedPreferences.getString(Constants.LANGUAGE_CODE, Locale.getDefault().getLanguage());

        return org.schabi.newpipe.extractor.localization.Localization.fromLocalizationCode(languageCode);
    }

    public static ContentCountry getPreferredContentCountry(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String countryCode = sharedPreferences.getString(Constants.COUNTRY_CODE, Locale.getDefault().getCountry());
        return new ContentCountry(countryCode);
    }

    public static Locale getPreferredLocale(Context context) {

        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String languageCode = sharedPreferences.getString(Constants.LANGUAGE_CODE, Locale.getDefault().getLanguage());
            return new Locale(languageCode);
        }
        return new Locale(Locale.getDefault().getLanguage());
    }

    public static String localizeNumber(Context context, long number) {
        Locale locale = getPreferredLocale(context);
        NumberFormat nf = NumberFormat.getInstance(locale);
        return nf.format(number);
    }

    private static String formatDate(Context context, Date date) {
        Locale locale = getPreferredLocale(context);
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_DATE_MMMddYYYY, locale);
        return formatter.format(date);
    }

    public static String localizeDate(Context context, Date date) {
        Resources res = context.getResources();
        String dateString = res.getString(R.string.upload_date_text);

        String formattedDate = formatDate(context, date);
        return String.format(dateString, formattedDate);
    }

    public static String localizeViewCount(Context context, long viewCount) {
        return getQuantity(context, R.plurals.views, R.string.no_views, viewCount, localizeNumber(context, viewCount));
    }

    public static String localizeSubscribersCount(Context context, long subscriberCount) {
        return getQuantity(context, R.plurals.subscribers, R.string.no_subscribers, subscriberCount, localizeNumber(context, subscriberCount));
    }

    public static String localizeStreamCount(Context context, long streamCount) {
        return getQuantity(context, R.plurals.videos, R.string.no_videos, streamCount, localizeNumber(context, streamCount));
    }

    public static String shortCount(Context context, long count) {
        if (count >= 1000000000) {
            return count / 1000000000 + context.getString(R.string.short_billion);
        } else if (count >= 1000000) {
            return count / 1000000 + context.getString(R.string.short_million);
        } else if (count >= 1000) {
            return count / 1000 + context.getString(R.string.short_thousand);
        } else {
            return Long.toString(count);
        }
    }

    public static String shortViewCount(Context context, long viewCount) {
        return getQuantity(context, R.plurals.views, R.string.no_views, viewCount, shortCount(context, viewCount));
    }

    public static String shortSubscriberCount(Context context, long subscriberCount) {
        return getQuantity(context, R.plurals.subscribers, R.string.no_subscribers, subscriberCount, shortCount(context, subscriberCount));
    }

    private static String getQuantity(Context context, @PluralsRes int pluralId, @StringRes int zeroCaseStringId, long count, String formattedCount) {
        if (count == 0) return context.getString(zeroCaseStringId);

        // As we use the already formatted count, is not the responsibility of this method handle long numbers
        // (it probably will fall in the "other" category, or some language have some specific rule... then we have to change it)
        int safeCount = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : count < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) count;
        return context.getResources().getQuantityString(pluralId, safeCount, formattedCount);
    }

    public static String getDurationString(long duration) {
        if (duration < 0) {
            duration = 0;
        }
        String output;
        long days = duration / (24 * 60 * 60L); /* greater than a day */
        duration %= (24 * 60 * 60L);
        long hours = duration / (60 * 60L); /* greater than an hour */
        duration %= (60 * 60L);
        long minutes = duration / 60L;
        long seconds = duration % 60L;

        //handle days
        if (days > 0) {
            output = String.format(Locale.US, "%d:%02d:%02d:%02d", days, hours, minutes, seconds);
        } else if (hours > 0) {
            output = String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            output = String.format(Locale.US, "%d:%02d", minutes, seconds);
        }
        return output;
    }

    private static void initPrettyTime() {
        prettyTime = new PrettyTime(Locale.getDefault());
        // Do not use decades as YouTube doesn't either.
        prettyTime.removeUnit(Decade.class);
    }

    private static PrettyTime getPrettyTime() {
        // If pretty time's Locale is different, init again with the new one.
        if (!prettyTime.getLocale().equals(Locale.getDefault())) {
            initPrettyTime();
        }
        return prettyTime;
    }

    public static String relativeTime(Calendar calendarTime) {
        return getPrettyTime().formatUnrounded(calendarTime);
    }
}
