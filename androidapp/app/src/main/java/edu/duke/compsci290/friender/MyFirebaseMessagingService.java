package edu.duke.compsci290.friender;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

//import com.firebase.jobdispatcher.FirebaseJobDispatcher;
//import com.firebase.jobdispatcher.GooglePlayDriver;
//import com.firebase.jobdispatcher.Job;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Log.d("Notificationmessage", remoteMessage.getNotification().getBody());
        String channelId = getString(R.string.default_notification_channel_id);
        String title = notification.getTitle();
        String body = notification.getBody();
        String clickAction = notification.getClickAction();
        Intent intent = new Intent(clickAction);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder notifiBuilder = new Notification.Builder(this)
                .setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher).setPriority(Notification.PRIORITY_MAX);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.notify(0, notifiBuilder.build());
        Intent intent1 = new Intent(getApplicationContext(), MapsActivity.class);
        intent1.putExtra("notification", body);
        startActivity(intent1);

    }
}
