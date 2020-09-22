package io.awesome.gultube.report;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Vector;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.awesome.gultube.ActivityCommunicator;
import io.awesome.gultube.R;
import io.awesome.gultube.activities.MainActivity;
import io.awesome.gultube.base.BaseActivity;
import io.awesome.gultube.util.ThemeHelper;

public class ErrorActivity extends BaseActivity {
	
	@BindView(R.id.default_toolbar) Toolbar toolbar;
	
	// BUNDLE TAGS
	public static final String ERROR_INFO = "error_info";
	public static final String ERROR_LIST = "error_list";
	
	//private String[] errorList;
	//private ErrorInfo errorInfo;
	private Class returnActivity;
	//private String currentTimeStamp;
	
	public static void reportUiError(final AppCompatActivity activity, final Throwable el) {
		
		reportError(activity, el, activity.getClass(), null,
					ErrorInfo.make(UserAction.UI_ERROR, "none", "", R.string.app_ui_crash));
	}
	
	public static void reportError(final Context context, final List<Throwable> el,
								   final Class returnActivity, View rootView, final ErrorInfo errorInfo) {
		if (rootView != null) {
			Snackbar.make(rootView, R.string.error_snackbar_message, 3 * 1000)
					.setActionTextColor(Color.YELLOW)
					.setAction(R.string.error_snackbar_action, v -> startErrorActivity(returnActivity, context, errorInfo, el)).show();
		}
		else {
			startErrorActivity(returnActivity, context, errorInfo, el);
		}
	}
	
