package io.awesome.gagtube.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

public class SerializedCache {
	
	private static final SerializedCache instance = new SerializedCache();
	private static final int MAX_ITEMS_ON_CACHE = 50;
	
	private static final LruCache<String, CacheData> lruCache = new LruCache<>(MAX_ITEMS_ON_CACHE);
	
	private SerializedCache() {
	}
	
	public static SerializedCache getInstance() {
		return instance;
	}
	
	@Nullable
	public <T> T take(@NonNull final String key, @NonNull final Class<T> type) {
		synchronized (lruCache) {
			return lruCache.get(key) != null ? getItem(lruCache.remove(key), type) : null;
		}
	}
	
	@Nullable
	public <T> T get(@NonNull final String key, @NonNull final Class<T> type) {
		synchronized (lruCache) {
			final CacheData data = lruCache.get(key);
			return data != null ? getItem(data, type) : null;
		}
	}
	
	@Nullable
	public <T extends Serializable> String put(@NonNull T item, @NonNull final Class<T> type) {
		final String key = UUID.randomUUID().toString();
		return put(key, item, type) ? key : null;
	}
	
	public <T extends Serializable> boolean put(@NonNull final String key, @NonNull T item, @NonNull final Class<T> type) {
		synchronized (lruCache) {
			try {
				lruCache.put(key, new CacheData<>(clone(item, type), type));
				return true;
			}
			catch (Exception ignored) {
			}
		}
		return false;
	}
	
	public void clear() {
		synchronized (lruCache) {
			lruCache.evictAll();
		}
	}
	
	public long size() {
		synchronized (lruCache) {
			return lruCache.size();
		}
	}
	
	@Nullable
	private <T> T getItem(@NonNull final CacheData data, @NonNull final Class<T> type) {
		return type.isAssignableFrom(data.type) ? type.cast(data.item) : null;
	}
	
	@NonNull
	private <T extends Serializable> T clone(@NonNull T item, @NonNull final Class<T> type) throws Exception {
		final ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream();
		try (final ObjectOutputStream objectOutput = new ObjectOutputStream(bytesOutput)) {
			objectOutput.writeObject(item);
			objectOutput.flush();
		}
		final Object clone = new ObjectInputStream(new ByteArrayInputStream(bytesOutput.toByteArray())).readObject();
		return type.cast(clone);
	}
	
	final private static class CacheData<T> {
		private final T item;
		private final Class<T> type;
		
		private CacheData(@NonNull final T item, @NonNull Class<T> type) {
			this.item = item;
			this.type = type;
		}
	}
}
