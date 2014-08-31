package com.genuitec.mobione.android;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;

public class MobiOnePlugin extends CordovaPlugin {
	
	private final Map<String, Action> actions;
	
	public MobiOnePlugin() {
		Map<String, Action> actions = new HashMap<String, Action>();
		actions.put("useNativeDialPhone".toLowerCase(), new UseNativeDialPhone());
		actions.put("useNativeVideoPlayer".toLowerCase(), new UseNativeVideoPlayer());
		actions.put("playVideo".toLowerCase(), new PlayVideo());
		this.actions = Collections.unmodifiableMap(actions);
	}

	abstract class Action {
		abstract void execute(JSONArray data, CallbackContext context);
	}

	final class UseNativeDialPhone extends Action {
		@Override
		void execute(JSONArray data, CallbackContext context) {
			context.success("false");
		}
	}

	final class UseNativeVideoPlayer extends Action {
		@Override
		void execute(JSONArray data, CallbackContext context) {
			context.success("true");
		}
	}
	final class PlayVideo extends Action {
		@Override
		void execute(JSONArray data, CallbackContext context) {
			String media = null;
			try {
				media = data.getString(0);
			} catch (Exception e) {
				e.printStackTrace();
				context.error("JSONException");
				return;
			}
			try {
				
				Intent intent = new Intent(cordova.getActivity(), PlayVideoActivity.class);
				intent.putExtra("media", media);
				cordova.getActivity().startActivity(intent);
				context.success();
			} catch (Exception e) {
				context.error("IOException");
			}
		}
	}
	
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		return super.execute(action, args, callbackContext);
	}
}
