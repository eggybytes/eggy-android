package com.eggybytes.android.eggy_android;

import android.os.Build;
import java.util.Locale;
import java.util.TimeZone;

public class EggyDevice {
    // Client-app defined string that uniquely identifies the user. If the client app does not define this, eggy will assign an identifier associated with the anonymous user on the backend.
    final String clientUserId;

    final String preferredLanguage;
    final String osName;
    final int osVersion;
    final String deviceModel;
    final int timezoneSecondsFromGmt;

    protected EggyDevice(Builder builder) {
        this.clientUserId = builder.clientUserId;
        this.preferredLanguage = builder.preferredLanguage;
        this.osName = builder.osName;
        this.osVersion = builder.osVersion;
        this.deviceModel = builder.deviceModel;
        this.timezoneSecondsFromGmt = builder.timezoneSecondsFromGmt;
    }

    // Creates an EggyDevice with the specified client user ID
    public EggyDevice(String clientUserId) {
        this.clientUserId = clientUserId;
        this.preferredLanguage = Locale.getDefault().getLanguage();
        this.osName = Build.VERSION.RELEASE;
        this.osVersion = Build.VERSION.SDK_INT;
        this.deviceModel = Build.MODEL;
        this.timezoneSecondsFromGmt = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000;
    }

    // Creates an EggyDevice without any specified client user ID
    public EggyDevice() {
        this((String)null);
    }

    public static class Builder {
        // Client-app defined string that uniquely identifies the user. If the client app does not define this, eggy will assign an identifier associated with the anonymous user on the backend.
        private String clientUserId;

        private String preferredLanguage;
        private String osName;
        private int osVersion;
        private String deviceModel;
        private int timezoneSecondsFromGmt;

        // Creates a Builder with the specified client user ID
        public Builder(String clientUserId) {
            this.clientUserId = clientUserId;
        }

        public Builder clientUserId(String s) {
            this.clientUserId = s;
            return this;
        }

        public Builder preferredLanguage(String s) {
            this.preferredLanguage = s;
            return this;
        }

        public Builder osName(String s) {
            this.osName = s;
            return this;
        }

        public Builder osVersion(int n) {
            this.osVersion = n;
            return this;
        }

        public Builder deviceModel(String s) {
            this.deviceModel = s;
            return this;
        }

        public Builder timezoneSecondsFromGmt(int n) {
            this.timezoneSecondsFromGmt = n;
            return this;
        }

        public EggyDevice build() {
            return new EggyDevice(this);
        }
    }
}
