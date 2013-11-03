package kr.omniavinco.koreanpebble;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;

public class PebbleReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Boot Intent Handler
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			Log.i("KoreanPebble", "BootCompleted");
			
			Intent serviceIntent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
			context.startService(serviceIntent);
		}
		else if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") ||
				intent.getAction().equals("android.provider.Telephony.MMS_RECEIVED"))
		{
			NotiSmsMms(context, intent);
		}
	}
	
	private void NotiSmsMms(Context context, Intent intent)
	{
		// referenced http://msatpathy.wordpress.com/android/send-and-receiving-sms-%E2%80%93-in-android/
		Bundle bundle = intent.getExtras();
		SmsMessage[] msgs = null;
		if (bundle != null)
		{
			Object[] pdus = (Object[])bundle.get("pdus");
			msgs = new SmsMessage[pdus.length]; 
			for (int i=0; i<msgs.length; i++)
			{
				msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]); 
				
				String msg = "";
				msg += "SMS From " + msgs[i].getOriginatingAddress() + " :" + msgs[i].getDisplayMessageBody() + "\n";
				Log.d("PebbleReceiver", msg);
				
				String sender = msgs[i].getDisplayOriginatingAddress();
				
				// scan the name from addressbook
				String senderName = GetNameFromPhoneNumber(context, sender);
				
				// if name is found, attach the name before the sender phone number
				if (senderName != null)
				{
					sender = senderName;
				}
				
				// Create json object to be sent to Pebble
				final Map<String, Object> data = new HashMap<String, Object>();
				data.put("title", sender);
				data.put("body", msgs[i].getDisplayMessageBody());
				final JSONObject jsonData = new JSONObject(data);
				final String notificationData = jsonData.toString();
	
				// Create the intent to house the Pebble notification
				final Intent notiIntent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
				notiIntent.putExtra("messageType", Constants.PEBBLE_MESSAGE_TYPE_ALERT);
				notiIntent.putExtra("notificationData", notificationData);
				context.sendBroadcast(notiIntent);
			}
		}
	}
	
	private String GetNameFromPhoneNumber(Context context, String phoneNumber)
	{
		Uri uri;
		uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		String[] projection = new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME };
		Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
		
		String result = null;
		if (cursor != null)
		{
			if (cursor.moveToFirst())
			{
				result = cursor.getString(0);
			}
			cursor.close();
		}
		return result;
	}
}
