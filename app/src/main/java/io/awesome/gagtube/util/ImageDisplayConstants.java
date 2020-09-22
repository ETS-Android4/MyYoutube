package io.awesome.gagtube.util;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import io.awesome.gagtube.R;

public class ImageDisplayConstants {
	
	private static final int BITMAP_FADE_IN_DURATION_MILLIS = 50;
	
	private static final DisplayImageOptions BASE_DISPLAY_IMAGE_OPTIONS =
			new DisplayImageOptions.Builder()
					.cacheInMemory(true)
					.cacheOnDisk(true)
					.resetViewBeforeLoading(true)
					.bitmapConfig(Bitmap.Config.RGB_565)
					.imageScaleType(ImageScaleType.EXACTLY)
					.displayer(new FadeInBitmapDisplayer(BITMAP_FADE_IN_DURATION_MILLIS))
					.build();
	
	public static final DisplayImageOptions DISPLAY_THUMBNAIL_OPTIONS =
			new DisplayImageOptions.Builder()
					.cloneFrom(BASE_DISPLAY_IMAGE_OPTIONS)
					.showImageForEmptyUri(R.drawable.no_image)
					.showImageOnFail(R.drawable.no_image)
					.build();
}
