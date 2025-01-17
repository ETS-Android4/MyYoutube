package io.awesome.gagtube.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import com.annimon.stream.Stream;

import org.schabi.newpipe.extractor.NewPipe;

import java.util.Date;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import io.awesome.gagtube.R;
import io.awesome.gagtube.base.BaseActivity;
import io.awesome.gagtube.util.AppUtils;
import io.awesome.gagtube.util.Constants;
import io.awesome.gagtube.util.DateTimeUtils;
import io.awesome.gagtube.util.Localization;
import io.awesome.gagtube.util.SharedPrefsHelper;
import io.awesome.gagtube.util.ThemeHelper;

import static org.schabi.newpipe.extractor.NewPipe.getDownloader;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        setTheme(ThemeHelper.getSettingsThemeStyle(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = defaultPreferences.edit();

        String countryCode = AppUtils.getDeviceCountryIso(this);
        String languageCode = Stream.of(Locale.getAvailableLocales()).filter(locale -> locale.getCountry().equals(AppUtils.getDeviceCountryIso(this))).map(Locale::getLanguage).findFirst().get();
        // save COUNTRY_CODE, LANGUAGE_CODE to preferences
        editor.putString(Constants.COUNTRY_CODE, countryCode);
        editor.putString(Constants.LANGUAGE_CODE, languageCode);
        editor.apply();

        // init localization
        NewPipe.init(getDownloader(), Localization.getPreferredLocalization(this), Localization.getPreferredContentCountry(this));
        
        // if start app in the first time
        if (SharedPrefsHelper.getLongPrefs(this, SharedPrefsHelper.Key.START_APP_DATE.name()) == 0) {
            // save the start app date
            Date startAppDate = DateTimeUtils.startOfDay(new Date());
            SharedPrefsHelper.setLongPrefs(this, SharedPrefsHelper.Key.START_APP_DATE.name(), startAppDate.getTime());
        }

        openMainActivity();
    }

    private void openMainActivity() {

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        // end here
        finish();
    }
}
