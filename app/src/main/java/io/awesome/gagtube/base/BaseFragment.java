package io.awesome.gagtube.base;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import icepick.Icepick;

public abstract class BaseFragment extends Fragment {
	
	protected BaseActivity activity;
	
	// Fragment's Lifecycle
	@Override
	public void onAttach(@NotNull Context context) {
		
		super.onAttach(context);
		
		activity = (BaseActivity) context;
	}
	
	@Override
	public void onDetach() {
		
		super.onDetach();
		
		activity = null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		Icepick.restoreInstanceState(this, savedInstanceState);
		if (savedInstanceState != null) onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public void onViewCreated(@NonNull View rootView, Bundle savedInstanceState) {
		
		super.onViewCreated(rootView, savedInstanceState);
		
		initViews(rootView, savedInstanceState);
		initListeners();
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		
		super.onSaveInstanceState(outState);
		
		Icepick.saveInstanceState(this, outState);
	}
	
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	// Init
	protected void initViews(View rootView, Bundle savedInstanceState) {
	}
	
	protected void initListeners() {
	}
	
	// Utils
	public void setTitle(String title) {
		
		if (activity != null && activity.getSupportActionBar() != null) {
			activity.getSupportActionBar().setTitle(title);
		}
	}
}
