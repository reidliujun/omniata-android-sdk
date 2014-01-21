package com.omniata.android.sdk;

import android.util.Log;

/* package */ class OmniataLog {
	private static int priority = Log.ASSERT;
	public static void setPriority(int priority) {OmniataLog.priority = priority;}
	public static int wtf(String tag, String msg) {return log(Log.ASSERT, tag, msg, null);}
	public static int e(String tag, String msg) {return log(Log.ERROR, tag, msg, null);}
	public static int w(String tag, String msg) {return log(Log.WARN, tag, msg, null);}
	public static int i(String tag, String msg) {return log(Log.INFO, tag, msg, null);}
	public static int d(String tag, String msg) {return log(Log.DEBUG, tag, msg, null);}
	public static int v(String tag, String msg) {return log(Log.VERBOSE, tag, msg, null);}
	public static int wtf(String tag, String msg, Throwable t) {return log(Log.ASSERT, tag, msg, t);}
	public static int e(String tag, String msg, Throwable t) {return log(Log.ERROR, tag, msg, t);}
	public static int w(String tag, String msg, Throwable t) {return log(Log.WARN, tag, msg, t);}
	public static int i(String tag, String msg, Throwable t) {return log(Log.INFO, tag, msg, t);}
	public static int d(String tag, String msg, Throwable t) {return log(Log.DEBUG, tag, msg, t);}
	public static int v(String tag, String msg, Throwable t) {return log(Log.VERBOSE, tag, msg, t);}
	private static int log(int priority, String tag, String msg, Throwable t) { 
		if (priority >= OmniataLog.priority) {
			if (t == null) {
				return Log.println(priority, tag, msg);
			} else {
				return Log.println(priority, tag, msg + "\n" + join(t.getStackTrace()));
			}
		} else {
			return 0;
		}
	}
	
	private static <E> String join(E[] e){
		StringBuilder sb = new StringBuilder();
		for(E i : e) {
			sb.append(i.toString());
		}
		return sb.toString();
	}
}
