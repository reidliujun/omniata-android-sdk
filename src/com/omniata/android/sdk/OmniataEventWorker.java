package com.omniata.android.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

class OmniataEventWorker implements Runnable {
	private static final String TAG 			      	= "OmniataEventWorker";
	private static final int    SECONDS 				= 1000;
	private static final int    CONNECTION_TIMEOUT 		= 30 * SECONDS;
	private static final int    READ_TIMEOUT 	   		= 30 * SECONDS;
	private static final int	RETRY_CONNECTIVITY_TIME = 16 * SECONDS;
	private static final int	MIN_TIME_BETWEEN_EVENTS = 1  * SECONDS;
	private static final int    MAX_BACKOFF_EXP         = 9;				// 2^9 = 512 Seconds ~ 8 minutes
	private static final int    MAX_RETRIES             = 30;

	private Context 							context;
	private int 								connectionTimeout;
	private int 								readTimeout;
	private boolean 							debug;	
	private PersistentBlockingQueue<JSONObject> eventLog;
	private int									retries;
	private Thread								worker;
	private boolean								isRunning;
	private boolean								isStarted;
	
	enum EventStatus {
		SUCCESS,
		RETRY,
		DISCARD
	};

	public OmniataEventWorker(Context context, PersistentBlockingQueue<JSONObject> eventLog) {
		this.context 		   = context;
		this.eventLog   	   = eventLog;
		this.connectionTimeout = CONNECTION_TIMEOUT;
		this.readTimeout 	   = READ_TIMEOUT;
		this.debug 			   = false;
		this.retries		   = 0;
		this.worker            = new Thread(this);
	}
	
	public void start() {
		if (!isStarted) {
			this.worker.start();
			isStarted = true;
		}
	}

	/**
	 * Returns the amount of time thread should sleep before attempting to resend.
	 * Will back off exponentially to prevent pegging servers in case of downtime
	 */
	protected int sleepTime() {
		// We'll cap the retry sleep time to a maximum of ~8 minutes, fixes OP-1618
		return (1 << Math.min(MAX_BACKOFF_EXP, retries)) * SECONDS;
	}

	/**
	 * Causes thread to sleep based on retry count

	 */
	protected void throttle() throws InterruptedException {
		int timeSleepMS = sleepTime();
		sleep(timeSleepMS);
	}

	protected void sleep(int timeMS) throws InterruptedException {
		OmniataLog.i(TAG, "Retrying in " + timeMS + "ms");
		Thread.sleep(timeMS);
	}

	@Override
	public void run() {
		OmniataLog.i(TAG, "Thread begin");
		isRunning = true;
		try {
			while(isRunning) {
				OmniataLog.v(TAG, "Thread running: " + Thread.currentThread().getId());
				// Check for network connectivity prior to processing events
				if (OmniataUtils.isConnected(context)) {
					OmniataLog.v(TAG, "Connection available");
					processEvents();
				} else {
					OmniataLog.v(TAG, "Connection unavailable");
					sleep(RETRY_CONNECTIVITY_TIME);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		OmniataLog.i(TAG, "Thread done");
	}

	protected void processEvents() throws InterruptedException {
		long now = System.currentTimeMillis();
		
		JSONObject event = eventLog.blockingPeek();
		
		// Events are stored on the servers on one second precision. Waiting here
		// assures each event has a different timestamp. Different timestamp is needed
		// for reliable sorting of events (by timestamp).
		long timeToWait = MIN_TIME_BETWEEN_EVENTS - (System.currentTimeMillis() - now);
		if (timeToWait > 0) {
			Thread.sleep(timeToWait);
		}

		switch(sendEvent(event)) {
		case RETRY:
			retries++;
			if (retries < MAX_RETRIES) {
				throttle();
				break;
			} else {
				// Fall through
			}
			
		case SUCCESS:
		case DISCARD:
			retries = 0;
			eventLog.take();
			break;
		}
	}

	/**
	 * Attempts to send an event
	 * @param event
	 * @return false if retry should be attempted
	 */	
	protected EventStatus sendEvent(JSONObject event) {
		HttpURLConnection connection = null;

		try {
			// om_delta
			try {
				long creationTime = event.getLong("om_creation_time");
				long omDelta = (System.currentTimeMillis() - creationTime) / 1000;
				event.put("om_delta", omDelta);
				event.remove("om_creation_time");
			} catch (JSONException e) {
				OmniataLog.e(TAG, e.toString());
			}
			
			String query    = OmniataUtils.jsonToQueryString(event);
			String eventURL = OmniataUtils.getEventAPI(true, debug) + "?" + query;
			
			OmniataLog.i(TAG, "Calling event endpoint: " + eventURL);
			Log.i(TAG,"Calling event endpoint: " + eventURL);
			URL url = new URL(eventURL);

			connection = (HttpURLConnection)url.openConnection();

			connection.setConnectTimeout(connectionTimeout);
			connection.setReadTimeout(readTimeout);

			InputStream is = connection.getInputStream();
			int httpResponseCode 	   = connection.getResponseCode();
			String httpResponseMessage = connection.getResponseMessage();

			OmniataLog.d(TAG, "" + httpResponseCode + ": " + httpResponseMessage);

			// Read & ignore the response. It's a good practise to read, from the server's 
			// point of view it's cleaner when the client reads the response before
			// closing the connection.s
			@SuppressWarnings("unused")
			int bytesRead = -1;
			byte[] buffer = new byte[64];
			while ((bytesRead = is.read(buffer)) >= 0) {}

			// 5xx Server Error
			if (httpResponseCode >= 500) { 
				/* Will retry */
				return EventStatus.RETRY;
			} 
			// 4xx Client Error
			else if (httpResponseCode >= 400) {
				return EventStatus.DISCARD;
			} 
			// 3xx Redirection
			else if (httpResponseCode >= 300) {
				if (httpResponseCode == 304) {
					return EventStatus.SUCCESS;
				} else {
					return EventStatus.DISCARD;
				}
			} 
			// 2xx Success
			else if (httpResponseCode >= 200) {
				return EventStatus.SUCCESS;
			} 
			// 1xx Informational
			else {
				return EventStatus.DISCARD;
			}		
		} catch (MalformedURLException e) {
			OmniataLog.e(TAG, e.toString());
			return EventStatus.DISCARD;
		} catch (IOException e) {
			OmniataLog.e(TAG, e.toString());
			return EventStatus.RETRY;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
