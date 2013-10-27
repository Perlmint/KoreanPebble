package kr.omniavinco.koreanpebble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

public class NotiService extends AccessibilityService {
	private SharedPreferences sharedPreferences;
	private final AccessibilityServiceInfo serviceInfo = new AccessibilityServiceInfo();

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("notiService", "create");
	}

	@Override
	protected void onServiceConnected() {
		
		Log.d("NotiService", "Connected.");

		serviceInfo.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		} else {
			serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		}    
		this.setServiceInfo(serviceInfo);

		sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
		Intent serviceIntent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
		startService(serviceIntent);
	}

	private void getTextViews(ArrayList<TextView> views, ViewGroup v)
	{
		if (views == null) {
			return;
		}
		for (int i = 0; i < v.getChildCount(); i++) {
			View child = v.getChildAt(i); 
			if (child instanceof TextView) {
				views.add((TextView)child);
			} else if (child instanceof ViewGroup) {
				getTextViews(views, (ViewGroup)child);
			}
		}
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		PackageManager pm = getPackageManager();

		String eventPackageName = event.getPackageName().toString();

		boolean found = sharedPreferences.getBoolean(eventPackageName, false);        

		if (!found) {
			Log.e("service_ignored", eventPackageName);
			return;
		}
		Log.e("service_accepted", eventPackageName);

		String message = "";

		try {
			Notification notification = (Notification) event.getParcelableData();

			if (notification == null) {
				Log.d("NotiService", "noti is null");
				return;
			}
			
			// ignore non-vibration notification
			if (notification.vibrate == null) {
				return;
			}

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup localView = (ViewGroup) inflater.inflate(notification.contentView.getLayoutId(), null);
			notification.contentView.reapply(getApplicationContext(), localView);

			// Find all texts of the notification
			ArrayList<TextView> views = new ArrayList<TextView>();
			getTextViews(views, localView);
			Log.d("NotiService", views.size() + "");
			for (TextView v : views) {
				String text = v.getText().toString();
				if (!text.equals("")) {
					Log.d("NotiService", "[ItemId]              " + v.getId());
					Log.d("NotiService", "[Text]                " + text);
					message += text + "\n";
				}
			}
		} catch (Exception e) {
			Log.e("NotiService", "Noti message error : " + e.getMessage() + e.getCause() );
		}

		Log.d("NotiService", message);
		// Create json object to be sent to Pebble
		final Map<String, Object> data = new HashMap<String, Object>();
		try {
			data.put("title", pm.getApplicationLabel(pm.getApplicationInfo(eventPackageName, 0)));
		} catch (NameNotFoundException e) {
			data.put("title", eventPackageName);
		}
		if (message != null) {
			data.put("body", message);
		} else {
			data.put("body", " ");
		}
		final JSONObject jsonData = new JSONObject(data);
		final String notificationData = jsonData.toString();

		// Create the intent to house the Pebble notification
		final Intent i = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
		i.putExtra("messageType", Constants.PEBBLE_MESSAGE_TYPE_ALERT);
		i.putExtra("notificationData", notificationData);
		sendBroadcast(i);

	}

	@Override
	public void onInterrupt() {
		//sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}

}
