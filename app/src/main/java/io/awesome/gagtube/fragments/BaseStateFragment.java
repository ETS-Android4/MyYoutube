package io.awesome.gagtube.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.jakewharton.rxbinding2.view.RxView;

import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ContentNotSupportedException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import icepick.State;
import io.awesome.gagtube.R;
import io.awesome.gagtube.activities.MainActivity;
import io.awesome.gagtube.activities.ReCaptchaActivity;
import io.awesome.gagtube.base.BaseFragment;
import io.awesome.gagtube.report.ErrorActivity;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.AnimationUtils;
import io.awesome.gagtube.util.ExtractorHelper;
import io.awesome.gagtube.util.InfoCache;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class BaseStateFragment<I> extends BaseFragment implements ViewContract<I> {
	
	@State
	protected AtomicBoolean wasLoading = new AtomicBoolean();
	protected AtomicBoolean isLoading = new AtomicBoolean();
	
	@Nullable
	protected View emptyStateView;
	@Nullable
	protected ProgressBar loadingProgressBar;
	
	protected View errorPanelRoot;
	protected MaterialButton errorButtonRetry;
	protected TextView errorTextView;
	public static boolean isNetworkRetry;
	
	@State
	protected boolean useAsFrontPage = false;
	
	@Override
	public void onViewCreated(@NonNull View rootView, Bundle savedInstanceState) {
		super.onViewCreated(rootView, savedInstanceState);
		doInitialLoadLogic();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		wasLoading.set(isLoading.get());
	}
	
	public void useAsFrontPage(boolean value) {
		useAsFrontPage = value;
	}
	
	// Init
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		super.initViews(rootView, savedInstanceState);
		
		emptyStateView = rootView.findViewById(R.id.empty_state_view);
		loadingProgressBar = rootView.findViewById(R.id.loading_progress_bar);
		
		errorPanelRoot = rootView.findViewById(R.id.error_panel);
		errorButtonRetry = rootView.findViewById(R.id.error_button_retry);
		errorTextView = rootView.findViewById(R.id.error_message_view);
	}
	
	@SuppressLint("CheckResult")
	@Override
	protected void initListeners() {
		super.initListeners();
		RxView.clicks(errorButtonRetry)
				.debounce(300, TimeUnit.MILLISECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(o -> onRetryButtonClicked());
	}
	
	protected void onRetryButtonClicked() {
		isNetworkRetry = true;
		reloadContent();
	}
	
	public void reloadContent() {
		startLoading(true);
	}
	
	// Load
	protected void doInitialLoadLogic() {
		startLoading(true);
	}
	
	protected void startLoading(boolean forceLoad) {
		showLoading();
		isLoading.set(true);
	}
	
	// Contract
	@Override
	public void showLoading() {
		if (emptyStateView != null) AnimationUtils.animateView(emptyStateView, false, 100);
		if (loadingProgressBar != null) AnimationUtils.animateView(loadingProgressBar, true, 200);
		AnimationUtils.animateView(errorPanelRoot, false, 100);
	}
	
	@Override
	public void hideLoading() {
		if (emptyStateView != null) AnimationUtils.animateView(emptyStateView, false, 100);
		if (loadingProgressBar != null) AnimationUtils.animateView(loadingProgressBar, false, 0);
		AnimationUtils.animateView(errorPanelRoot, false, 100);
	}
	
	@Override
	public void showEmptyState() {
		isLoading.set(false);
		if (emptyStateView != null) AnimationUtils.animateView(emptyStateView, true, 100);
		if (loadingProgressBar != null) AnimationUtils.animateView(loadingProgressBar, false, 0);
		AnimationUtils.animateView(errorPanelRoot, false, 100);
	}
	
	@Override
	public void showError(String message, boolean showRetryButton) {
		isLoading.set(false);
		InfoCache.getInstance().clearCache();
		hideLoading();
		
		errorTextView.setText(message);
		if (showRetryButton) AnimationUtils.animateView(errorButtonRetry, true, 300);
		else AnimationUtils.animateView(errorButtonRetry, false, 0);
		AnimationUtils.animateView(errorPanelRoot, true, 100);
	}
	
	@Override
	public void handleResult(I result) {
		hideLoading();
	}
	
	// Error handling
	
	/**
	 * Default implementation handles some general exceptions
	 *
	 * @return if the exception was handled
	 */
	protected boolean onError(Throwable exception) {
		isLoading.set(false);
		
		if (isDetached() || isRemoving()) {
			return true;
		}
		
		if (ExtractorHelper.isInterruptedCaused(exception)) {
			return true;
		}
		
		if (exception instanceof ReCaptchaException) {
			onReCaptchaException((ReCaptchaException) exception);
			return true;
		}
		else if (exception instanceof ContentNotAvailableException) {
			showError(getString(R.string.content_not_available), false);
			return true;
		}
		else if (exception instanceof ContentNotSupportedException) {
			showError(getString(R.string.content_not_supported), false);
			return true;
		}
		else if (exception instanceof IOException) {
			showError(getString(R.string.network_error), true);
			return true;
		}
		
		return false;
	}
	
	public void onReCaptchaException(final ReCaptchaException exception) {
		Toast.makeText(activity, R.string.recaptcha_request_toast, Toast.LENGTH_LONG).show();
		// Open ReCaptcha Challenge Activity
		Intent intent = new Intent(activity, ReCaptchaActivity.class);
		intent.putExtra(ReCaptchaActivity.RECAPTCHA_URL_EXTRA, exception.getUrl());
		startActivityForResult(intent, ReCaptchaActivity.RECAPTCHA_REQUEST);
		
		showError(getString(R.string.recaptcha_request_toast), false);
	}
	
	public void onUnrecoverableError(Throwable exception, UserAction userAction, String serviceName, String request, @StringRes int errorId) {
		onUnrecoverableError(Collections.singletonList(exception), userAction, serviceName, request, errorId);
	}
	
	public void onUnrecoverableError(List<Throwable> exception, UserAction userAction, String serviceName, String request, @StringRes int errorId) {
		
		if (serviceName == null) serviceName = "none";
		if (request == null) request = "none";
		
		ErrorActivity.reportError(getContext(), exception, MainActivity.class, null, ErrorActivity.ErrorInfo.make(userAction, serviceName, request, errorId));
	}
	
	public void showSnackBarError(Throwable exception, UserAction userAction, String serviceName, String request, @StringRes int errorId) {
		showSnackBarError(Collections.singletonList(exception), userAction, serviceName, request, errorId);
	}
	
	/**
	 * Show a SnackBar and only call ErrorActivity#reportError IF we a find a valid view (otherwise the error screen appears)
	 */
	public void showSnackBarError(List<Throwable> exception, UserAction userAction, String serviceName, String request, @StringRes int errorId) {
		
		View rootView = activity != null ? activity.findViewById(android.R.id.content) : null;
		if (rootView == null && getView() != null) rootView = getView();
		if (rootView == null) return;
		
		ErrorActivity.reportError(getContext(), exception, MainActivity.class, rootView,
								  ErrorActivity.ErrorInfo.make(userAction, serviceName, request, errorId));
	}
}
