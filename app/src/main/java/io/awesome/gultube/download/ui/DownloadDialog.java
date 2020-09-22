package io.awesome.gultube.download.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import icepick.Icepick;
import icepick.State;
import io.awesome.gultube.R;
import io.awesome.gultube.download.helper.MissionRecoveryInfo;
import io.awesome.gultube.download.io.StoredDirectoryHelper;
import io.awesome.gultube.download.io.StoredFileHelper;
import io.awesome.gultube.download.processor.PostProcessing;
import io.awesome.gultube.download.service.DownloadManager;
import io.awesome.gultube.download.service.DownloadManagerService;
import io.awesome.gultube.download.service.MissionState;
import io.awesome.gultube.report.ErrorActivity;
import io.awesome.gultube.report.UserAction;
import io.awesome.gultube.util.FilenameUtils;
import io.awesome.gultube.util.ListHelper;
import io.awesome.gultube.util.PermissionHelper;
import io.awesome.gultube.util.SecondaryStreamHelper;
import io.awesome.gultube.util.StreamItemAdapter;
import io.awesome.gultube.util.StreamItemAdapter.StreamSizeWrapper;
import io.awesome.gultube.util.ThemeHelper;
import io.reactivex.disposables.CompositeDisposable;

public class DownloadDialog extends BottomSheetDialogFragment implements RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {
	private static final String TAG = "DialogFragment";
	
	@State
	protected StreamInfo currentInfo;
	@State
	protected StreamSizeWrapper<AudioStream> wrappedAudioStreams = StreamSizeWrapper.empty();
	@State
	protected StreamSizeWrapper<VideoStream> wrappedVideoStreams = StreamSizeWrapper.empty();
	@State
	protected int selectedVideoIndex = 0;
	@State
	protected int selectedAudioIndex = 0;
	
	private StreamItemAdapter<AudioStream, Stream> audioStreamsAdapter;
	private StreamItemAdapter<VideoStream, AudioStream> videoStreamsAdapter;
	
	private final CompositeDisposable disposables = new CompositeDisposable();
	
	private TextInputEditText nameEditText;
	private Spinner streamsSpinner;
	private RadioGroup radioStreamsGroup;
	
	public static DownloadDialog newInstance(StreamInfo info) {
		DownloadDialog dialog = new DownloadDialog();
		dialog.setInfo(info);
		return dialog;
	}
	
	public static DownloadDialog newInstance(Context context, StreamInfo info) {
		final ArrayList<VideoStream> streamsList = new ArrayList<>(ListHelper.getSortedStreamVideosList(context, info.getVideoStreams(), info.getVideoOnlyStreams(), false));
		final int selectedStreamIndex = ListHelper.getDefaultResolutionIndex(context, streamsList);
		
		final DownloadDialog instance = newInstance(info);
		instance.setVideoStreams(streamsList);
		instance.setSelectedVideoStream(selectedStreamIndex);
		instance.setAudioStreams(info.getAudioStreams());
		
		return instance;
	}
	
	private void setInfo(StreamInfo info) {
		this.currentInfo = info;
	}
	
	public void setAudioStreams(List<AudioStream> audioStreams) {
		setAudioStreams(new StreamSizeWrapper<>(audioStreams, getContext()));
	}
	
	public void setAudioStreams(StreamSizeWrapper<AudioStream> wrappedAudioStreams) {
		this.wrappedAudioStreams = wrappedAudioStreams;
	}
	
	public void setVideoStreams(List<VideoStream> videoStreams) {
		setVideoStreams(new StreamSizeWrapper<>(videoStreams, getContext()));
	}
	
	public void setVideoStreams(StreamSizeWrapper<VideoStream> wrappedVideoStreams) {
		this.wrappedVideoStreams = wrappedVideoStreams;
	}
	
	public void setSelectedVideoStream(int selectedVideoIndex) {
		this.selectedVideoIndex = selectedVideoIndex;
	}
	
