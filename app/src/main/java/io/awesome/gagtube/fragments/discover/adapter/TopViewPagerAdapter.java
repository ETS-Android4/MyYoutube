package io.awesome.gagtube.fragments.discover.adapter;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import io.awesome.gagtube.R;
import io.awesome.gagtube.fragments.discover.TopFragment;

public class TopViewPagerAdapter extends FragmentStatePagerAdapter {
	
	private Context context;
	
	public TopViewPagerAdapter(FragmentManager fm, Context context) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		this.context = context;
	}
	
	@NotNull
	@Override
	public Fragment getItem(int position) {
		
		switch (position) {
			
			case 0:
				return TopFragment.getInstance(10, context.getString(R.string.music));
			
			case 1:
				return TopFragment.getInstance(24, context.getString(R.string.entertainment));
			
			case 2:
				return TopFragment.getInstance(1, context.getString(R.string.films));
			
			case 3:
				return TopFragment.getInstance(20, context.getString(R.string.gaming));
			
			case 4:
				return TopFragment.getInstance(17, context.getString(R.string.sports));
			
			case 5:
				return TopFragment.getInstance(2, context.getString(R.string.cars));
			
			case 6:
				return TopFragment.getInstance(15, context.getString(R.string.pets));
			
			default:
				return new Fragment();
		}
	}
	
	@Override
	public int getCount() {
		return 7;
	}
	
	@Nullable
	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return context.getString(R.string.music);
			
			case 1:
				return context.getString(R.string.entertainment);
			
			case 2:
				return context.getString(R.string.films);
			
			case 3:
				return context.getString(R.string.gaming);
			
			case 4:
				return context.getString(R.string.sports);
			
			case 5:
				return context.getString(R.string.cars);
			
			case 6:
				return context.getString(R.string.pets);
			
			default:
				return "";
		}
	}
}
