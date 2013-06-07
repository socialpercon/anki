package wei.mark.floatingfolders;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.R;
import com.ichi2.anki.Reviewer;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Utils;

public final class FloatingFolder extends StandOutWindow {
	private static final int APP_SELECTOR_ID = -2;

	private static final int APP_SELECTOR_CODE = 2;
	private static final int APP_SELECTOR_FINISHED_CODE = 3;
	public static final int STARTUP_CODE = 4;
	Reviewer mReviewer;
	PackageManager mPackageManager;
	WindowManager mWindowManager;

	
	int iconSize;
	int squareWidth;

	Animation mFadeOut, mFadeIn;

	public static void showFolders(Context context) {
		sendData(context, FloatingFolder.class, DISREGARD_ID, STARTUP_CODE,
				null, null, DISREGARD_ID);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mPackageManager = getPackageManager();
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mReviewer = new Reviewer();
		Collection col = AnkiDroidApp.getCol();
/*        mSched = col.getSched();
        mCollectionFilename = col.getPath();

        mBaseUrl = Utils.getBaseUrl(col.getMedia().getDir());
*/
		mFadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		mFadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

		int duration = 100;
		mFadeOut.setDuration(duration);
		mFadeIn.setDuration(duration);
	}

	@Override
	public String getAppName() {
		return "Floating Folders";
	}

	@Override
	public int getAppIcon() {
		return R.drawable.ic_launcher;
	}

	@Override
	public void createAndAttachView(final int id, FrameLayout frame) {
		LayoutInflater inflater = LayoutInflater.from(this);

		// choose which type of window to show
		if (APP_SELECTOR_ID == id) {
			final View view = inflater.inflate(R.layout.folder, frame,
					true);
			mReviewer.mMainLayout = view.findViewById(R.id.main_layout);
		} else {
			// id is not app selector
			View view = inflater.inflate(R.layout.folder, frame, true);
		}
	}

	@Override
	public StandOutLayoutParams getParams(int id, Window window) {
		if (APP_SELECTOR_ID == id) {
			return new StandOutLayoutParams(id, 400,
					StandOutLayoutParams.FILL_PARENT,
					StandOutLayoutParams.CENTER, StandOutLayoutParams.TOP);
		} else {
			int width = 400;
			int height = 400;
			return new StandOutLayoutParams(id, width, height, 50, 50);
		}
	}

	@Override
	public int getFlags(int id) {
		if (APP_SELECTOR_ID == id) {
			return super.getFlags(id);
		} else {
			return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
					| StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
					| StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
		}
	}

	@Override
	public void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends StandOutWindow> fromCls, int fromId) {
		switch (requestCode) {
			case APP_SELECTOR_CODE:
				if (APP_SELECTOR_ID == id) {
					// app selector receives data
					Window window = show(APP_SELECTOR_ID);
					window.data.putInt("fromId", fromId);
				}
				break;
			case APP_SELECTOR_FINISHED_CODE:
				final ActivityInfo app = data.getParcelable("app");
				Log.d("FloatingFolder", "Received app: " + app);

				Window window = getWindow(id);
				if (window == null) {
					return;
				}

				ViewGroup flow = (ViewGroup) window.findViewById(R.id.folder);


				break;
			case STARTUP_CODE:
					
					show(DEFAULT_ID);
				break;
		}
	}

	

	private View getAppView(final int id, final ActivityInfo app) {
		LayoutInflater inflater = LayoutInflater.from(this);
		final View frame = inflater.inflate(R.layout.app_square, null);

		frame.setTag(app);

		frame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = mPackageManager
						.getLaunchIntentForPackage(app.packageName);
				startActivity(intent);
			}
		});

		frame.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				ActivityInfo app = (ActivityInfo) v.getTag();
				Log.d("FloatingFolder",
						"Long clicked: " + app.loadLabel(mPackageManager));
				return true;
			}
		});

		ImageView icon = (ImageView) frame.findViewById(R.id.icon);
		icon.setImageDrawable(app.loadIcon(mPackageManager));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				iconSize, iconSize);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		icon.setLayoutParams(params);

		TextView name = (TextView) frame.findViewById(R.id.name);
		name.setText(app.loadLabel(mPackageManager));

		View square = frame.findViewById(R.id.square);
		square.setLayoutParams(new FrameLayout.LayoutParams(squareWidth,
				FrameLayout.LayoutParams.WRAP_CONTENT));

		return frame;
	}

	@Override
	public void onResize(int id, Window window, View view, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			resizeToGridAndSave(id, -1);
		}
	}

	private void resizeToGridAndSave(final int id, final int cols) {
		final Window window = getWindow(id);

		window.post(new Runnable() {

			@Override
			public void run() {
				FlowLayout flow = (FlowLayout) window.findViewById(R.id.folder);


				int columns = cols;

				if (cols == -1) {
					columns = flow.getCols();
				}

				if (columns < 2) {
					columns = 2;
				}


				int width = flow.getLeft()
						+ (((ViewGroup) flow.getParent()).getWidth() - flow
								.getRight()) + columns * squareWidth;
				int height = width;

				StandOutLayoutParams params = window.getLayoutParams();
				params.width = width;
				params.height = height;
				updateViewLayout(id, params);

			}
		});
	}

	@Override
	public boolean onFocusChange(int id, Window window, boolean focus) {
		if (id == APP_SELECTOR_ID && !focus) {
			close(APP_SELECTOR_ID);
			return false;
		}

		return super.onFocusChange(id, window, focus);
	}

	@Override
	public boolean onTouchBody(final int id, final Window window,
			final View view, MotionEvent event) {


		return false;
	}

	public String getPersistentNotificationMessage(int id) {
		return "Click to close all windows.";
	}

	public Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getCloseAllIntent(this, FloatingFolder.class);
	}

}
