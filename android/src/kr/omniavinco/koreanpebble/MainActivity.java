package kr.omniavinco.koreanpebble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {
	private AlertDialog sendMessageDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		checkIsEnabledNotiService();
		initSendMessagePopup();
		initAppTable();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()) {
		case R.id.action_send_msg:
			sendMessageDialog.show();
			break;
		case R.id.action_download_pbw:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PEBBLE_APP_DOWNLOAD_LINK));
			startActivity(browserIntent);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void initSendMessagePopup() {

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		alertDialogBuilder.setTitle("Send message to pebble : ");

		LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.activity_main_send_message_dialog, null);
		final EditText msgBox = (EditText)layout.findViewById(R.id.send_message_dialog_msg);

		alertDialogBuilder.setView(layout);
		alertDialogBuilder.setPositiveButton("Send", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				final Map<String, Object> data = new HashMap<String, Object>();
				data.put("title", "사용자 메시지");
				data.put("body", msgBox.getText().toString());
				final JSONObject jsonData = new JSONObject(data);
				final String notificationData = jsonData.toString();

				// Create the intent to house the Pebble notification
				final Intent intent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
				intent.putExtra("messageType", Constants.PEBBLE_MESSAGE_TYPE_ALERT);
				intent.putExtra("sender", "custom message");
				intent.putExtra("notificationData", notificationData);

				sendBroadcast(intent);

				msgBox.setText("");
			}
		});
		alertDialogBuilder.setNegativeButton("Cancel", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		sendMessageDialog = alertDialogBuilder.create();
	}

	private void checkIsEnabledNotiService() {
		AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Service.ACCESSIBILITY_SERVICE);
		// feedbackSpoken|feedbackHaptic|feedbackVisual|feedbackGeneric
		List<AccessibilityServiceInfo> installedServices = accessibilityManager
				.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);

		boolean found = false;
		for (AccessibilityServiceInfo serviceInfo : installedServices) {
			String key = serviceInfo.getId();
			Log.d("", key);
			if (key.equals(Constants.NOTISERVICE_NAME)) {
				found = true;
				break;
			}
		}
		if (!found) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle("Warning");
			builder.setMessage(getString(R.string.need_to_accessibility_setup));
			builder.setPositiveButton("Open Setting", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
					startActivityForResult(intent, 0);
				}
			});
			builder.setNegativeButton("Confirm", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			builder.show();
		}
	}

	private void initAppTable() {
		final SharedPreferences preferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
		TableLayout table = (TableLayout) findViewById(R.id.appList);
		table.removeAllViewsInLayout();
		
		final PackageManager packageManager = getPackageManager();
		//get a list of installed apps.
		List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

		for (final ApplicationInfo packageInfo : packages) {
			/*if ((packageInfo.flags & ApplicationInfo.FLAG_) != 0) {
				continue;
			}*/


			TableRow row = (TableRow)LayoutInflater.from(MainActivity.this)
					.inflate(R.layout.activity_main_installed_app_table_row, null);
			ImageView rowIcon = (ImageView)row.findViewById(R.id.installed_app_icon);
			TextView rowName = (TextView)row.findViewById(R.id.installed_app_name);
			Switch rowSwitch = (Switch)row.findViewById(R.id.installed_app_switch);

			rowIcon.setImageDrawable(packageInfo.loadIcon(getPackageManager()));
			rowName.setText(packageInfo.loadLabel(getPackageManager()).toString());
			if (preferences.contains(packageInfo.packageName)) {
				rowSwitch.setChecked(preferences.getBoolean(packageInfo.packageName, false));
			}
			rowSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				final String packageName = String.copyValueOf(packageInfo.packageName.toCharArray());
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					Editor editor = preferences.edit();
					if (isChecked) {
						editor.putBoolean(packageName, isChecked);
					} else if (preferences.contains(packageName)) { 
						editor.remove(packageName);	
					}
					editor.commit();
				}
			});

			table.addView(row);
		}

	}

}
