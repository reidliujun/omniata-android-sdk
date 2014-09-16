Example of using the multiple apiKeys and userIds for the new SDK is listed as follows:

```java
userInfo = new HashMap<String, String>(){{
    	put("<apiKey 1>", "<userId 1>");
    	put("<apiKey 2>", "<userId 2>");
    }}; 
```

And initialize as follows:
````java
Omniata.initialize(activity, userInfo, true);
```
