#Omniata-android-sdk
Check [here](https://omniata.atlassian.net/wiki/display/DOC/ANDROID+SDK) for Omniata integration documentation.

##Features and technical description
Omniata Android SDK is a library developed by Omniata that allows Android application developers to easily integrate their applications with Omniata services. The key features of the SDK are sending events to Omniata using Event API and requesting content for users using Channel API.

###Release Notes
This version is compatible with the previous version v2.0.0.
Changed details:
    - Change methods name for Unity usage.
    - Fix Unity related URL encoded bug.

###Integrating Omniata Android SDK
####Installation and upgrade
The Omniata Android SDK requires the following permissions in your ApplicationManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```
Next place omniata-android-sdk.jar in your project's lib directory. Finally add an import for the SDK.
Upgrading to the latest version involves replacing the old jar with the new one.

####Initialization
Import the SDK package.
```java
import com.omniata.android.sdk.*;
```
Initialize the library by calling the initialize function with the identifier of the user and the API key provided in the panel.
For initial testing it is recommended to track events in debug mode against the event monitor. This will allow you to debug your events in real-time.
<ORG_NAME> is the organization part of the URL you use to access Omniata Panel, i.e. https://organization.panel.omniata.com -> <ORG_NAME> would be 'organization'
```java
public class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
  ...
   //========================
   // Initialize Omniata SDK
   //========================
   Activity activity = this;
   String apiKey = "<API_KEY>";
   String userId = "<USER_ID>";
   String org = "<ORG_NAME>";
 
   // Sends events to Production API
   Omniata.initialize(activity, apiKey, userId,org);
    
   ...
  }
  ...
}
```

####Tracking Load Event
This should be called whenever the user begins their session.
```java
Omniata.trackLoad();
```

####Tracking Revenue Event
```java
double total = 1.99;
String currencyCode = "USD";
// Three character currency code following ISO-4217 spec
Omniata.trackRevenue(total, currencyCode);
```

####Tracking Custom Event
```java
import org.json.JSONObject;
import org.json.JSONException;
...
JSONObject parameters = new JSONObject();
try{
  parameters.put("xp", 1000);
  parameters.put("level", 2);
} catch(JSONException e){
  // do something
}
 
Omniata.track("level_up", parameters);
```

####Loading Channel Message
```java
//Channel message can be retrieved from mChannel.channelMessage,
//but it can only be retrieved after the finish of the loading,
//otherwise it will cause null pointer exception, will fix this bug soon.
int channel_id = 40;
OmniataChannelEngine mChannel = new OmniataChannelEngine();
//Load the channel message for certain channel
Omniata.channel(channel_id, mChannel);
```

####Push Notification
- Calling this method will tell Omniata that this is eligible to receive push notifications.
```java
Activity activity = this;
Omniata.initialize(activity, "<API_KEY>", "<USER_ID>");
...
Omniata.enablePushNotifications("<REGISTRATION_ID>");
```

- Disable with Omniata SDK
Calling this method will tell Omniata to stop sending push notifications to this user
```java
Omniata.disablePushNotifications();
```




