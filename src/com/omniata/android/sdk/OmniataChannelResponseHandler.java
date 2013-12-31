package com.omniata.android.sdk;

import org.json.JSONArray;

public interface OmniataChannelResponseHandler {
	public void onSuccess(int channel, JSONArray content);
	public void onError(int channel, Exception e);
}