	public void setSelectedAudioStream(int selectedAudioIndex) {
		this.selectedAudioIndex = selectedAudioIndex;
	}
	
    /*//////////////////////////////////////////////////////////////////////////
    // LifeCycle
    //////////////////////////////////////////////////////////////////////////*/
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!PermissionHelper.checkStoragePermissions(getActivity(), PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE)) {
			getDialog().dismiss();
			return;
		}
		
		context = getContext();
		
		setStyle(STYLE_NO_TITLE, ThemeHelper.getBottomSheetDialogThem(context));
		Icepick.restoreInstanceState(this, savedInstanceState);
		
		SparseArray<SecondaryStreamHelper<AudioStream>> secondaryStreams = new SparseArray<>(4);
		List<VideoStream> videoStreams = wrappedVideoStreams.getStreamsList();
		
		for (int i = 0; i < videoStreams.size(); i++) {
			if (!videoStreams.get(i).isVideoOnly()) continue;
			AudioStream audioStream = SecondaryStreamHelper.getAudioStreamFor(wrappedAudioStreams.getStreamsList(), videoStreams.get(i));
			
			if (audioStream != null) {
				secondaryStreams.append(i, new SecondaryStreamHelper<>(wrappedAudioStreams, audioStream));
			}
		}
		
		this.videoStreamsAdapter = new StreamItemAdapter<>(context, wrappedVideoStreams, secondaryStreams);
		this.audioStreamsAdapter = new StreamItemAdapter<>(context, wrappedAudioStreams);
		
		Intent intent = new Intent(context, DownloadManagerService.class);
		context.startService(intent);
		
		context.bindService(intent, new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName cname, IBinder service) {
				DownloadManagerService.DownloadManagerBinder mgr = (DownloadManagerService.DownloadManagerBinder) service;
				
				mainStorageAudio = mgr.getMainStorageAudio();
				mainStorageVideo = mgr.getMainStorageVideo();
				downloadManager = mgr.getDownloadManager();
				
				btnDownload.setEnabled(true);
				
				context.unbindService(this);
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				// nothing to do
			}
		}, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.download_dialog, container);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		nameEditText = view.findViewById(R.id.file_name);
		nameEditText.setText(FilenameUtils.createFilename(getContext(), currentInfo.getName()));
		selectedAudioIndex = ListHelper.getDefaultAudioFormat(getContext(), currentInfo.getAudioStreams());
		
		streamsSpinner = view.findViewById(R.id.quality_spinner);
		streamsSpinner.setOnItemSelectedListener(this);
		
		radioStreamsGroup = view.findViewById(R.id.video_audio_group);
		radioStreamsGroup.setOnCheckedChangeListener(this);
		
		btnDownload = view.findViewById(R.id.btn_download);
		btnDownload.setEnabled(false);
		
		setupDownloadOptions();
		fetchStreamsSize();
		
		// download
		btnDownload.setOnClickListener(v -> prepareSelectedDownload());
		
		view.findViewById(R.id.container).setOnClickListener(v -> hideKeyboard());
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(nameEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}
	
	private void fetchStreamsSize() {
		disposables.clear();
		
		disposables.add(StreamSizeWrapper.fetchSizeForWrapper(wrappedVideoStreams).subscribe(result -> {
			if (radioStreamsGroup.getCheckedRadioButtonId() == R.id.video_button) {
				setupVideoSpinner();
			}
		}));
		disposables.add(StreamSizeWrapper.fetchSizeForWrapper(wrappedAudioStreams).subscribe(result -> {
			if (radioStreamsGroup.getCheckedRadioButtonId() == R.id.audio_button) {
				setupAudioSpinner();
			}
		}));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		disposables.clear();
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}
	
	/*//////////////////////////////////////////////////////////////////////////
	// Inits
	//////////////////////////////////////////////////////////////////////////*/
	private void setupAudioSpinner() {
		if (getContext() == null) return;
		
		streamsSpinner.setAdapter(audioStreamsAdapter);
		streamsSpinner.setSelection(selectedAudioIndex);
		setRadioButtonsState(true);
	}
	
	private void setupVideoSpinner() {
		if (getContext() == null) return;
		
		streamsSpinner.setAdapter(videoStreamsAdapter);
		streamsSpinner.setSelection(selectedVideoIndex);
		setRadioButtonsState(true);
	}
	
    /*//////////////////////////////////////////////////////////////////////////
    // Radio group Video&Audio options - Listener
    //////////////////////////////////////////////////////////////////////////*/
	
	@Override
	public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
		switch (checkedId) {
			case R.id.audio_button:
				setupAudioSpinner();
				break;
			case R.id.video_button:
				setupVideoSpinner();
				break;
		}
	}

    /*//////////////////////////////////////////////////////////////////////////
    // Streams Spinner Listener
    //////////////////////////////////////////////////////////////////////////*/
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		switch (radioStreamsGroup.getCheckedRadioButtonId()) {
			case R.id.audio_button:
				selectedAudioIndex = position;
				break;
			case R.id.video_button:
				selectedVideoIndex = position;
				break;
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/
	
	protected void setupDownloadOptions() {
		setRadioButtonsState(false);
		
		final RadioButton audioButton = radioStreamsGroup.findViewById(R.id.audio_button);
		final RadioButton videoButton = radioStreamsGroup.findViewById(R.id.video_button);
		final boolean isVideoStreamsAvailable = videoStreamsAdapter.getCount() > 0;
		final boolean isAudioStreamsAvailable = audioStreamsAdapter.getCount() > 0;
		
		audioButton.setVisibility(isAudioStreamsAvailable ? View.VISIBLE : View.GONE);
		videoButton.setVisibility(isVideoStreamsAvailable ? View.VISIBLE : View.GONE);
		
		if (isVideoStreamsAvailable) {
			videoButton.setChecked(true);
			setupVideoSpinner();
		}
		else if (isAudioStreamsAvailable) {
			audioButton.setChecked(true);
			setupAudioSpinner();
		}
		else {
			Toast.makeText(getContext(), R.string.no_streams_available_download, Toast.LENGTH_SHORT).show();
			getDialog().dismiss();
		}
	}
	
	private void setRadioButtonsState(boolean enabled) {
		radioStreamsGroup.findViewById(R.id.audio_button).setEnabled(enabled);
		radioStreamsGroup.findViewById(R.id.video_button).setEnabled(enabled);
	}
	
	private StoredDirectoryHelper mainStorageAudio = null;
	private StoredDirectoryHelper mainStorageVideo = null;
	private DownloadManager downloadManager = null;
	private MaterialButton btnDownload = null;
	private Context context;
	
	private String getNameEditText() {
		String str = nameEditText.getText().toString().trim();
		return FilenameUtils.createFilename(context, str.isEmpty() ? currentInfo.getName() : str);
	}
	
	private void showFailedDialog(@StringRes int msg) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.general_error)
				.setMessage(msg)
				.setNegativeButton(getString(R.string.ok), null)
				.create()
				.show();
	}
	
	private void showErrorActivity(Exception e) {
		ErrorActivity.reportError(
				context,
				Collections.singletonList(e),
				null,
				null,
				ErrorActivity.ErrorInfo.make(UserAction.SOMETHING_ELSE, "-", "-", R.string.general_error)
		);
	}
	
	private void prepareSelectedDownload() {
		StoredDirectoryHelper mainStorage;
		MediaFormat format;
		String mime;
		
		// first, build the filename and get the output folder (if possible)
		// later, run a very very very large file checking logic
		
		String filename = getNameEditText().concat(".");
		
		switch (radioStreamsGroup.getCheckedRadioButtonId()) {
			case R.id.audio_button:
				mainStorage = mainStorageAudio;
				format = audioStreamsAdapter.getItem(selectedAudioIndex).getFormat();
				if (format == MediaFormat.WEBMA_OPUS) {
					mime = "audio/ogg";
					filename += "opus";
				}
				else {
					mime = format.mimeType;
					filename += format.suffix;
				}
				break;
			case R.id.video_button:
				mainStorage = mainStorageVideo;
				format = videoStreamsAdapter.getItem(selectedVideoIndex).getFormat();
				mime = format.mimeType;
				filename += format.suffix;
				break;
			default:
				throw new RuntimeException("No stream selected");
		}
		
		// check for existing file with the same name
		checkSelectedDownload(mainStorage, mainStorage.findFile(filename), filename, mime);
	}
	
	private void checkSelectedDownload(StoredDirectoryHelper mainStorage, Uri targetFile, String filename, String mime) {
		StoredFileHelper storage;
		
		try {
			if (mainStorage == null) {
				// using SAF on older android version
				storage = new StoredFileHelper(context, null, targetFile, "");
			} else if (targetFile == null) {
				// the file does not exist, but it is probably used in a pending download
				storage = new StoredFileHelper(mainStorage.getUri(), filename, mime, mainStorage.getTag());
			} else {
				// the target filename is already use, attempt to use it
				storage = new StoredFileHelper(context, mainStorage.getUri(), targetFile, mainStorage.getTag());
			}
		} catch (Exception e) {
			showErrorActivity(e);
			return;
		}
		
		// check if is our file
		MissionState state = downloadManager.checkForExistingMission(storage);
		@StringRes int msgBtn;
		@StringRes int msgBody;
		
		switch (state) {
			case Finished:
				msgBtn = R.string.overwrite;
				msgBody = R.string.overwrite_finished_warning;
				break;
			case Pending:
				msgBtn = R.string.overwrite;
				msgBody = R.string.download_already_pending;
				break;
			case PendingRunning:
				msgBtn = R.string.generate_unique_name;
				msgBody = R.string.download_already_running;
				break;
			case None:
				if (mainStorage == null) {
					// This part is called if:
					// * using SAF on older android version
					// * save path not defined
					// * if the file exists overwrite it, is not necessary ask
					if (!storage.existsAsFile() && !storage.create()) {
						showFailedDialog(R.string.error_file_creation);
						return;
					}
					continueSelectedDownload(storage);
					return;
				} else if (targetFile == null) {
					// This part is called if:
					// * the filename is not used in a pending/finished download
					// * the file does not exists, create
					
					if (!mainStorage.mkdirs()) {
						showFailedDialog(R.string.error_path_creation);
						return;
					}
					
					storage = mainStorage.createFile(filename, mime);
					if (storage == null || !storage.canWrite()) {
						showFailedDialog(R.string.error_file_creation);
						return;
					}
					
					continueSelectedDownload(storage);
					return;
				}
				msgBtn = R.string.overwrite;
				msgBody = R.string.overwrite_unrelated_warning;
				break;
			default:
				return;
		}
		
		
		AlertDialog.Builder askDialog = new AlertDialog.Builder(context)
				.setTitle(R.string.download)
				.setMessage(msgBody)
				.setNegativeButton(android.R.string.cancel, null);
		final StoredFileHelper finalStorage = storage;
		
		
		if (mainStorage == null) {
			// This part is called if:
			// * using SAF on older android version
			// * save path not defined
			switch (state) {
				case Pending:
				case Finished:
					askDialog.setPositiveButton(msgBtn, (dialog, which) -> {
						dialog.dismiss();
						downloadManager.forgetMission(finalStorage);
						continueSelectedDownload(finalStorage);
					});
					break;
			}
			
			askDialog.create().show();
			return;
		}
		
		askDialog.setPositiveButton(msgBtn, (dialog, which) -> {
			dialog.dismiss();
			
			StoredFileHelper storageNew;
			switch (state) {
				case Finished:
				case Pending:
					downloadManager.forgetMission(finalStorage);
				case None:
					if (targetFile == null) {
						storageNew = mainStorage.createFile(filename, mime);
					} else {
						try {
							// try take (or steal) the file
							storageNew = new StoredFileHelper(context, mainStorage.getUri(), targetFile, mainStorage.getTag());
						} catch (IOException e) {
							Log.e(TAG, "Failed to take (or steal) the file in " + targetFile.toString());
							storageNew = null;
						}
					}
					
					if (storageNew != null && storageNew.canWrite())
						continueSelectedDownload(storageNew);
					else
						showFailedDialog(R.string.error_file_creation);
					break;
				case PendingRunning:
					storageNew = mainStorage.createUniqueFile(filename, mime);
					if (storageNew == null)
						showFailedDialog(R.string.error_file_creation);
					else
						continueSelectedDownload(storageNew);
					break;
			}
		});
		
		askDialog.create().show();
	}
	
	private void continueSelectedDownload(@NonNull StoredFileHelper storage) {
		if (!storage.canWrite()) {
			showFailedDialog(R.string.permission_denied);
			return;
		}
		
		// check if the selected file has to be overwritten, by simply checking its length
		try {
			if (storage.length() > 0) storage.truncate();
		}
		catch (IOException e) {
			Log.e(TAG, "failed to truncate the file: " + storage.getUri().toString(), e);
			showFailedDialog(R.string.overwrite_failed);
			return;
		}
		
		Stream selectedStream;
		Stream secondaryStream = null;
		char kind;
		String[] urls;
		MissionRecoveryInfo[] recoveryInfo;
		String psName = null;
		String[] psArgs = null;
		long nearLength = 0;
		
		// more download logic: select muxer, subtitle converter, etc.
		switch (radioStreamsGroup.getCheckedRadioButtonId()) {
			case R.id.audio_button:
				kind = 'a';
				selectedStream = audioStreamsAdapter.getItem(selectedAudioIndex);
				
				if (selectedStream.getFormat() == MediaFormat.M4A) {
					psName = PostProcessing.ALGORITHM_M4A_NO_DASH;
				}
				else if (selectedStream.getFormat() == MediaFormat.WEBMA_OPUS) {
					psName = PostProcessing.ALGORITHM_OGG_FROM_WEBM_DEMUXER;
				}
				break;
			case R.id.video_button:
				kind = 'v';
				selectedStream = videoStreamsAdapter.getItem(selectedVideoIndex);
				
				SecondaryStreamHelper<AudioStream> secondary = videoStreamsAdapter
						.getAllSecondary()
						.get(wrappedVideoStreams.getStreamsList().indexOf(selectedStream));
				
				if (secondary != null) {
					secondaryStream = secondary.getStream();
					
					if (selectedStream.getFormat() == MediaFormat.MPEG_4)
						psName = PostProcessing.ALGORITHM_MP4_FROM_DASH_MUXER;
					else
						psName = PostProcessing.ALGORITHM_WEBM_MUXER;
					
					psArgs = null;
					long videoSize = wrappedVideoStreams.getSizeInBytes((VideoStream) selectedStream);
					
					// set nearLength, only, if both sizes are fetched or known. This probably
					// does not work on slow networks but is later updated in the downloader
					if (secondary.getSizeInBytes() > 0 && videoSize > 0) {
						nearLength = secondary.getSizeInBytes() + videoSize;
					}
				}
				break;
			default:
				return;
		}
		
		if (secondaryStream == null) {
			urls = new String[]{selectedStream.getUrl()};
			recoveryInfo = new MissionRecoveryInfo[]{new MissionRecoveryInfo(selectedStream)};
		}
		else {
			urls = new String[]{selectedStream.getUrl(), secondaryStream.getUrl()};
			recoveryInfo = new MissionRecoveryInfo[]{new MissionRecoveryInfo(selectedStream), new MissionRecoveryInfo(secondaryStream)};
		}
		
		DownloadManagerService.startMission(context, urls, storage, kind, 50, currentInfo.getUrl(), psName, psArgs, nearLength, recoveryInfo);
		dismiss();
	}
}
