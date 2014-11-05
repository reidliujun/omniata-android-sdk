package com.omniata.android.sdk;

import org.json.JSONArray;

import android.util.Log;

public class OmniataChannelEngine implements OmniataChannelResponseHandler{
	
	public static JSONArray channelMessage = null;
	private static String TAG = "OmniataChannelEngine";
	
	@Override
	public void onSuccess(int channel, JSONArray content) {
		channelMessage = content;
	}
	

	@Override
	public void onError(int channel, Exception e) {
		Log.i("Omniata Log", "Channel info retrieved failed");
		Log.e("Omniata Log", e.toString());
	}
}
