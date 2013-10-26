package kr.omniavinco.koreanpebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PebbleReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			Log.i("cccc", "received");
			Intent serviceIntent = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
			context.startService(serviceIntent);
		}
	}

}
