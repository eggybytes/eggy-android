package com.eggybytes.android.eggy_android;

import android.os.Bundle;
import android.util.Log;
import com.google.firebase.messaging.RemoteMessage;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class EggyClient {
    static final String TAG = "EggyClient";

    private static EggyClient sharedEggyClient;

    private final EggyConfig config;
    private final EggyDevice device;
    public String pushToken;

    public static void start(EggyConfig config, EggyDevice device) {
        if (sharedEggyClient != null) {
            Log.w(TAG, "EggyClient.start() was called more than once");
            return;
        }

        sharedEggyClient = new EggyClient(config, device);
    }

    public static EggyClient shared() {
        return sharedEggyClient;
    }

    public static void registerWithDeviceApi(String firebaseInstanceId, String deviceToken) {
        EggyClient client = EggyClient.shared();
        if (client == null) {
            return;
        }

        client.pushToken = deviceToken;

        String json = "{\n"
                + "    \"firebase_instance_id\": \"" + firebaseInstanceId + "\",\n"
                + "    \"fcm_device_token\": \"" + deviceToken + "\",\n"
                + "    \"preferred_language\": \"" + client.device.preferredLanguage + "\",\n"
                + "    \"os_name\": \"" + client.device.osName + "\",\n"
                + "    \"os_version\": \"" + client.device.osVersion + "\",\n"
                + "    \"device_model\": \"" + client.device.deviceModel + "\",\n"
                + "    \"timezone_seconds_from_gmt\": \"" + client.device.timezoneSecondsFromGmt + "\"\n"
                + "}";
        byte[] body = json.getBytes();

        try {
            URL url = new URL(client.config.getDeviceUri().toString());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("authorization", client.config.getApiToken());
            connection.getOutputStream().write(body);
            connection.getOutputStream().flush();
            connection.getOutputStream().close();

            String resp = connection.getResponseMessage();
            int respCode = connection.getResponseCode();
            Log.v(TAG, "Device registration: " + String.valueOf(respCode));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendPushNotificationWasOpenedEventWithExtras(Bundle extras) {
        Integer organizationId = (extras.getString("eggy_organization_id") != null) ? Integer.parseInt(Objects.requireNonNull(extras.getString("eggy_organization_id"))) : null;
        Integer appId = (extras.getString("eggy_app_id") != null) ? Integer.parseInt(Objects.requireNonNull(extras.getString("eggy_app_id"))) : null;
        Integer communiqueId = (extras.getString("eggy_communique_id") != null) ? Integer.parseInt(Objects.requireNonNull(extras.getString("eggy_communique_id"))) : null;
        Long userId = (extras.getString("eggy_user_id") != null) ? Long.parseLong(Objects.requireNonNull(extras.getString("eggy_user_id"))) : null;
        Integer pushNotificationId = (extras.getString("eggy_push_notification_id") != null) ? Integer.parseInt(Objects.requireNonNull(extras.getString("eggy_push_notification_id"))) : null;

        sendPushEvent("push_notification_was_opened", organizationId, appId, communiqueId, userId, pushNotificationId);
    }

    public static void sendPushNotificationWasReceivedForegroundEventWithMessage(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().isEmpty()) {
            return;
        }

        Integer organizationId = (remoteMessage.getData().get("eggy_organization_id") != null) ? Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("eggy_organization_id"))) : null;
        Integer appId = (remoteMessage.getData().get("eggy_app_id") != null) ? Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("eggy_app_id"))) : null;
        Integer communiqueId = (remoteMessage.getData().get("eggy_communique_id") != null) ? Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("eggy_communique_id"))) : null;
        Long userId = (remoteMessage.getData().get("eggy_user_id") != null) ? Long.parseLong(Objects.requireNonNull(remoteMessage.getData().get("eggy_user_id"))) : null;
        Integer pushNotificationId = (remoteMessage.getData().get("eggy_push_notification_id") != null) ? Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("eggy_push_notification_id"))) : null;

        sendPushEvent("push_notification_was_received_foreground", organizationId, appId, communiqueId, userId, pushNotificationId);
    }

    public static void sendPushEvent(String eventType, Integer organizationId, Integer appId, Integer communiqueId, Long userId, Integer pushNotificationId) {
        Log.v(TAG, String.format("Sending push event for " + eventType));

        EggyClient client = EggyClient.shared();
        if (client == null) {
            Log.i(TAG, "Could not send push event because no eggy client was found");
            return;
        }

        String json = "{\n"
                + "    \"raw_events\": [{\n"
                + ((organizationId != null) ? "        \"organization_id\": \"" + organizationId + "\",\n" : "")
                + ((client.device.clientUserId != null) ? "        \"client_id\": \"" + client.device.clientUserId + "\",\n" : "")
                + "        \"client_event_time\": \"" + System.currentTimeMillis() + "\",\n"
                + "        \"eventMetadata\": {\n"
                + "            \"" + eventType + "\": {\n"
                + "                \"app_id\": \"" + appId + "\",\n"
                + "                \"communique_id\": \"" + communiqueId + "\",\n"
                + "                \"user_id\": \"" + userId + "\",\n"
                + "                \"push_notification_id\": \"" + pushNotificationId + "\",\n"
                + "                \"device_token\": \"" + client.pushToken + "\"\n"
                + "            }\n"
                + "        }\n"
                + "    }]\n"
                + "}";

        byte[] body = json.getBytes();

        try {
            URL url = new URL(client.config.getEventsUri().toString());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("authorization", client.config.getApiToken());
            connection.getOutputStream().write(body);
            connection.getOutputStream().flush();
            connection.getOutputStream().close();

            String resp = connection.getResponseMessage();
            int respCode = connection.getResponseCode();
            Log.v(TAG, "Push event send: " + respCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private EggyClient(EggyConfig config, EggyDevice device) {
        this.config = config;
        this.device = device;
        this.pushToken = null;
    }
}
