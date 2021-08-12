package com.eggybytes.android.eggy_android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.eggybytes.android.eggy_android.EggyClient;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class EggyFCMService extends FirebaseMessagingService {
    private static final String TAG = "EggyFCMService";

    private static final String EGGY_MESSAGE_HEADER_KEY = "eggy_message";
    private static final String EGGY_PUSH_NOTIFICATION_ID_KEY = "push_notification_id";
    private static final String EGGY_NOTIFICATION_MANAGER_TAG = "EggyNotifMgr";
    private static final String EGGY_DEFAULT_NOTIFICATION_CHANNEL_ID = "EggyNotificationChannel";
    private static final String EGGY_DEFAULT_NOTIFICATION_CHANNEL_NAME = "General Notifications";

    // Called when a new token is generated. This is invoked after app install when a token is first
    // generated and again if the token changes.
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.v(TAG, "Registering new FCM push token in onNewToken: " + token);

        new Thread() {
            @Override
            public void run() {
                EggyClient.registerWithDeviceApi(String.valueOf(FirebaseInstallations.getInstance().getId()), token);
            }
        }.start();
    }

    // Called when a message is received while the app is in the foreground, or when a message with
    // only data is received when app is in the background. (All eggy messages include data, so
    // eggy messages received while backgrounded will be received in system tray + extras of the
    // intent instead of here.)
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        handleEggyMessage(this, remoteMessage);
    }

    // Checks if a received message was sent from eggy, and processes it if so. If not, does
    // nothing.
    private static void handleEggyMessage(Context context, RemoteMessage remoteMessage) {
        if (remoteMessage == null) {
            Log.i(TAG, "Remote message from FCM was null; not handling");
        }

        assert remoteMessage != null;
        if (!isEggyPushNotification(remoteMessage)) {
            Log.i(TAG, "Remote message from FCM was not from eggy; not handling");
            return;
        }

        Map<String, String> remoteMessageData = remoteMessage.getData();
        Log.v(TAG, "Received remote message from FCM via eggy: " + remoteMessageData);

        NotificationHandlerRunnable notificationHandlerRunnable = new NotificationHandlerRunnable(context, remoteMessage);
        new Thread(notificationHandlerRunnable).start();
    }

    private static class NotificationHandlerRunnable implements Runnable {
        private final Context context;
        private final RemoteMessage remoteMessage;

        NotificationHandlerRunnable(Context context, @NonNull RemoteMessage remoteMessage) {
            this.context = context;
            this.remoteMessage = remoteMessage;
        }

        @Override
        public void run() {
            Log.v(TAG, "Handling remote message from FCM via eggy: " + this.remoteMessage);
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification == null) {
                return;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(EGGY_NOTIFICATION_MANAGER_TAG, getNotificationId(notification, remoteMessage.getData()), createNotification(context, notification));

            EggyClient.sendPushNotificationWasReceivedForegroundEventWithMessage(remoteMessage);
        }

        private static Notification createNotification(Context context, RemoteMessage.Notification notification) {
            String channelId = getOrCreateNotificationChannelId(context);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId).setAutoCancel(true);

            if (notification.getTitle() != null) {
                notificationBuilder.setContentTitle(notification.getTitle());
                notificationBuilder.setTicker(notification.getTitle());
            }

            if (notification.getBody() != null) {
                notificationBuilder.setContentText(notification.getBody());
            }

            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, -1, intent, PendingIntent.FLAG_ONE_SHOT);
            notificationBuilder.setContentIntent(pendingIntent);

            ApplicationInfo info = null;
            try {
                info = pm.getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (info == null) return null;
            notificationBuilder.setSmallIcon(info.icon);

            // Notifications can (optionally) include a sound to play when the notification is
            // delivered. Starting with Android O, the sound is set on the notification channel, not
            // on each notification.
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);

            // Starting with Android O, the priority is set on the notification channel, not on each
            // notification.
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

            return notificationBuilder.build();
        }

        private static String getOrCreateNotificationChannelId(Context context) {
            // If we're on a version before O, channel ID is irrelevant
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return EGGY_DEFAULT_NOTIFICATION_CHANNEL_ID;
            }

            // Create the default channel if it doesn't already exist
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager.getNotificationChannel(EGGY_DEFAULT_NOTIFICATION_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        EGGY_DEFAULT_NOTIFICATION_CHANNEL_ID,
                        EGGY_DEFAULT_NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }

            return EGGY_DEFAULT_NOTIFICATION_CHANNEL_ID;
        }

        private static int getNotificationId(RemoteMessage.Notification notification, Map<String, String> remoteMessageData) {
            String pushNotificationId = remoteMessageData.get(EGGY_PUSH_NOTIFICATION_ID_KEY);
            if (pushNotificationId != null) {
                return Integer.parseInt(pushNotificationId);
            }

            String title = notification.getTitle();
            String body = notification.getBody();

            String key = "";
            if (title != null) {
                key = key + title;
            }

            if (body != null) {
                key = key + body;
            }

            return key.hashCode();
        }
    }

    // Returns true if the FCM message was sent from eggy, and false otherwise.
    private static boolean isEggyPushNotification(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> remoteMessageData = remoteMessage.getData();
        String eggyMessageHeader = remoteMessageData.get(EGGY_MESSAGE_HEADER_KEY);

        return eggyMessageHeader != null && eggyMessageHeader.equals("true");
    }
}
