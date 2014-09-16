package com.omniata.android.sdk;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
	private static final String SDK_VERSION = "android-1.1.1";
	
	private static Omniata instance;
	
	/**
	 * Initializes the Omniata API with single apiKey and userId
	 * 
	 * @param activity
	 * @param apiKey	The api-key
	 * @param userID	The user-id
	 * @param debug 	True if events should be tracked against the event-monitor
	 * @throws IllegalArgumentException if activity is null
	 * @throws IllegalArgumentException if apiKey is null or empty
	 * @throws IllegalArgumentException if userID is null or empty 
	 */
		
	public static void initialize(Activity activity, String apiKey, String userID, boolean debug) throws IllegalArgumentException{
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
	
	/**
	 * Initializes the Omniata API with multiple apiKeys and userIds
	 * 
	 * @param activity
	 * @param userInfo	The api-key:the user-id
	 * @param debug 	True if events should be tracked against the event-monitor
	 * @throws IllegalArgumentException if activity is null
	 * @throws IllegalArgumentException if apiKey is null or empty
	 * @throws IllegalArgumentException if userID is null or empty 
	 */
	public static void initialize(Activity activity, Map<String, String> userInfo, boolean debug) throws IllegalArgumentException{
		synchronized(Omniata.class) {			
			if (instance == null) {
				OmniataLog.i(TAG, "Initializing Omniata API");
				instance = new Omniata(activity, userInfo, debug);
			}
			
			/*
			 * Since this singleton may persist across application launches
			 * we need to support re-initialization of the SDK
			 */
			instance._initialize(activity, userInfo, debug);
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
	 * @throws IllegalArgumentException if activity is null
	 * @throws IllegalArgumentException if apiKey is null or empty
	 * @throws IllegalArgumentException if userID is null or empty
	 */
	public static void initialize(Activity activity, String apiKey, String userID) throws IllegalArgumentException {
		initialize(activity, apiKey, userID, false);
	}
	
	/**
	 * Tracks a parameterless event
	 * 
	 * @param eventType
	 * @throws IllegalArgumentException if eventType is null or empty
	 * @throws IllegalStateException if SDK not initialized 
	 */
	public static void track(String eventType) throws IllegalArgumentException, IllegalStateException {
		track(eventType, null);
	}
	
	/**
	 * Tracks an event with parameters
	 * 
	 * @param eventType
	 * @param parameters
	 * @throws IllegalArgumentException if eventType is null or empty
	 * @throws IllegalStateException if SDK not initialized  
	 */
	public static void track(String eventType, JSONObject parameters) throws IllegalArgumentException, IllegalStateException {
		synchronized(Omniata.class) {
			assertInitialized();
			instance._track(eventType, parameters);
		}
	}
	
	private static void assertInitialized() throws IllegalStateException{
		if (instance == null) {
			throw new IllegalStateException("Uninitialized SDK");
		}
	}
	
	/**
	 * Tracks a load event. 
	 * Should be called upon app start.
	 * @throws IllegalStateException if SDK not initialized 
	 */
	public static void trackLoad() throws IllegalStateException{
		trackLoad(null);
	}
	
	/**
	 * Tracks a load event with additional parameters
	 * @param parameters Additional parameters to track with event
	 * @throws IllegalStateException if SDK not initialized
	 */
	public static void trackLoad(JSONObject parameters) throws IllegalStateException {
		if (parameters == null) {
			parameters = new JSONObject();
		}
		track("om_load", OmniataUtils.mergeJSON(getAutomaticParameters(), parameters));
	}
	
	/**
	 * Sets the current user id used to track events
	 * @param userId
	 * @throws IllegalArgumentException if userID is null or empty
	 * @throws IllegalStateException if SDK not initialized 
	 */
	public static void setUserId(String userId) throws IllegalArgumentException, IllegalStateException {
		synchronized(Omniata.class) {
			assertInitialized();
			OmniataUtils.assertUserIdValid(userId);			
			instance._setUserId(userId);
		}
	}
	
	/**
	 * Sets the current API key used to track events
	 * @param apiKey
	 * @throws IllegalArgumentException if apiKey is null or empty
	 * @throws IllegalStateException if SDK not initialized 
	 */
	public static void setApiKey(String apiKey) throws IllegalArgumentException, IllegalStateException {
		synchronized(Omniata.class) {
			assertInitialized();
			OmniataUtils.assertApiKeyValid(apiKey);
			instance._setApiKey(apiKey);
		}
	}
	
	/**
	 * Fetches content for this user from a specific channel
	 * 
	 * @param channelId The id of this channel
	 * @param handler An object implementing OmniataChannelResponseHandler
	 * @throws IllegalStateException if SDK not initialized
	 */
	public static void channel(int channelId, OmniataChannelResponseHandler handler) throws IllegalStateException {
		synchronized(Omniata.class) {
			assertInitialized();			
			instance._channel(channelId, handler);
		}
	}
	
	/**
	 * Tracks a revenue event
	 * 
	 * @param total Revenue amount in currency code
	 * @param currencyCode A three letter currency code following ISO-4217 spec.
	 * @throws IllegalStateException if SDK not initialized 
	 */
	public static void trackRevenue(double total, String currencyCode) throws IllegalStateException {
		// TODO: add currency code validation
		trackRevenue(total, currencyCode, null);
	}
	
	/**
	 * Tracks a revenue event
	 * 
	 * @param total Revenue amount in currency code
	 * @param currencyCode A three letter currency code following ISO-4217 spec.
	 * @param additionalParams Additional parameters to be tracked with event
	 * @throws IllegalStateException if SDK not initialized 
	 */
	public static void trackRevenue(double total, String currencyCode, JSONObject additionalParams) throws IllegalStateException {
		JSONObject parameters = new JSONObject();

		try {
			parameters.put("total", total); // Java doesn't use locale-specific formatting, so this is safe
			parameters.put("currency_code", currencyCode);
			
			if (additionalParams != null) {
				Iterator<String> i = (Iterator<String>)additionalParams.keys();
				while(i.hasNext()) {
					String key = (String)i.next();
					Object val = additionalParams.get(key);
					parameters.put(key, val);
				}
			}
			
			track("om_revenue", parameters);
			
		} catch (JSONException e) {
			OmniataLog.e(TAG, e.toString());
		}
	}
	
	public static void enablePushNotifications(String registrationId) {
		JSONObject params = new JSONObject();
		try {
			params.put("om_registration_id", registrationId);
			track("om_gcm_enable", params);
		} catch (JSONException e) {
			OmniataLog.e(TAG, e.toString());
		}
	}
	
	public static void disablePushNotifications() {
		track("om_gcm_disable");
	}
	
	protected static JSONObject getAutomaticParameters() {
		JSONObject properties = new JSONObject();
		Locale locale = Locale.getDefault();
		
		try {
			// Standard automatic parameters
			properties.put("om_sdk_version", SDK_VERSION);
			properties.put("om_os_version", android.os.Build.VERSION.SDK_INT);
			properties.put("om_platform", "android");
			properties.put("om_device", android.os.Build.MODEL);
			
			// Android-specific parameters
			properties.put("om_android_id", Settings.Secure.ANDROID_ID);
			properties.put("om_android_serial", android.os.Build.SERIAL);
			properties.put("om_android_device", android.os.Build.DEVICE);
			properties.put("om_android_hardware", android.os.Build.HARDWARE);
		
			if (locale != null) {
				properties.put("om_locale", locale);
			}
		} catch(Throwable e) {
			
		}
		return properties;
	}
	
	protected void _track(String eventType, JSONObject parameters) throws IllegalArgumentException {
		JSONObject event;
		
		OmniataUtils.assertValidEventType(eventType);
		
		try {			
			if (parameters != null) {
				event = new JSONObject(parameters.toString());
			} else {
				event = new JSONObject();
			}
			
			event.put("om_event_type", eventType);
			
			
			if (apiKeysStr!=null){
				event.put("api_key", apiKeysStr);
				event.put("uid", userIdsStr);
				OmniataLog.i(TAG, event.toString());
			}else{
				event.put("api_key", apiKey);
				event.put("uid", userID);
				OmniataLog.i(TAG, event.toString());
			}
			
			event.put("om_creation_time", System.currentTimeMillis());
			
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
				String uri = OmniataUtils.getChannelAPI(true) + "?api_key=" + apiKey + "&uid=" + userID + "&channel_id" + channelId;
				
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
	
	/** 
	 * Format the apiKeys and userIds inside of URL
	 * @param apiKeysStr the format string for apiKeys
	 * @param userIdsStr the format string for userIds
	 */
	private void _getStrfromMap(Map <String, String> userInfo){
		
		this.apiKeys = new String[userInfo.size()];
    	this.userIds = new String[userInfo.size()];
    	this.apiKeysStr = "";
    	this.userIdsStr = "";
    	int i = 0;
        Set<Entry<String, String>> entries = userInfo.entrySet();
        Iterator<Entry<String, String>> iterator = entries.iterator();
        while(iterator.hasNext()){
        	@SuppressWarnings("rawtypes")
			Map.Entry mapping = (Map.Entry)iterator.next();
        	apiKeys[i] = mapping.getKey().toString(); 
        	userIds[i] = userInfo.get(apiKeys[i]);
        	i++;
        }
        
    	for (i=0;i<apiKeys.length;i++){
			if (i!= apiKeys.length-1){
				apiKeysStr = apiKeysStr + apiKeys[i]+",";
				userIdsStr = userIdsStr + userIds[i]+",";
			}else{
				apiKeysStr = apiKeysStr + apiKeys[i];
				userIdsStr = userIdsStr + userIds[i];
			}
    	}
    	
	}
	
	private Omniata(Activity activity, String apiKey, String userID, boolean debug) {

	}
	private Omniata(Activity activity, Map<String, String> userInfo, boolean debug) {

	}
	
	/** 
	 * Initialization for single apiKey and userId
	 * @param activity
	 * @param debug
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	private void _initialize(Activity activity, String apiKey, String userID, boolean debug) throws IllegalArgumentException, IllegalStateException {
		OmniataLog.i(TAG, "Initializing Omniata with apiKey: " + apiKey + " and userID: " + userID);
		
		if (activity == null) {
			throw new IllegalArgumentException("Activity is null");
		}
		
		OmniataUtils.assertApiKeyValid(apiKey);
		OmniataUtils.assertUserIdValid(userID);

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
	
	/**
	 * Initialization for multiple apiKeys and userIds
	 * @param activity
	 * @param userInfo
	 * @param debug
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	private void _initialize(Activity activity, Map<String, String> userInfo, boolean debug) throws IllegalArgumentException, IllegalStateException {
		instance._getStrfromMap(userInfo);
		
		for (int i=0;i<apiKeys.length;i++){
			OmniataLog.i(TAG, "Initializing Omniata with apiKey: " + apiKeys[i] + " and userID: " + userIds[i]);
			
			OmniataUtils.assertApiKeyValid(apiKeys[i]);
			OmniataUtils.assertUserIdValid(userIds[i]);
				
				
		}
		if (activity == null) {
			throw new IllegalArgumentException("Activity is null");
		}
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
	private String []							apiKeys;
	private String []							userIds;
	private String 								apiKeysStr;
	private String								userIdsStr;
	private BlockingQueue<JSONObject> 			eventBuffer;
	private PersistentBlockingQueue<JSONObject> eventLog;
	private OmniataEventLogger					eventLogger;
	private OmniataEventWorker					eventWorker;
}
