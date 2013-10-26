package kr.omniavinco.koreanpebble;

import kr.omniavinco.koreanpebble.Constants;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

public class NotiService extends AccessibilityService {
    SharedPreferences sharedPreferences;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	Log.e("notiService", "create");
    }
    
	@Override
	protected void onServiceConnected() {
		sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        Intent serviceIntent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
        startService(serviceIntent);
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
        
        
     // get the notification text
        String notificationText = event.getText().toString();
        // strip the first and last characters which are [ and ]
        notificationText = notificationText.substring(1, notificationText.length() - 1);

        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                notificationText += "\n" + getExtraBigData((Notification) parcelable, notificationText.trim());
            } else {
                notificationText += "\n" + getExtraData((Notification) parcelable, notificationText.trim());
            }

        }
        
        // Create json object to be sent to Pebble
        final Map<String, Object> data = new HashMap<String, Object>();
        try {
            data.put("title", pm.getApplicationLabel(pm.getApplicationInfo(eventPackageName, 0)));
        } catch (NameNotFoundException e) {
            data.put("title", eventPackageName);
        }
        if (notificationText != null) {
        	data.put("body", notificationText);
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
