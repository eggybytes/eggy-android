package com.eggybytes.android.eggy_android;

import android.net.Uri;

public class EggyConfig {
    static final Uri DEFAULT_DEVICE_URI = Uri.parse("https://useeggy.com/registration/device/android");
    static final Uri DEFAULT_EVENTS_URI = Uri.parse("https://useeggy.com/events/send");

    static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10_000; // 10 seconds

    // The API token for your [eggy app](https://useeggy.com/apps)
    private final String apiToken;

    // The URL for registering device attributes
    private final Uri deviceUri;

    // The URL for sending events
    private final Uri eventsUri;

    // The timeout interval for network requests in milliseconds. Defaults to 10 seconds
    private final int connectionTimeoutMillis;

    public String getApiToken() {
        return this.apiToken;
    }

    public Uri getDeviceUri() {
        return this.deviceUri;
    }

    public Uri getEventsUri() {
        return this.eventsUri;
    }

    public int getConnectionTimeoutMillis() {
        return this.connectionTimeoutMillis;
    }

    // Creates an EggyConfig with the specified API token
    public EggyConfig(String apiToken) {
        this.apiToken = apiToken;
        this.deviceUri = DEFAULT_DEVICE_URI;
        this.eventsUri = DEFAULT_EVENTS_URI;
        this.connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;
    }
}
