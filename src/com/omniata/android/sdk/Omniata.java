package com.omniata.android.sdk;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.provider.Settings;

public class Omniata {
	
	private static final String TAG       = "Omniata";
	private static final String EVENT_LOG = "events";
	private static final String VERSION   = "1.1.0"; // TODO: Track SDK version
	
	private static Omniata instance;
	
	/**
	 * Initializes the Omniata API
	 * 
	 * @param activity
	 * @param apiKey	The api-key
	 * @param userID	The user-id
	 * @param debug 	True if events should be tracked against the event-monitor
	 */
	public static void initialize(Activity activity, String apiKey, String userID, boolean debug) {
		synchronized(Omniata.class) {	
			if (instance == null) {
				OmniataLog.i(TAG, "Initializing Omniata API");
				instance = new Omniata(activity, apiKey, userID, debug);
			}
			
			/*
			 * Since this singleton may persist across application launches
			 * we need to support re-initialization of the SDK
			 */
			instance._initialize(activity, apiKey, userID, debug);
		}
	}
	
	public static void setLogLevel(int priority) {
		OmniataLog.setPriority(priority);
	}
	
	/**
	 * Initializes the Omniata API
	 * 
	 * @param activity
	 * @param api_key
	 * @param user_id
	 */
	public static void initialize(Activity activity, String apiKey, String userID) {
		initialize(activity, apiKey, userID, false);
	}
	
	/**
	 * Tracks a parameterless event
	 * 
	 * @param eventType
	 */
	public static void track(String eventType) {
		track(eventType, null);
	}
	
	/**
	 * Tracks an event with parameters
	 * 
	 * @param eventType
	 * @param parameters
	 */
	public static void track(String eventType, JSONObject parameters) {
		synchronized(Omniata.class) {
			instance._track(eventType, parameters);
		}
	}
	
	/**
	 * Tracks a load event. 
	 * Should be called upon app start.
	 */
	public static void trackLoad() {
		trackLoad(getDeviceProperties());
	}
	
	public static void trackLoad(JSONObject parameters) {
		synchronized(Omniata.class) {
			instance._track("om_load", OmniataUtils.mergeJSON(getDeviceProperties(), parameters));
		}
	}
	
	/**
	 * Sets the current user id used to track events
	 * @param userId
	 */
	public static void setUserId(String userId) {
		synchronized(Omniata.class) {
			instance._setUserId(userId);
		}
	}
	
	/**
	 * Sets the current API key used to track events
	 * @param apiKey
	 */
	public static void setApiKey(String apiKey) {
		synchronized(Omniata.class) {
			instance._setApiKey(apiKey);
		}
	}
	
	/**
	 * Fetches content for this user from a specific channel
	 * 
	 * @param channelId The id of this channel
	 * @param handler An object implementing OmniataChannelResponseHandler
	 */
	public static void channel(int channelId, OmniataChannelResponseHandler handler) {
		synchronized(Omniata.class) {
			instance._channel(channelId, handler);
		}
	}
	
	/**
	 * Tracks a revenue event
	 * 
	 * @param total Revenue amount in currency code
	 * @param currencyCode A three letter currency code following ISO-4217 spec.
	 */
	public static void trackRevenue(double total, String currencyCode) {
		trackRevenue(total, currencyCode, null);
	}
	
	/**
	 * Tracks a revenue event
	 * 
	 * @param total Revenue amount in currency code
	 * @param currencyCode A three letter currency code following ISO-4217 spec.
	 * @param additionalParams Additional parameters to be tracked with event
	 */
	public static void trackRevenue(double total, String currencyCode, JSONObject additionalParams) {
		JSONObject parameters = new JSONObject();
		
		try {
			parameters.put("total", total); // Java doesn't use locale-specific formatting, so this is safe
			parameters.put("currency_code", currencyCode);
			
			if (additionalParams != null) {
				@SuppressWarnings("unchecked")
				Iterator<String> i = (Iterator<String>)additionalParams.keys();
				while(i.hasNext()) {
					String key = (String)i.next();
					Object val = additionalParams.get(key);
					parameters.put(key, val);
				}
			}
			
			synchronized(Omniata.class) {
				instance._track("om_revenue", parameters);
			}
			
		} catch (JSONException e) {
			OmniataLog.e(TAG, e.toString());
		}
	}
	
