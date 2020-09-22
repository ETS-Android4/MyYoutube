package io.awesome.gagtube.download.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.awesome.gagtube.R;
import io.awesome.gagtube.download.helper.DownloadMission;
import io.awesome.gagtube.download.io.StoredFileHelper;
import io.awesome.gagtube.download.service.DownloadManager;
import io.awesome.gagtube.download.service.DownloadManagerService;
import io.awesome.gagtube.download.service.DownloadManagerService.DownloadManagerBinder;
import io.awesome.gagtube.download.ui.adapter.MissionAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nononsenseapps.filepicker.Utils;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MissionsFragment extends Fragment {
	
	private static final int SPAN_SIZE = 2;
	private static final int REQUEST_DOWNLOAD_SAVE_AS = 0x1230;
	
	private MenuItem mClear = null;
	private MenuItem mStart = null;
	private MenuItem mPause = null;
	
	private RecyclerView mList;
	private View mEmpty;
	private MissionAdapter mAdapter;
	private GridLayoutManager mGridManager;
	private Context mContext;
	
	private DownloadManagerBinder mBinder;
	private boolean mForceUpdate;
	
	private DownloadMission unsafeMissionTarget = null;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mBinder = (DownloadManagerBinder) binder;
			mBinder.clearDownloadNotifications();
			
			mAdapter = new MissionAdapter(mContext, mBinder.getDownloadManager(), mEmpty, getView());
			mAdapter.setRecover(MissionsFragment.this::recoverMission);
			
			setAdapterButtons();
			
			mBinder.addMissionEventListener(mAdapter);
			mBinder.enableNotifications(false);
			
			updateList();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// What to do?
		}
	};
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_missions, container, false);
		
		// Bind the service
		mContext.bindService(new Intent(mContext, DownloadManagerService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		// Views
		mEmpty = v.findViewById(R.id.list_empty_view);
		mList = v.findViewById(R.id.mission_recycler);
		
		// Init layouts managers
		mGridManager = new GridLayoutManager(getActivity(), SPAN_SIZE);
		mGridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				switch (mAdapter.getItemViewType(position)) {
					case DownloadManager.SPECIAL_PENDING:
					case DownloadManager.SPECIAL_FINISHED:
						return SPAN_SIZE;
					default:
						return 1;
				}
			}
		});
		
		setHasOptionsMenu(true);
		
		return v;
	}
	
	/**
	 * Added in API level 23.
	 */
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		
		// Bug: in api< 23 this is never called
		// so mActivity=null
		// so app crashes with null-pointer exception
		mContext = context;
	}
	
	/**
	 * deprecated in API level 23,
	 * but must remain to allow compatibility with api<23
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		
		mContext = activity;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mBinder == null || mAdapter == null) return;
		
		mBinder.removeMissionEventListener(mAdapter);
		mBinder.enableNotifications(true);
		mContext.unbindService(mConnection);
		mAdapter.onDestroy();
		
		mBinder = null;
		mAdapter = null;
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		mClear = menu.findItem(R.id.clear_list);
		mStart = menu.findItem(R.id.start_downloads);
		mPause = menu.findItem(R.id.pause_downloads);
		
		if (mAdapter != null) setAdapterButtons();
		
		super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.clear_list:
				MaterialAlertDialogBuilder prompt = new MaterialAlertDialogBuilder(mContext);
				prompt.setTitle(R.string.clear_download_history);
				prompt.setMessage(R.string.confirm_prompt);
				// Intentionally misusing button's purpose in order to achieve good order
				//prompt.setNegativeButton(R.string.clear_download_history, (dialog, which) -> mAdapter.clearFinishedDownloads(false));
				prompt.setPositiveButton(R.string.delete_downloaded_files, (dialog, which) -> mAdapter.clearFinishedDownloads(true));
				prompt.setNegativeButton(R.string.cancel, null);
				prompt.create().show();
				return true;
			case R.id.start_downloads:
				mBinder.getDownloadManager().startAllMissions();
				return true;
			case R.id.pause_downloads:
				mBinder.getDownloadManager().pauseAllMissions(false);
				mAdapter.refreshMissionItems();// update items view
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void updateList() {
		mList.setLayoutManager(mGridManager);
		// destroy all created views in the recycler
		mList.setAdapter(null);
		mAdapter.notifyDataSetChanged();
		
		// re-attach the adapter in grid/lineal mode
		mList.setAdapter(mAdapter);
	}
	
	private void setAdapterButtons() {
		if (mClear == null || mStart == null || mPause == null) return;
		
		mAdapter.setClearButton(mClear);
		mAdapter.setMasterButtons(mStart, mPause);
	}
	
	private void recoverMission(@NonNull DownloadMission mission) {
		unsafeMissionTarget = mission;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (mAdapter != null) {
			mAdapter.onResume();
			
			if (mForceUpdate) {
				mForceUpdate = false;
				mAdapter.forceUpdate();
			}
			
			mBinder.addMissionEventListener(mAdapter);
			mAdapter.checkMasterButtonsVisibility();
		}
		if (mBinder != null) mBinder.enableNotifications(false);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mAdapter != null) {
			mForceUpdate = true;
			mBinder.removeMissionEventListener(mAdapter);
			mAdapter.onPaused();
		}
		
		if (mBinder != null) mBinder.enableNotifications(true);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode != REQUEST_DOWNLOAD_SAVE_AS || resultCode != Activity.RESULT_OK) return;
		
		if (unsafeMissionTarget == null || data.getData() == null) {
			return;
		}
		
		try {
			Uri fileUri = data.getData();
			if (fileUri.getAuthority() != null) {
				fileUri = Uri.fromFile(Utils.getFileForUri(fileUri));
			}
			
			String tag = unsafeMissionTarget.storage.getTag();
			unsafeMissionTarget.storage = new StoredFileHelper(mContext, null, fileUri, tag);
			mAdapter.recoverMission(unsafeMissionTarget);
		}
		catch (IOException e) {
			Toast.makeText(mContext, R.string.general_error, Toast.LENGTH_LONG).show();
		}
	}
}
