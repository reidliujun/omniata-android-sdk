package com.omniata.android.sdk;

import java.io.InputStream;

/* package */ interface OmniataHttpResponseHandler {
	/* package */ void onComplete(int responseCode, InputStream in);
	/* package */ void onError(Exception e);
}
