package com.phonegap.plugins.downloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.util.Log;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;

public class Downloader extends Plugin {

	@Override
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		
		if (!action.equals("downloadFile")) 
			return new PluginResult(PluginResult.Status.INVALID_ACTION);
		
		try {
			
			String fileUrl = args.getString(0);
			JSONObject params = args.getJSONObject(1);
			
			String fileName = params.has("fileName") ? 
					params.getString("fileName"):
					fileUrl.substring(fileUrl.lastIndexOf("/")+1);
			
			String dirName = params.has("dirName") ? params.getString("dirName") : null;
					
			Boolean overwrite = params.has("overwrite") ? params.getBoolean("overwrite") : false;
			
			return this.downloadUrl(fileUrl, dirName, fileName, overwrite, callbackId);
			
		} catch (JSONException e) {

			e.printStackTrace();
			return new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage());
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new PluginResult(PluginResult.Status.ERROR, e.getMessage());
		}
	
	}
	
	private File getTempDir() {
		if (PackageManager.PERMISSION_GRANTED == 
				cordova.getActivity().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
			Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			return new File(Environment.getExternalStorageDirectory(), "Download");
		} else {
			return cordova.getActivity().getFilesDir();
		}
	}

	@SuppressLint({ "WorldReadableFiles", "WorldWriteableFiles" })
	private PluginResult downloadUrl(String fileUrl, String dirName, String fileName, Boolean overwrite, String callbackId) throws InterruptedException, JSONException {	
		try {
			final File targetDir;
			if (dirName != null) {
				targetDir = new File(dirName);
				if (!targetDir.exists()) {
					boolean res = targetDir.mkdirs();
					Log.d("PhoneGapLog", "directory " + dirName + " created: " + res);				
				}
			} else {
				targetDir = getTempDir();
				dirName = targetDir.getAbsolutePath();
			}
			File file = new File(targetDir, fileName);
			
			Log.d("PhoneGapLog", "Downloading "+ fileUrl + " into " + file);
			
			if (!overwrite && file.exists()) {
				Log.d("DownloaderPlugin", "File already exist");
				
				JSONObject obj = new JSONObject();
				obj.put("status", 1);
				obj.put("total", 0);
				obj.put("file", fileName);
				obj.put("dir", dirName);
				obj.put("progress", 100);
				
				return new PluginResult(PluginResult.Status.OK, obj);
			}

			URL url = new URL(fileUrl);
			HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
			ucon.setRequestMethod("GET");
			ucon.connect();

			Log.d("PhoneGapLog", "Download start");

			InputStream is = ucon.getInputStream();
			byte[] buffer = new byte[1024];
			int readed = 0, 
			    progress = 0,
			    totalReaded = 0,
			    fileSize = ucon.getContentLength();
			
			// internal app file storage can be used by default or supplied by user, so check this ensure proper file rights
			FileOutputStream fos = 
				cordova.getActivity().getFilesDir().equals(targetDir) ?
					cordova.getActivity().openFileOutput(fileName, Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE) :
					new FileOutputStream(file); 

			try {
				while ((readed = is.read(buffer)) > 0) {
					
					fos.write(buffer, 0, readed);
					totalReaded += readed;
					
					int newProgress = (int) (totalReaded*100/fileSize);				
					if (newProgress != progress)
					 progress = informProgress(fileSize, newProgress, dirName, fileName, callbackId);
	
				}
			} finally {
				is.close();
				fos.close();
			}
			
			Log.d("PhoneGapLog", "Download finished");

			JSONObject obj = new JSONObject();
			obj.put("status", 1);
			obj.put("total", fileSize);
			obj.put("file", fileName);
			obj.put("dir", dirName);
			obj.put("progress", progress);
						
			return new PluginResult(PluginResult.Status.OK, obj);
		}
		catch (FileNotFoundException e) {
			Log.d("PhoneGapLog", "File Not Found: " + e);
			return new PluginResult(PluginResult.Status.ERROR, 404);
		}
		catch (IOException e) {
			Log.d("PhoneGapLog", "Error: " + e);
			return new PluginResult(PluginResult.Status.ERROR, e.getMessage());
		}

	}
	
	private int informProgress(int fileSize, int progress, String dirName, String fileName, String callbackId) throws InterruptedException, JSONException {
		
		JSONObject obj = new JSONObject();
		obj.put("status", 0);
		obj.put("total", fileSize);
		obj.put("file", fileName);
		obj.put("dir", dirName);
		obj.put("progress", progress);
		
		PluginResult res = new PluginResult(PluginResult.Status.OK, obj);
		res.setKeepCallback(true);
		success(res, callbackId);
		
		//Give a chance for the progress to be sent to javascript
		Thread.sleep(100);
		
		return progress; 
	}

}
