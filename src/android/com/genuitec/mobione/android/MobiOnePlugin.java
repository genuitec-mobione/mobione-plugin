/**
 * 
 */
package com.genuitec.mobione.android;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import android.app.Activity;
import android.content.Intent;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.apache.cordova.api.PluginResult.Status;

/**
 * Plug-in that exposes the common MobiOne behaviors to JavaScript.
 * 
 * @author Lonnie Pryor
 */
public class MobiOnePlugin extends Plugin {

	/** The action indexed by name. */
	private final Map<String, Action> actions;

	/** Creates a new MobiOne plug-in. */
	public MobiOnePlugin() {
		Map<String, Action> actions = new HashMap<String, Action>();
		actions.put("useNativeDialPhone".toLowerCase(), new UseNativeDialPhone());
		actions.put("useNativeVideoPlayer".toLowerCase(), new UseNativeVideoPlayer());
		actions.put("playVideo".toLowerCase(), new PlayVideo());
		actions.put("exitApp".toLowerCase(), new ExitApp());
		this.actions = Collections.unmodifiableMap(actions);
	}

	/* @see org.apache.cordova.api.Plugin#execute(String, JSONArray, String) */
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		Action actionObj = null;
		if (action != null)
			actionObj = actions.get(action.toLowerCase());
		if (actionObj == null)
			throw new IllegalArgumentException("Invalid MobiOne action: " + action);
		return actionObj.execute(data, callbackId);
	}

	/**
	 * Base class for individual actions.
	 * 
	 * @author Lonnie Pryor
	 */
	abstract class Action {

		/** Runs this action. */
		abstract PluginResult execute(JSONArray data, String callbackId);

	}

	/**
	 * Action that determines if native tel:// URL activation is required.
	 * 
	 * @author Lonnie Pryor
	 */
	final class UseNativeDialPhone extends Action {
		/* @see com.genuitec.mobione.android.MobiOnePlugin.Action#execute(org.json.JSONArray, java.lang.String) */
		@Override
		PluginResult execute(JSONArray data, String callbackId) {
			return new PluginResult(Status.OK, "false");
		}
	}

	/**
	 * Action that determines if the native video player is required.
	 * 
	 * @author Lonnie Pryor
	 */
	final class UseNativeVideoPlayer extends Action {
		/* @see com.genuitec.mobione.android.MobiOnePlugin.Action#execute(org.json.JSONArray, java.lang.String) */
		@Override
		PluginResult execute(JSONArray data, String callbackId) {
			return new PluginResult(Status.OK, "true");
		}
	}

	/**
	 * Action that plays a video in full-screen mode.
	 * 
	 * @author Lonnie Pryor
	 */
	final class PlayVideo extends Action {
		/* @see com.genuitec.mobione.android.MobiOnePlugin.Action#execute(org.json.JSONArray, java.lang.String) */
		@Override
		PluginResult execute(JSONArray data, String callbackId) {
			String media = null;
			try {
				media = data.getString(0);
			} catch (Exception e) {
				e.printStackTrace();
				return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
			}
			try {
				
				Intent intent = new Intent(cordova.getActivity(), PlayVideoActivity.class);
				intent.putExtra("media", media);
				cordova.getActivity().startActivity(intent);
				
				return new PluginResult(Status.OK);
			} catch (Exception e) {
				return new PluginResult(PluginResult.Status.IO_EXCEPTION);
			}
		}
	}

	/**
	 * Action that terminates the application.
	 * 
	 * @author Lonnie Pryor
	 */
	final class ExitApp extends Action {
		/* @see com.genuitec.mobione.android.MobiOnePlugin.Action#execute(org.json.JSONArray, java.lang.String) */
		@Override
		PluginResult execute(JSONArray data, String callbackId) {
			cordova.getActivity().setResult(Activity.RESULT_OK);
			cordova.getActivity().finish();
			return new PluginResult(Status.OK, "true");
		}
	}

}
