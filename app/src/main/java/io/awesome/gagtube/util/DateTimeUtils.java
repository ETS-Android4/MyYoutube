package io.awesome.gagtube.util;

import android.content.Context;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
	
	public static Date startOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}
	
	// show rate dialog
	public static boolean isInstalled2Days(Context context) {
		long timeInMilliseconds = SharedPrefsHelper.getLongPrefs(context, SharedPrefsHelper.Key.START_APP_DATE.name());
		Date startAppDate = startOfDay(new Date(timeInMilliseconds));
		Date currentDate = startOfDay(new Date());
		return dateDiff(startAppDate, currentDate) == 2;
	}
	
	// send local push notification 1
	public static boolean isInstalled5Days(Context context) {
		long timeInMilliseconds = SharedPrefsHelper.getLongPrefs(context, SharedPrefsHelper.Key.START_APP_DATE.name());
		Date startAppDate = startOfDay(new Date(timeInMilliseconds));
		Date currentDate = startOfDay(new Date());
		return dateDiff(startAppDate, currentDate) == 5;
	}
	
	// send local push notification 2
	public static boolean isInstalled7Days(Context context) {
		long timeInMilliseconds = SharedPrefsHelper.getLongPrefs(context, SharedPrefsHelper.Key.START_APP_DATE.name());
		Date startAppDate = startOfDay(new Date(timeInMilliseconds));
		Date currentDate = startOfDay(new Date());
		return dateDiff(startAppDate, currentDate) == 7;
	}
	
	// send local push notification 3
	public static boolean isInstalled10Days(Context context) {
		long timeInMilliseconds = SharedPrefsHelper.getLongPrefs(context, SharedPrefsHelper.Key.START_APP_DATE.name());
		Date startAppDate = startOfDay(new Date(timeInMilliseconds));
		Date currentDate = startOfDay(new Date());
		return dateDiff(startAppDate, currentDate) == 10;
	}
	
	// show "We are so close" dialog
	public static boolean isInstalled15Days(Context context) {
		long timeInMilliseconds = SharedPrefsHelper.getLongPrefs(context, SharedPrefsHelper.Key.START_APP_DATE.name());
		Date startAppDate = startOfDay(new Date(timeInMilliseconds));
		Date currentDate = startOfDay(new Date());
		return dateDiff(startAppDate, currentDate) == 15;
	}
	
	public static long dateDiff(Date dateFrom, Date dateTo) {
		long diff = dateTo.getTime() - dateFrom.getTime();
		return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}
}
