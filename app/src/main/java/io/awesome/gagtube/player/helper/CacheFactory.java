package io.awesome.gagtube.player.helper;

import android.content.Context;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

import androidx.annotation.NonNull;

class CacheFactory implements DataSource.Factory {
	
	private static final String CACHE_FOLDER_NAME = "exoplayer";
	private static final int CACHE_FLAGS = CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR;
	
	private final DefaultDataSourceFactory dataSourceFactory;
	private final long maxFileSize;
	
	// Creating cache on every instance may cause problems with multiple players when
	// sources are not ExtractorMediaSource
	// see: https://stackoverflow.com/questions/28700391/using-cache-in-exoplayer
	private static SimpleCache cache;
	
	public CacheFactory(@NonNull final Context context, @NonNull final String userAgent, @NonNull final TransferListener transferListener) {
		
		this(context, userAgent, transferListener, PlayerHelper.getPreferredCacheSize(), PlayerHelper.getPreferredFileSize());
	}
	
	private CacheFactory(@NonNull final Context context, @NonNull final String userAgent, @NonNull final TransferListener transferListener, final long maxCacheSize, final long maxFileSize) {
		
		this.maxFileSize = maxFileSize;
		
		dataSourceFactory = new DefaultDataSourceFactory(context, userAgent, transferListener);
		File cacheDir = new File(context.getExternalCacheDir(), CACHE_FOLDER_NAME);
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		
		if (cache == null) {
			final LeastRecentlyUsedCacheEvictor cacheEvictor = new LeastRecentlyUsedCacheEvictor(maxCacheSize);
			cache = new SimpleCache(cacheDir, cacheEvictor, new ExoDatabaseProvider(context));
		}
	}
	
	@Override
	public DataSource createDataSource() {
		
		final DefaultDataSource dataSource = dataSourceFactory.createDataSource();
		final FileDataSource fileSource = new FileDataSource();
		final CacheDataSink dataSink = new CacheDataSink(cache, maxFileSize);
		
		return new CacheDataSource(cache, dataSource, fileSource, dataSink, CACHE_FLAGS, null);
	}
}