The old initialization is still support like this:

```java
Omniata.initialize(activity, apiKey, userId, true);

```

The new initialization method to support new host name can call like this.

##Debug mode will goes to the url '<org>.analyzer-test.omniata.com'
```java
String org = "rovio";
Omniata.initialize(activity,apiKey, userId, org, true);
```

##Non-debug mode goes to the url '<org>.analyzer.omniata.com' and '<org>.engager.omniata.com'
```java
String org = "rovio";
Omniata.initialize(activity,apiKey, userId, org, false);
```
