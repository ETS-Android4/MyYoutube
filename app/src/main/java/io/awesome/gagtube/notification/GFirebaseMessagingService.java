package io.awesome.gagtube.notification;

import android.annotation.SuppressLint;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import androidx.core.app.NotificationCompat;
import io.awesome.gagtube.notification.notify.GeneralNotify;
import io.awesome.gagtube.notification.push.GeneralPush;
import io.awesome.gagtube.notification.push.Push;

public class GFirebaseMessagingService extends FirebaseMessagingService {
	
	@Override
	public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {
		
		// create Push from remoteMessage
		Push push = createPush(remoteMessage);
		
		// general push
		if (push instanceof GeneralPush) {
			GeneralNotify.createNotification(this, (GeneralPush) push);
		}
	}
	
	@SuppressLint("LongLogTag")
	private static Push createPush(RemoteMessage remoteMessage) {
		
		// get notification data
		Log.d("GFirebaseMessagingService", remoteMessage.getData().toString());
		
		try {
			JSONObject json = new JSONObject(remoteMessage.getData().toString());
			JSONObject data = json.getJSONObject("data");
			
			// mandatory alert field
			String title = data.getString("title");
			String alert = data.getString("alert");
			String imageUrl = data.getString("imageUrl");
			String packageName = data.getString("packageName");
			long sentTime = remoteMessage.getSentTime();
			
			return new GeneralPush(title, alert, imageUrl, packageName, sentTime);
		}
		catch (Exception e) {
			Log.e("GFirebaseMessagingService", "Exception: " + e.getMessage());
			return null;
		}
	}
	
	public static void setSound(NotificationCompat.Builder builder) {
		
		// set to default tone
		builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		
		// setGroupAlertBehavior
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// bug of double notification sound in Android Oreo
			builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
		}
	}
	
	public static void setVibrate(NotificationCompat.Builder builder) {
		
		// default
		builder.setVibrate(new long[]{0, 250});
	}
}