	protected static JSONObject getDeviceProperties() {
		JSONObject properties = new JSONObject();
		Locale locale = Locale.getDefault();
		
		try {
			properties.put("om_platform", "Android");
			properties.put("om_android_id", Settings.Secure.ANDROID_ID);
			properties.put("om_android_serial", android.os.Build.SERIAL);
			properties.put("om_android_model", android.os.Build.MODEL);
			properties.put("om_android_device", android.os.Build.DEVICE);
			properties.put("om_android_hardware", android.os.Build.HARDWARE);
		
			if (locale != null) {
				properties.put("om_locale", locale);
			}
		} catch(Throwable e) {
			
		}
		return properties;
	}
	
	protected void _track(String eventType, JSONObject parameters) {
		JSONObject event;
		
		try {			
			if (parameters != null) {
				event = new JSONObject(parameters.toString());
			} else {
				event = new JSONObject();
			}
			
			event.put("om_event_type", eventType);
			event.put("api_key", apiKey);
			event.put("uid", userID);
			
			while(true) {
				try {
					eventBuffer.put(event);
					break;
				} catch (InterruptedException e) {
				}
			}
		} catch (JSONException e) {
			OmniataLog.e(TAG, e.toString());
		}
	}
	
	protected void _channel(final int channelId, final OmniataChannelResponseHandler handler) {
		Thread req = new Thread(new Runnable() {
			
			@Override
			public void run() {
				String uri = OmniataUtils.getChannelAPI(false) + "?api_key=" + apiKey + "&uid=" + userID + "&channel_id" + channelId;
				
				try {
					URL url = new URL(uri);
					final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
					
					final int httpResponse = connection.getResponseCode();
					
					if (httpResponse >= 200 && httpResponse < 300) {
						activity.runOnUiThread(new Runnable() {							
							@Override
							public void run() {
								try {
									String body = OmniataUtils.convertStreamToString(connection.getInputStream());
									JSONObject jsonObj =  new JSONObject(body);
									JSONArray content   = jsonObj.getJSONArray("content");
									handler.onSuccess(channelId, content);
								} catch (Exception e) {
									handler.onError(channelId, e);
								}
							}
						});
						
					} else {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								handler.onError(channelId, new Exception("Error: Invalid http response code: " + httpResponse));
							}
						});
					}
				} catch (final Exception e) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							handler.onError(channelId, e);
						}
					});
					
				}
			}
		});
		
		req.start();
	}
	
	private void _setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	private void _setUserId(String userId) {
		this.userID = userId;
	}
	
	private Omniata(Activity activity, String apiKey, String userID, boolean debug) {

	}
	
	private void _initialize(Activity activity, String apiKey, String userID, boolean debug) {
		OmniataLog.i(TAG, "Initializing Omniata with apiKey: " + apiKey + " and userID: " + userID);

		this.apiKey   	  = apiKey;
		this.userID   	  = userID;
		
		if (this.activity == null) {
			this.activity = activity;
		}
		
		if (eventBuffer == null) {
			eventBuffer = new LinkedBlockingQueue<JSONObject>();
		}
		
		if (eventLog == null) {
			eventLog = new PersistentBlockingQueue<JSONObject>(activity, EVENT_LOG, JSONObject.class);
		}
		
		if (eventLogger == null) {
			eventLogger = new OmniataEventLogger(eventBuffer, eventLog);
		}
		
		if (eventWorker == null) {
			eventWorker = new OmniataEventWorker(activity, eventLog, debug);
		}
		
		eventLogger.start();
		eventWorker.start();
	}
	
	private Activity 							activity;
	private String 								apiKey;
	private String 								userID;	
	private BlockingQueue<JSONObject> 			eventBuffer;
	private PersistentBlockingQueue<JSONObject> eventLog;
	private OmniataEventLogger					eventLogger;
	private OmniataEventWorker					eventWorker;
}
