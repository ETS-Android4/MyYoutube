package io.awesome.gagtube.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import io.awesome.gagtube.R;
import io.awesome.gagtube.util.ThemeHelper;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setStatusBarGradient(this);
	}
	
	public static void setStatusBarGradient(Activity activity) {
		
		Window window = activity.getWindow();
		window.setStatusBarColor(ThemeHelper.isLightThemeSelected(activity) ? ContextCompat.getColor(activity, R.color.light_status_bar_color) : ContextCompat.getColor(activity, R.color.dark_status_bar_color));
	}
}
