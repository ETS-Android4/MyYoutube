package io.awesome.gultube.util.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import butterknife.ButterKnife;
import io.awesome.gultube.R;

public final class ExitAppDialog extends DialogFragment {
	

	
	private Activity activity;
	private Runnable callback;
	
	public ExitAppDialog(Runnable callback) {
		this.callback = callback;
	}
	
	public static ExitAppDialog newInstance(Runnable callback) {
		return new ExitAppDialog(callback);
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = getActivity();
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		
		View dialogView = View.inflate(activity, R.layout.dialog_exit_app, null);
		ButterKnife.bind(this, dialogView);

		
		@SuppressLint("CheckResult") final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.dialog_exit_app_msg)
				.setView(dialogView)
				.setCancelable(true)
				.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
				.setPositiveButton(R.string.yes, (dialog, which) -> callback.run());
		
		return dialogBuilder.create();
	}
	

}
