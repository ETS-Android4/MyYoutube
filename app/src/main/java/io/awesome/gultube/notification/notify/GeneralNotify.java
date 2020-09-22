package io.awesome.gultube.notification.notify;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.core.app.NotificationCompat;
import io.awesome.gultube.R;
import io.awesome.gultube.notification.GFirebaseMessagingService;
import io.awesome.gultube.notification.push.GeneralPush;

public class GeneralNotify {
	
	public static void createNotification(Context context, GeneralPush generalPush) {
		
		String channelId = context.getString(R.string.general_channel_name);
		
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// builder for Notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
		
		// big picture style
		NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
		
		String title = generalPush.getTitle();
		String text = generalPush.getAlert();
		
		// title
		builder.setContentTitle(title);
		bigPictureStyle.setBigContentTitle(title);
		// text
		builder.setContentText(text);
		// image
		bigPictureStyle.bigPicture(getBitmapFromURL(generalPush.getImageUrl()));
		
		// set expanded style
		builder.setStyle(bigPictureStyle);
		
		// high priority and vibrate/sound will give heads up on lollipop
		builder.setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_MESSAGE);
		// make it vibrate, sound
		GFirebaseMessagingService.setSound(builder);
		GFirebaseMessagingService.setVibrate(builder);
		
		// small icon
		builder.setSmallIcon(R.drawable.ic_youtube_notification);
		// ticker is latest generalPush
		builder.setTicker(generalPush.getAlert());
		// intent
		builder.setContentIntent(getTaskStack(context, generalPush));
		// auto dismiss on press
		builder.setAutoCancel(true);
		// set notification to channel
		builder.setChannelId(channelId);
		
		// NotificationManager do notify()
		if (notificationManager != null) {
			notificationManager.notify("GeneralNotify", 1000, builder.build());
		}
	}
	
	private static PendingIntent getTaskStack(Context context, GeneralPush generalPush) {
		
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + generalPush.getPackageName()));
		
		return PendingIntent.getActivity(context, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	private static Bitmap getBitmapFromURL(String strURL) {
		
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			return BitmapFactory.decodeStream(input);
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}