/*
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) 2005-2010, Nitobi Software Inc.
 * Copyright (c) 2010, IBM Corporation
 */
package com.phonegap.plugins.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;

public class Viewer extends CordovaPlugin {

	private class CallbackData {
		private final String uri;
		private final CallbackContext callbackContext;
		private final Runnable cleanup;
		
		public CallbackData(String uri, CallbackContext callbackContext, Runnable cleanup) throws IOException {
			this.uri = uri;
			this.callbackContext = callbackContext;
			this.cleanup = cleanup;
		}
		
		void reportSuccess() {
			i("reportSuccess!");
			JSONObject res = new JSONObject();
			try {
				res.put("file", uri);
			} catch (JSONException e) { /* dummy */ }
			callbackContext.success(res);
		}
		
		void cleanup() {
			i("CallbackData.cleanup for " + uri);
			if (this.cleanup != null) {
				cleanup.run();
			}
		}
	}
	
	private int currentRequest = 0;
	private SparseArray<CallbackData> requests = new SparseArray<CallbackData>();

	private static String TAG = "Viewer Plugin";

	private static void i(String s) {
		Log.i(TAG, s);
	}

	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		
		if (!action.equals("showFile")) {
			return false;
		}
		
		try {			
			String file = Uri.encode(args.getString(0), "/");
			
			Uri uri = Uri.parse(
					new URL(
							new URL(this.webView.getUrl()),
							file).toString());
			
			String mimeType = args.getString(1);
			
			this.showFile(uri, mimeType, callbackContext);

			//PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
			//pluginResult.setKeepCallback(true);
			//return pluginResult;		
		} catch (Throwable e) {
			e.printStackTrace();
			callbackContext.error(e.getMessage());
		}
		return true;
	}
	
	@Override
	public void onDestroy() {
		i("OnDestroy");
		for (int i = 0; i < requests.size(); ++i) {
			int request = requests.keyAt(i);
			CallbackData cd = requests.get(request);
			cd.cleanup();
		}
		requests.clear();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		i("onActivityResult: " + requestCode + ", " + resultCode + ", " + intent);
		
		CallbackData cd = removeCd(requestCode);
		if (cd != null)
			cd.reportSuccess();
	}
		
	synchronized private CallbackData removeCd(int request) {
		CallbackData cd = requests.get(request);
		if (cd != null) {
			requests.delete(request);
			cd.cleanup();
		}
		return cd;
	}
	
	synchronized private int addCd(CallbackData cd) {
		int request = currentRequest++;
		requests.put(request, cd);
		return request;
	}
	
	@SuppressLint({ "WorldReadableFiles", "WorldWriteableFiles" })
	private void showFile(Uri uri, String mimeType, CallbackContext callbackContext) throws Throwable {
		i("showFile: " + uri.toString() + ", mime: " + mimeType);
		
		final String assetPrefix = "/android_asset/";
		
		final Uri realUri;
		final CallbackData cd;
		
		if (uri.getScheme().equals("file") && uri.getPath().startsWith(assetPrefix)) {
			// uri points to a local file in assets/www, lets copy it to world accessible location
			
			String assetFile = uri.getPath().substring(assetPrefix.length());
			i("Opening asset: " + assetFile);
			AssetManager am = this.cordova.getActivity().getAssets();
				
			InputStream is = null;			
			FileOutputStream fos = null;
			File tempFile = null;
			try {
				is = am.open(assetFile);
				String fileName = uri.getLastPathSegment();

				if (PackageManager.PERMISSION_GRANTED == 
						cordova.getActivity().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
					Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					
					File tempDir = new File(Environment.getExternalStorageDirectory(), "Download");
					tempFile = new File(tempDir, fileName);
					fos = new FileOutputStream(tempFile);					
				} else {
					File tempDir = this.cordova.getActivity().getFilesDir();
					tempFile = new File(tempDir, fileName); 
					fos = this.cordova.getActivity().openFileOutput(tempFile.getName(),
						Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE); // to avoid 'read-only file' warnings in some apps
				}
				i("temp file: " + tempFile.getAbsolutePath());
				
				realUri = Uri.fromFile(tempFile);					

				byte[] buffer = new byte[8192];
				int length;
				while ((length = is.read(buffer)) > 0) {
					fos.write(buffer, 0, length);
				}
				fos.close();
			} catch (Throwable e) {
				if (fos != null) {
					fos.close();
					tempFile.delete();
				}
				throw e;
			} finally {
				if (is != null) is.close();
			}
			final File toDelete = tempFile;
			cd = new CallbackData(uri.toString(), callbackContext, new Runnable() {
				@Override
				public void run() {
					i("DELETING " + toDelete);
					toDelete.delete();
				}
			});			
		} else {
			i("Opening uri: " + uri);
			realUri = uri;
			cd = new CallbackData(uri.toString(), callbackContext, null);		
		}
		
		int request = addCd(cd);
		try {			
			final Intent intent = new Intent(Intent.ACTION_VIEW);
			if (mimeType != null) {
				intent.setDataAndType(realUri, mimeType);				
			} else {
				intent.setData(realUri);
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);				
			this.cordova.startActivityForResult(this, intent, request);
			
		} catch (Throwable e) {
			removeCd(request);
			throw e;
		}					
	}
}
