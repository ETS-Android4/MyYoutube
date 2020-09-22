package io.awesome.gagtube.player;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * Fixes a leak caused by AudioManager using an Activity context.
 * Tracked at https://android-review.googlesource.com/#/c/140481/1 and
 * https://github.com/square/leakcanary/issues/205
 * Source: https://gist.github.com/jankovd/891d96f476f7a9ce24e2
 */

public class AudioServiceLeak extends ContextWrapper {
	
	AudioServiceLeak(Context base) {
		super(base);
	}
	
	public static ContextWrapper preventLeakOf(Context base) {
		return new AudioServiceLeak(base);
	}
	
	@Override
	public Object getSystemService(String name) {
		
		if (Context.AUDIO_SERVICE.equals(name)) {
			return getApplicationContext().getSystemService(name);
		}
		return super.getSystemService(name);
	}
} 