	private static void startErrorActivity(Class returnActivity, Context context, ErrorInfo errorInfo, List<Throwable> throwableList) {
		
		ActivityCommunicator activityCommunicator = ActivityCommunicator.getCommunicator();
		activityCommunicator.returnActivity = returnActivity;
		Intent intent = new Intent(context, ErrorActivity.class);
		//intent.putExtra(ERROR_INFO, errorInfo);
		//intent.putExtra(ERROR_LIST, elToSl(throwableList));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
	
	public static void reportError(final Context context, final Throwable e,
								   final Class returnActivity, View rootView, final ErrorInfo errorInfo) {
		
		List<Throwable> throwableList = null;
		if (e != null) {
			throwableList = new Vector<>();
			throwableList.add(e);
		}
		reportError(context, throwableList, returnActivity, rootView, errorInfo);
	}
	
	private static String getStackTrace(final Throwable throwable) {
		
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
	
	// errorList to StringList
	private static String[] elToSl(List<Throwable> stackTraces) {
		
		String[] out = new String[stackTraces.size()];
		for (int i = 0; i < stackTraces.size(); i++) {
			out[i] = getStackTrace(stackTraces.get(i));
		}
		return out;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		ThemeHelper.setTheme(this);
		setContentView(R.layout.activity_error);
		ButterKnife.bind(this, this);
		
		//Intent intent = getIntent();
		
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.the_app_is_under_maintenance);
			actionBar.setDisplayShowTitleEnabled(true);
		}
		
		// views
		/*TextView errorView = findViewById(R.id.errorView);
		TextView errorMessageView = findViewById(R.id.errorMessageView);*/
		
		ActivityCommunicator ac = ActivityCommunicator.getCommunicator();
		returnActivity = ac.returnActivity;
		//errorInfo = intent.getParcelableExtra(ERROR_INFO);
		//errorList = intent.getStringArrayExtra(ERROR_LIST);
		
		// important add guru meditation
		/*addGuruMeditation();
		currentTimeStamp = getCurrentTimeStamp();
		
		// normal bug report
		if (errorInfo.message != 0) {
			errorMessageView.setText(errorInfo.message);
		}
		else {
			errorMessageView.setVisibility(View.GONE);
			findViewById(R.id.messageWhatHappenedView).setVisibility(View.GONE);
		}
		
		errorView.setText(formErrorText(errorList));*/
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		/*MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.error_menu, menu);*/
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
			
			case android.R.id.home:
				goToReturnActivity();
				break;
			
			/*case R.id.menu_item_share_error: {
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setDataAndType(Uri.parse("mailto:"), "text/plain");
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.FEEDBACK_EMAIL});
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Android Feedback");
				intent.putExtra(Intent.EXTRA_TEXT, buildErrorContent());
				
				// open mail apps
				try {
					if (intent.resolveActivity(getPackageManager()) != null) {
						startActivity(intent);
					}
				}
				catch (ActivityNotFoundException ex) {
					Toast.makeText(this, R.string.msg_no_apps, Toast.LENGTH_SHORT).show();
				}
			}
			break;*/
		}
		return false;
	}
	
	/*private String formErrorText(String[] strings) {
		
		StringBuilder text = new StringBuilder();
		if (strings != null) {
			for (String string : strings) {
				text.append("-------------------------------------\n").append(string);
			}
		}
		text.append("-------------------------------------");
		return text.toString();
	}*/
	
	/**
	 * Get the checked activity.
	 *
	 * @param returnActivity the activity to return to
	 * @return the casted return activity or null
	 */
	@Nullable
	static Class<? extends Activity> getReturnActivity(Class<?> returnActivity) {
		
		Class<? extends Activity> checkedReturnActivity = null;
		if (returnActivity != null) {
			if (Activity.class.isAssignableFrom(returnActivity)) {
				checkedReturnActivity = returnActivity.asSubclass(Activity.class);
			}
			else {
				checkedReturnActivity = MainActivity.class;
			}
		}
		return checkedReturnActivity;
	}
	
	private void goToReturnActivity() {
		
		Class<? extends Activity> checkedReturnActivity = getReturnActivity(returnActivity);
		if (checkedReturnActivity == null) {
			super.onBackPressed();
		}
		else {
			Intent intent = new Intent(this, checkedReturnActivity);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			NavUtils.navigateUpTo(this, intent);
		}
	}
	
	/*private String buildErrorContent() {
		try {
			JSONArray exceptionArray = new JSONArray();
			if (errorList != null) {
				for (String error : errorList) {
					exceptionArray.put(error);
				}
			}
			
			String userAction = String.format("User action [%s]\n\n", getUserActionString(errorInfo.userAction));
			String request = String.format("Request [%s]\n\n", errorInfo.request);
			String exceptions = String.format("Error Detail [%s]\n\n", exceptionArray.toString());
			
			String model = String.format("Model [%s]", Build.MODEL);
			String os = String.format("OS [%s]", "Android");
			String os_version = String.format("OS Version [%s]", Build.VERSION.RELEASE);
			String deviceInfo = String.format("About Device:\n%s\n%s\n%s\n\n", model, os, os_version);
			String time = String.format("Time: [%s]\n", currentTimeStamp);
			
			return userAction + request + exceptions + deviceInfo + time;
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	private String getUserActionString(UserAction userAction) {
		if (userAction == null) {
			return "Your description is in another castle.";
		}
		else {
			return userAction.getMessage();
		}
	}
	
	private void addGuruMeditation() {
		
		TextView sorryView = findViewById(R.id.errorSorryView);
		String text = sorryView.getText().toString();
		sorryView.setText(text);
	}*/
	
	@Override
	public void onBackPressed() {
		
		goToReturnActivity();
	}
	
	/*public String getCurrentTimeStamp() {
		
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(new Date());
	}*/
	
	public static class ErrorInfo implements Parcelable {
		
		public static final Creator<ErrorInfo> CREATOR = new Creator<ErrorInfo>() {
			
			@Override
			public ErrorInfo createFromParcel(Parcel source) {
				return new ErrorInfo(source);
			}
			
			@Override
			public ErrorInfo[] newArray(int size) {
				return new ErrorInfo[size];
			}
		};
		
		final UserAction userAction;
		final public String request;
		final String serviceName;
		@StringRes final public int message;
		
		private ErrorInfo(UserAction userAction, String serviceName, String request, @StringRes int message) {
			
			this.userAction = userAction;
			this.serviceName = serviceName;
			this.request = request;
			this.message = message;
		}
		
		protected ErrorInfo(Parcel in) {
			
			this.userAction = UserAction.valueOf(in.readString());
			this.request = in.readString();
			this.serviceName = in.readString();
			this.message = in.readInt();
		}
		
		public static ErrorInfo make(UserAction userAction, String serviceName, String request, @StringRes int message) {
			
			return new ErrorInfo(userAction, serviceName, request, message);
		}
		
		@Override
		public int describeContents() {
			return 0;
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			
			dest.writeString(this.userAction.name());
			dest.writeString(this.request);
			dest.writeString(this.serviceName);
			dest.writeInt(this.message);
		}
	}
}
