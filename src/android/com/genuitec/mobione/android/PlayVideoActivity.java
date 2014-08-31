/**
 * 
 */
package com.genuitec.mobione.android;



import java.io.IOException;


import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;

/**
 * @author Lonnie
 * 
 */
public class PlayVideoActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl {

	private static final String TAG = "VideoPlayer";

	private SurfaceView surface = null;
	private SurfaceHolder holder = null;
	private MediaPlayer mediaPlayer = null;
	private MediaController mediaController = null;
	private Bundle extras = null;
	private Handler handler = new Handler();
	private int videoWidth;
	private int videoHeight;
	private boolean videoSizeKnown = false;
	private boolean videoReadyToBePlayed = false;
	
	private int layoutResource;
	
	int getResource(String type, String name) {
		return getResources().getIdentifier(name, type, getApplicationContext().getPackageName());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.layoutResource = getResource("layout", "play_video");
	
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(layoutResource);
		surface = (SurfaceView) findViewById(getResource("id", "surface"));
		holder = surface.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mediaController = new MediaController(this);
		extras = getIntent().getExtras();
		// text = (TextView)findViewById(R.id.text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		handler.post(new Runnable() {
			public void run() {
				if (videoReadyToBePlayed && videoSizeKnown) {
					View layout = findViewById(getResource("id", "layout"));
					int layoutWidth = layout.getWidth();
					int layoutHeight = layout.getHeight();
					if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && layoutWidth > layoutHeight
							|| newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
							&& layoutWidth < layoutHeight) {
						int tmp = layoutWidth;
						layoutWidth = layoutHeight;
						layoutHeight = tmp;
					}
					resizeHolder(layoutWidth, layoutHeight);
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (mediaPlayer != null)
			mediaPlayer.pause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		videoWidth = 0;
		videoHeight = 0;
		videoReadyToBePlayed = false;
		videoSizeKnown = false;
		super.onDestroy();
		if (mediaController != null)
			mediaController.hide();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mediaController != null)
			mediaController.show();
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		videoWidth = 0;
		videoHeight = 0;
		videoReadyToBePlayed = false;
		videoSizeKnown = false;
		try {
			mediaPlayer = new MediaPlayer();
			Uri uri = Uri.parse(extras.getString("media"));
			if (uri.isRelative()) {
				// AY: XXX: actually we should resolve this url via webview base url, but currently we're doint it in js
				AssetFileDescriptor descriptor = getResources().getAssets().openFd("www/" + uri);
				mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
						descriptor.getLength());				
			} else {
				final String assets = "/android_asset/";
				if (uri.getPath().startsWith(assets)) {
					// this is not FS, we can't use file:// uri
					String path = uri.getPath().substring(assets.length());
					AssetFileDescriptor descriptor = getResources().getAssets().openFd(path);
					Log.d("PlayVideoActivity", "opening " + path);
					mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
							descriptor.getLength());									
				} else {
					mediaPlayer.setDataSource(uri.toString());					
				}
			}
			mediaPlayer.setDisplay(holder);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnVideoSizeChangedListener(this);
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.prepare();
		} catch (IOException e) {
			Log.e(TAG, "Playback failed: " + e.getMessage(), e);
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 */
	@Override
	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mediaController.setMediaPlayer(this);
		mediaController.setAnchorView(findViewById(getResource("id", "layout")));
		handler.post(new Runnable() {
			public void run() {
				mediaController.setEnabled(true);
				mediaController.show();
			}
		});
		videoReadyToBePlayed = true;
		if (videoReadyToBePlayed && videoSizeKnown)
			startVideoPlayback();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.media.MediaPlayer.OnVideoSizeChangedListener#onVideoSizeChanged(android.media.MediaPlayer, int, int)
	 */
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		if (width == 0 || height == 0)
			Log.e(TAG, "Invalid video width (" + width + ") or height (" + height + ").");
		else {
			videoSizeKnown = true;
			videoWidth = width;
			videoHeight = height;
			if (videoReadyToBePlayed && videoSizeKnown)
				startVideoPlayback();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		setResult(Activity.RESULT_OK);
		finish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#start()
	 */
	@Override
	public void start() {
		mediaPlayer.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#pause()
	 */
	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#getDuration()
	 */
	@Override
	public int getDuration() {
		return mediaPlayer.getDuration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#getCurrentPosition()
	 */
	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#seekTo(int)
	 */
	@Override
	public void seekTo(int i) {
		mediaPlayer.seekTo(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#isPlaying()
	 */
	@Override
	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#getBufferPercentage()
	 */
	@Override
	public int getBufferPercentage() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#canPause()
	 */
	@Override
	public boolean canPause() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#canSeekBackward()
	 */
	@Override
	public boolean canSeekBackward() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.MediaController.MediaPlayerControl#canSeekForward()
	 */
	@Override
	public boolean canSeekForward() {
		return true;
	}

	/**
	 * Starts the video play-back.
	 */
	private void startVideoPlayback() {
		View layout = findViewById(getResource("id", "layout"));
		resizeHolder(layout.getWidth(), layout.getHeight());
		mediaPlayer.start();
	}

	/**
	 * Sets the size of the video display.
	 */
	private void resizeHolder(int layoutWidth, int layoutHeight) {
		int holderWidth = layoutWidth;
		int holderHeight = (int) Math.round(holderWidth / (double) videoWidth * videoHeight);
		if (holderHeight > layoutHeight) {
			holderHeight = layoutHeight;
			holderWidth = (int) Math.round(holderHeight / (double) videoHeight * videoWidth);
		}
		// text.setText("(" + videoWidth + " x " + videoHeight + ") :: (" + layoutWidth + " x " + layoutHeight +
		// ") => (" + holderWidth + " x " + holderHeight + ")");
		holder.setFixedSize(holderWidth, holderHeight);
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
