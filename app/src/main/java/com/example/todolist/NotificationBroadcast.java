package com.example.todolist;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notifyTask");
        Intent editTaskIntent = new Intent(context, EditTask.class);
        editTaskIntent.putExtra("id", intent.getIntExtra("id", 0));
        TaskStackBuilder taskStackBuilder=TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntentWithParentStack(editTaskIntent);
        PendingIntent pendingIntent=taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        builder.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Przypomnienie o zadaniu")
                .setContentText(intent.getStringExtra("task"))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority((NotificationCompat.PRIORITY_DEFAULT));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(intent.getIntExtra("id", 0), builder.build());
    }
}
