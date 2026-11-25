package com.lody.virtual.client.stub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;


/**
 * @author Lody
 *
 */
public class DaemonService extends Service {

    private static final String TAG = "VA-Daemon";
    private static final int NOTIFY_ID = 1001;
    private static final String CHANNEL_ID = "va_daemon";

	public static void startup(Context context) {
		try {
			context.startService(new Intent(context, DaemonService.class));
		} catch (RuntimeException e) {
			// Android 8+ rejects this while the app is in the background.
			Log.w(TAG, e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		startup(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			startForeground(NOTIFY_ID, buildNotification());
		} catch (RuntimeException e) {
			Log.w(TAG, e);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	private Notification buildNotification() {
		Notification.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.createNotificationChannel(new NotificationChannel(
					CHANNEL_ID, "VirtualApp service", NotificationManager.IMPORTANCE_LOW));
			builder = new Notification.Builder(this, CHANNEL_ID);
		} else {
			builder = new Notification.Builder(this);
		}
		return builder.setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
				.setContentTitle("VirtualApp")
				.setContentText("Engine is running")
				.build();
	}

	/** No longer started; kept because the manifest still declares it. */
	public static final class InnerService extends Service {

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
	}


}
