package com.omniata.android.sdk;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/* package */ class OmniataUtils {
	static final String API 	  		   = "api.omniata.com";
	static final String TEST_API  		   = "api-test.omniata.com";
	
	/* package */ static String getProtocol(boolean useSSL) {
		return useSSL ? "http://" : "http://";
	}
	
	/* package */ static String getEventAPI(boolean useSSL, boolean debug) {
		if (debug) {
			return getProtocol(useSSL) + TEST_API + "/event";
		} else {
			return getProtocol(useSSL) + API + "/event";
		}
	}
	
	/* package */ static String getChannelAPI(boolean useSSL) {
		return getProtocol(useSSL) + API + "/channel";
	}
	
	/* package */ static String convertStreamToString(InputStream is) {
	    Scanner s = new Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	/* package */ static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		
		return activeNetwork != null && activeNetwork.isConnected();
	}
	
	/* package */ static String jsonToQueryString(JSONObject jsonObj) {
		StringBuilder sb = new StringBuilder();
		
		try {
			@SuppressWarnings("unchecked")
			Iterator<String> i = (Iterator<String>)jsonObj.keys();
			while (i.hasNext()) {
				String key = (String)i.next();
				Object value;
				try {
					value = jsonObj.get(key);
				} catch (JSONException e) {
					value = "";
				}				
				sb.append(URLEncoder.encode(key, "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode(value.toString(), "UTF-8"));
				sb.append("&");
			}
		} catch (UnsupportedEncodingException e) {
			System.out.println(e);
		}
		
		return sb.substring(0, sb.length() - 1);
	}
	
	/* package */ static JSONObject mergeJSON(JSONObject obj1, JSONObject obj2) {
		JSONObject merged = new JSONObject();
		@SuppressWarnings("unchecked")
		Iterator<String> keys1 = (Iterator<String>)obj1.keys();
		@SuppressWarnings("unchecked")
		Iterator<String> keys2 = (Iterator<String>)obj2.keys();
		
		while(keys1.hasNext()) {
			String key = (String)keys1.next();
			try {
				if (!obj2.has(key)) {
					merged.put(key, obj1.get(key));
				}
			} catch(Exception e) {
			}
		}
		
		while(keys2.hasNext()) {
			String key = (String)keys2.next();
			try {
				merged.put(key, obj2.get(key));
			} catch(Exception e) {
			}
		}
		return merged;
	}
}
