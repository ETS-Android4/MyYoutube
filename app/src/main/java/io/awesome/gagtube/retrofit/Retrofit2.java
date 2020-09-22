package io.awesome.gagtube.retrofit;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import io.awesome.gagtube.BuildConfig;
import io.awesome.gagtube.R;
import io.awesome.gagtube.util.AppUtils;
import io.awesome.gagtube.util.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

public class Retrofit2 {
	
	private static Retrofit getRetrofit() {
		
		// logging Interceptor when not PRODUCTION
		HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
		loggingInterceptor.level(BuildConfig.DEBUG ? BODY : NONE);
		
		// create OkHttpClient for adding interceptors
		OkHttpClient client = Constants.getInstance().getOkHttpBuilder()
				.addInterceptor(loggingInterceptor)
				.build();
		
		// create Retrofit
		return new Retrofit.Builder().baseUrl(Constants.BASE_URL)
				// gson
				.addConverterFactory(GsonConverterFactory.create())
				// rx-java
				.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
				// interceptors
				.client(client)
				.build();
	}
	
	public static RestApi restApi() {
		
		return getRetrofit().create(RestApi.class);
	}
	
	public static <T> Observable.Transformer<T, T> applySchedulers() {
		
		return observable -> observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}
	
	public static <T> Single.Transformer<T, T> applySingleSchedulers() {
		
		return single -> single.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}
	
	public static void toastError(Context context, Throwable throwable) {
		
		// toast getErrorMessage() below
		Toast.makeText(context, getErrorMessage(context, throwable), Toast.LENGTH_SHORT).show();
	}
	
	public static void snackbarError(Context context, Throwable throwable, View view) {
		
		Snackbar.make(view, Retrofit2.getErrorMessage(context, throwable), Snackbar.LENGTH_SHORT).show();
	}
	
	public static String getErrorMessage(Context context, Throwable throwable) {
		
		String errorMessage;
		
		// if non-200 response
		if (throwable instanceof HttpException) {
			
			// get error Response from HttpException
			Response errorResponse = ((HttpException) throwable).response();
			
			// default to http status reason
			errorMessage = errorResponse.message();
			
			// if has errorBody
			if (errorResponse.errorBody() != null) {
				try {
					// set to errorMessage if not empty
					String errorBodyMessage = errorResponse.errorBody().string();
					if (!TextUtils.isEmpty(errorBodyMessage)) {
						errorMessage = errorBodyMessage;
					}
				}
				catch (IOException ignored) {
				}
			}
		}
		else {
			// default from throwable message
			errorMessage = throwable.getMessage();
			
			// if is network error
			if (throwable instanceof IOException && context != null) {
				
				// online
				if (AppUtils.isOnline(context)) {
					errorMessage = context.getString(R.string.network_error);
				}
				// offline
				else {
					errorMessage = context.getString(R.string.no_network);
				}
			}
		}
		
		return errorMessage;
	}
}
