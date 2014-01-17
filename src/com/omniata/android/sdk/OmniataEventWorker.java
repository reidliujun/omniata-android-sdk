package com.omniata.android.sdk;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

class OmniataEventWorker implements Runnable {
	private static final String TAG 			   = "Omniata";
	private static final int    CONNECTION_TIMEOUT = 30 * 1000;
	private static final int    READ_TIMEOUT 	   = 30 * 1000;
	private static final int	SLEEP_TIME		   = 1 * 1000;
	
	private Activity 							activity;
	private int 								connectionTimeout;
	private int 								readTimeout;
	private boolean 							debug;	
	private PersistentBlockingQueue<JSONObject> persistantQueue;
	
	public OmniataEventWorker(Activity activity, PersistentBlockingQueue<JSONObject> persistantQueue, boolean debug) {
		this.activity 		   = activity;
		this.persistantQueue   = persistantQueue;
		this.connectionTimeout = CONNECTION_TIMEOUT;
		this.readTimeout 	   = READ_TIMEOUT;
		this.debug 			   = debug;
	}
	
	@Override
	public void run() {			
		while(true) {
			if (OmniataUtils.isConnected(activity)) {
				processEvents();
			} else {
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
	}
	
	protected void processEvents() {
		try {
			if (sendEvent(persistantQueue.blockingPeek())) {
				persistantQueue.take();						
			} else {
				Thread.sleep(SLEEP_TIME);
			}
		} catch (InterruptedException e) {
			return;
		}
	}
	
	protected boolean sendEvent(JSONObject event) {
		HttpURLConnection connection = null;
		
		try {
			String query    = OmniataUtils.jsonToQueryString(event);
			String eventURL = OmniataUtils.getEventAPI(false, debug) + "?" + query;		
			URL url 		= new URL(eventURL);
			
			connection = (HttpURLConnection)url.openConnection();
			
			connection.setConnectTimeout(connectionTimeout);
			connection.setReadTimeout(readTimeout);
			
			connection.getInputStream();
			
			int httpResponseCode 	   = connection.getResponseCode();
			String httpResponseMessage = connection.getResponseMessage();
			
			Log.d(TAG, "" + httpResponseCode + ": " + httpResponseMessage);
			
			if (httpResponseCode >= 500) {
				// 5xx Server Error
				/* Will retry */
			} else if (httpResponseCode >= 400) {
				// 4xx Client Error
				/* Will not retry */
				return true;
			} else if (httpResponseCode >= 300) {
				// 3xx Redirection
			} else if (httpResponseCode >= 200) {
				// 2xx Success
				return true;
			} else {
				// 1xx Informational
			}		
		} catch (MalformedURLException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		
		return false;
	}
}
