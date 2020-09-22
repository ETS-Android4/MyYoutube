package io.awesome.gagtube.adsmanager.nativead;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import io.awesome.gagtube.R;

/**
 * Base class for a template view. *
 */
public class NativeAdView extends FrameLayout {
	
	private int templateType;
	private NativeAdStyle styles;
	private UnifiedNativeAd nativeAd;
	private UnifiedNativeAdView nativeAdView;
	
	private ImageView iconView;
	private TextView primaryView;
	private TextView secondaryView;
	private TextView tertiaryView;
	private RatingBar ratingBar;
	private MediaView mediaView;
	private Button callToActionView;
	private ConstraintLayout background;
	
	private static final String MEDIUM_TEMPLATE = "medium_template";
	private static final String SMALL_TEMPLATE = "small_template";
	
	public NativeAdView(Context context) {
		super(context);
	}
	
	public NativeAdView(Context context, @Nullable AttributeSet attrs) {
		
		super(context, attrs);
		initView(context, attrs);
	}
	
	public NativeAdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		
		super(context, attrs, defStyleAttr);
		initView(context, attrs);
	}
	
	public NativeAdView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		
		super(context, attrs, defStyleAttr, defStyleRes);
		initView(context, attrs);
	}
	
	public void setStyles(NativeAdStyle styles) {
		this.styles = styles;
		this.applyStyles();
	}
	
	public UnifiedNativeAdView getNativeAdView() {
		return nativeAdView;
	}
	
	private void applyStyles() {
		
		Drawable mainBackground = styles.getMainBackgroundColor();
		if (mainBackground != null) {
			background.setBackground(mainBackground);
			if (primaryView != null) {
				primaryView.setBackground(mainBackground);
			}
			if (tertiaryView != null) {
				tertiaryView.setBackground(mainBackground);
			}
		}
		
		Typeface primary = styles.getPrimaryTextTypeface();
		if (primary != null && primaryView != null) {
			primaryView.setTypeface(primary);
		}
		
		Typeface tertiary = styles.getTertiaryTextTypeface();
		if (tertiary != null && tertiaryView != null) {
			tertiaryView.setTypeface(tertiary);
		}
		
		Typeface ctaTypeface = styles.getCallToActionTextTypeface();
		if (ctaTypeface != null && callToActionView != null) {
			callToActionView.setTypeface(ctaTypeface);
		}
		
		int primaryTypefaceColor = styles.getPrimaryTextTypefaceColor();
		if (primaryTypefaceColor > 0 && primaryView != null) {
			primaryView.setTextColor(primaryTypefaceColor);
		}
		
		int tertiaryTypefaceColor = styles.getTertiaryTextTypefaceColor();
		if (tertiaryTypefaceColor > 0 && tertiaryView != null) {
			tertiaryView.setTextColor(tertiaryTypefaceColor);
		}
		
		int ctaTypefaceColor = styles.getCallToActionTypefaceColor();
		if (ctaTypefaceColor > 0 && callToActionView != null) {
			callToActionView.setTextColor(ctaTypefaceColor);
		}
		
		float ctaTextSize = styles.getCallToActionTextSize();
		if (ctaTextSize > 0 && callToActionView != null) {
			callToActionView.setTextSize(ctaTextSize);
		}
		
		float primaryTextSize = styles.getPrimaryTextSize();
		if (primaryTextSize > 0 && primaryView != null) {
			primaryView.setTextSize(primaryTextSize);
		}
		
		float tertiaryTextSize = styles.getTertiaryTextSize();
		if (tertiaryTextSize > 0 && tertiaryView != null) {
			tertiaryView.setTextSize(tertiaryTextSize);
		}
		
		Drawable ctaBackground = styles.getCallToActionBackgroundColor();
		if (ctaBackground != null && callToActionView != null) {
			callToActionView.setBackground(ctaBackground);
		}
		
		Drawable primaryBackground = styles.getPrimaryTextBackgroundColor();
		if (primaryBackground != null && primaryView != null) {
			primaryView.setBackground(primaryBackground);
		}
		
		Drawable tertiaryBackground = styles.getTertiaryTextBackgroundColor();
		if (tertiaryBackground != null && tertiaryView != null) {
			tertiaryView.setBackground(tertiaryBackground);
		}
		
		invalidate();
		requestLayout();
	}
	
	public void setNativeAd(UnifiedNativeAd nativeAd) {
		
		this.nativeAd = nativeAd;
		String store = nativeAd.getStore();
		String advertiser = nativeAd.getAdvertiser();
		String headline = nativeAd.getHeadline();
		String body = nativeAd.getBody();
		String price = nativeAd.getPrice();
		String cta = nativeAd.getCallToAction();
		Double starRating = nativeAd.getStarRating();
		Image icon = nativeAd.getIcon();
		
		String secondaryText;
		
		nativeAdView.setCallToActionView(callToActionView);
		nativeAdView.setHeadlineView(primaryView);
		nativeAdView.setMediaView(mediaView);
		mediaView.setMediaContent(nativeAd.getMediaContent());
		mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
		
		primaryView.setText(headline);
		callToActionView.setText(cta);
		
		secondaryView.setVisibility(VISIBLE);
		if (!TextUtils.isEmpty(advertiser)) {
			nativeAdView.setAdvertiserView(secondaryView);
			secondaryText = advertiser;
		}
		else if (!TextUtils.isEmpty(price)) {
			secondaryText = TextUtils.equals(price, "0.0") ? "FREE" : price;
		}
		else {
			secondaryText = "Sponsored";
		}
		
		//  Set the secondary view to be the star rating if available.
		if (ratingBar != null) {
			if (starRating != null && starRating > 0) {
				ratingBar.setVisibility(VISIBLE);
				ratingBar.setRating(starRating.floatValue());
				nativeAdView.setStarRatingView(ratingBar);
			}
			else {
				ratingBar.setVisibility(GONE);
			}
		}
		
		if (!TextUtils.isEmpty(secondaryText)) {
			secondaryView.setText(secondaryText);
		}
		
		if (tertiaryView != null) {
			if (!TextUtils.isEmpty(body)) {
				tertiaryView.setVisibility(VISIBLE);
				tertiaryView.setText(body);
				nativeAdView.setBodyView(tertiaryView);
			}
			else {
				tertiaryView.setVisibility(GONE);
			}
		}
		
		if (iconView != null) {
			if (icon != null) {
				iconView.setVisibility(VISIBLE);
				iconView.setImageDrawable(icon.getDrawable());
			}
			else {
				iconView.setVisibility(GONE);
			}
		}
		
		nativeAdView.setNativeAd(nativeAd);
	}
	
	public UnifiedNativeAd getNativeAd() {
		return nativeAd;
	}
	
	/**
	 * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
	 * method does not destroy the template view.
	 * https://developers.google.com/admob/android/native-unified#destroy_ad
	 */
	public void destroyNativeAd() {
		
		if (nativeAd != null) {
			nativeAd.destroy();
		}
	}
	
	public String getTemplateTypeName() {
		
		if (templateType == R.layout.native_ad_medium) {
			return MEDIUM_TEMPLATE;
		}
		else if (templateType == R.layout.native_ad_small_list) {
			return SMALL_TEMPLATE;
		}
		return "";
	}
	
	private void initView(Context context, AttributeSet attributeSet) {
		
		TypedArray attributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.NativeAdView, 0, 0);
		
		try {
			templateType = attributes.getResourceId(R.styleable.NativeAdView_native_ad_type, R.layout.native_ad_small_list);
		}
		finally {
			attributes.recycle();
		}
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (inflater != null) {
			inflater.inflate(templateType, this);
		}
	}
	
	@Override
	public void onFinishInflate() {
		
		super.onFinishInflate();
		
		nativeAdView = findViewById(R.id.native_ad_view);
		primaryView = findViewById(R.id.primary);
		secondaryView = findViewById(R.id.secondary);
		tertiaryView = findViewById(R.id.body);
		
		ratingBar = findViewById(R.id.rating_bar);
		if (ratingBar != null) {
			ratingBar.setEnabled(false);
		}
		
		callToActionView = findViewById(R.id.cta);
		iconView = findViewById(R.id.icon);
		mediaView = findViewById(R.id.media_view);
		background = findViewById(R.id.background);
	}
	
	private boolean adHasOnlyStore(UnifiedNativeAd nativeAd) {
		String store = nativeAd.getStore();
		String advertiser = nativeAd.getAdvertiser();
		return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser);
	}
}
