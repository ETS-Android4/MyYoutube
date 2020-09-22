package io.awesome.gultube.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import io.awesome.gultube.R;

public class GlideUtils {
	
	public static void loadAvatar(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.user_default)
				.error(R.drawable.user_default)
				.fallback(R.drawable.user_default)
				.into(imageView);
	}
	
	public static void loadThumbnail(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.no_image)
				.error(R.drawable.no_image)
				.fallback(R.drawable.no_image)
				.into(imageView);
	}
	
	public static void loadChannelBanner(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.channel_banner)
				.error(R.drawable.channel_banner)
				.fallback(R.drawable.channel_banner)
				.into(imageView);
	}
}
