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
import android.text.format.Time;
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

		serviceInfo.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		} else {
			serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		}    

		serviceInfo.notificationTimeout = 0;

		Log.d("NotiService", "Connected.");

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

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup localView = (ViewGroup) inflater.inflate(notification.contentView.getLayoutId(), null);
			notification.contentView.reapply(getApplicationContext(), localView);

			if (notification.vibrate == null) {
				Log.d("NotiService", "No vibration");
			}
			Log.d("NotiService", "Vibration : " + notification.vibrate.toString());

			/*
			//event time is elapsed from booting.
			Time t = new Time();
			t.set(java.lang.System.currentTimeMillis() + event.getEventTime() - android.os.SystemClock.elapsedRealtime());

			Log.d("NotiService", t.format3339(false));
			*/

			// Find all texts of the notification
			ArrayList<TextView> views = new ArrayList<TextView>();
			getTextViews(views, localView);
			Log.d("NotiService", views.size() + "");
			for (TextView v: views) {
				String text = v.getText().toString();
				if (!text.equals("")) {
					Log.d("NotiService", "[ItemId]              " + v.getId());
					Log.d("NotiService", "[Text]                " + text);
					message += text + "\n";
				}
			}
		} catch (Exception e) {
			Log.e("NotiService", "Noti message error." + e.getMessage());
		}



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

	private String getExtraData(Notification notification, String existing_text) {
		if (Constants.IS_LOGGABLE) {
			Log.i(Constants.LOG_TAG, "I am running extra data");
		}
		RemoteViews views = notification.contentView;
		if (views == null) {
			if (Constants.IS_LOGGABLE) {
				Log.i(Constants.LOG_TAG, "ContentView was empty, returning a blank string");
			}
			return "";
		}

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		} catch (RemoteViews.ActionException e) {
			return "";
		}

	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private String getExtraBigData(Notification notification, String existing_text) {
		if (Constants.IS_LOGGABLE) {
			Log.i(Constants.LOG_TAG, "I am running extra big data");
		}
		RemoteViews views = null;
		try {
			views = notification.bigContentView;
		} catch (NoSuchFieldError e) {
			return getExtraData(notification, existing_text);
		}
		if (views == null) {
			if (Constants.IS_LOGGABLE) {
				Log.i(Constants.LOG_TAG, "bigContentView was empty, running normal");
			}
			return getExtraData(notification, existing_text);
		}
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		}
	}

	private String dumpViewGroup(int depth, ViewGroup vg, String existing_text) {
		String text = "";
		Log.d(Constants.LOG_TAG, "root view, depth:" + depth + "; view: " + vg);
		for (int i = 0; i < vg.getChildCount(); ++i) {
			View v = vg.getChildAt(i);
			if (Constants.IS_LOGGABLE) {
				Log.d(Constants.LOG_TAG, "depth: " + depth + "; " + v.getClass().toString() + "; view: " + v);
			}
			if (v.getId() == android.R.id.title || v instanceof android.widget.Button
					|| v.getClass().toString().contains("android.widget.DateTimeView")) {
				if (Constants.IS_LOGGABLE) {
					Log.d(Constants.LOG_TAG, "I am going to skip this, but if I didn't, the text would be: "
							+ ((TextView) v).getText().toString());
				}
				if (existing_text.isEmpty() && v.getId() == android.R.id.title) {
					if (Constants.IS_LOGGABLE) {
						Log.d(Constants.LOG_TAG,
								"I was going to skip this, but the existing text was empty, and I need something.");
					}
				} else {
					continue;
				}
			}

			if (v instanceof TextView) {
				TextView tv = (TextView) v;
				if (tv.getText().toString() == "..." || tv.getText().toString() == "�"
						|| isInteger(tv.getText().toString())
						|| tv.getText().toString().trim().equalsIgnoreCase(existing_text)) {
					if (Constants.IS_LOGGABLE) {
						Log.d(Constants.LOG_TAG, "Text is: " + tv.getText().toString() + " but I am going to skip this");
					}
					continue;
				}
				text += tv.getText().toString() + "\n";
				if (Constants.IS_LOGGABLE) {
					Log.i(Constants.LOG_TAG, tv.getText().toString());
				}
			}
			if (v instanceof ViewGroup) {
				text += dumpViewGroup(depth + 1, (ViewGroup) v, existing_text);
			}
		}
		return text;
	}

	public boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void onInterrupt() {
		//sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}

}
