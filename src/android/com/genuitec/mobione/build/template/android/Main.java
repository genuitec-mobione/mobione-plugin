package com.genuitec.mobione.build.template.android;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.security.AccessControlException;

import org.apache.cordova.DroidGap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

public class Main extends DroidGap {
	private static final String Tag = "MobiOne-Main";
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Thread.setDefaultUncaughtExceptionHandler(
                new  UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread thread, final Throwable ex) {
						try {
							Looper.prepare();

							Log.e("MobiOne-Main", "Unhandled Exception: " + ex.getMessage());

							String msg = "Oops. Your app is chashing due to unhandled exception: " + ex.getMessage();
							if (ex instanceof java.lang.SecurityException) {
								if (ex instanceof AccessControlException) {
									String permission = AccessControlException.class.cast(ex).getPermission().getName();
									msg = "Application does not have enough permissions for doing: " + permission + ". Bye.";
								} else {
									msg = "Application does not have enough permissions for requested action. Bye.";
								}
								
							}
							new AlertDialog.Builder(Main.this.getActivity()).setTitle("Error").setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									android.os.Process.killProcess(android.os.Process.myPid());
								}
							}).show();

							Looper.loop();
						} catch (Throwable e) {
							Log.e(Tag, "Internal error, terminating the app", e);
							android.os.Process.killProcess(android.os.Process.myPid());							
						}
					}
				});
		
		// AY: Make web view debugging work for debuggable apps
		try {
			if(Build.VERSION.SDK_INT >= 19) { // KITKAT
			    if ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) ) {
			    	Method m = WebView.class.getDeclaredMethod("setWebContentsDebuggingEnabled", Boolean.TYPE);
			    	m.invoke(null, true);
			    	Log.d(Tag, "WebView debugging is enabled");
			    }
			}		
		} catch (Throwable e) {
			Log.e(Tag, "Failed to enable WebView debugging: " + e);
		}
		
		boolean splashIsPresent = false;
		try {
			Drawable splash = getResources().getDrawable(R.drawable.splash);		
			if (splash == null) throw new IllegalStateException();
			super.setIntegerProperty("splashscreen", R.drawable.splash);			
			splashIsPresent = true;
		} catch (Throwable t) {
			// ignore
		}
		String url = "file:///android_asset/www/" + getResources().getString(R.string.start_page);
		if (splashIsPresent) {
			super.loadUrl(url, 10000);			
		} else {
			super.loadUrl(url);						
		}
	}
}