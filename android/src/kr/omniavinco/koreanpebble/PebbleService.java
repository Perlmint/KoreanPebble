package kr.omniavinco.koreanpebble;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.util.Log;

class Message extends Object {
	static long idCounter = (long) 0;
	public final String title, msg;
	public final boolean vibe;
	public final long id;
	private final byte[] ListToArray(List<Byte> tmpBuf) {
		byte ret[] = null;
		if (tmpBuf.size() == 0) {
			ret = new byte[1];
			ret[0] = 0;
			return ret;
		}
		ret = new byte[tmpBuf.size()];
		int i = 0;
		for(Byte ch : tmpBuf) {
			if (i > 255) {
				break;
			}
			ret[i++] = ch;
		}
		return ret;
	}
	
	public Message(String title_, String message_, Boolean vibe_) {
		/*
		List<Byte> tmpBuf = PebbleUtil.unicode2han_str(title_);
		title = ListToArray(tmpBuf);
		tmpBuf = PebbleUtil.unicode2han_str(message_);
		msg = ListToArray(tmpBuf);
		*/
		title = title_;
		msg = message_;
		
		vibe = vibe_;
		
		if (idCounter < 5000) {
			id = ++idCounter;
		} else {
			id = idCounter = (long) 0;
		}
		
	}
}

public class PebbleService extends Service {
	final static UUID pebbleAppId = UUID.fromString("982492E8-50DB-4AE3-A69A-1B43E012C93F");
	PebbleKit.PebbleNackReceiver nackReceiver;
	PebbleKit.PebbleDataReceiver dataReceiver;
	PebbleKit.PebbleAckReceiver ackReceiver;
	BroadcastReceiver androidReceiver;
	
	final static byte PROTOCOL_TYPE_PING = 0, PROTOCOL_TYPE_NOTIFICATION = 1, PROTOCOL_TYPE_DIAL = 2;
	
	int transactionId = 1;
	
	List<Message> messageStack, sendedMessageStack;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("service", "create");
		messageStack = new ArrayList<Message>();
		sendedMessageStack = new ArrayList<Message>();
		registerReceivers();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		registerReceivers();
		return null;
	}
	
	public void sendMessage(String title, String msg, Boolean vibe) {
		messageStack.add(new Message(title, msg, vibe));
		PebbleDictionary pingData = new PebbleDictionary();
		pingData.addUint8(0, PROTOCOL_TYPE_PING);
		PebbleKit.sendDataToPebbleWithTransactionId(PebbleService.this, pebbleAppId, pingData, 0);
	}
	
	private void sendMessage(Message msg) {
		PebbleDictionary sendData = new PebbleDictionary();
		sendData.addUint8(0, PROTOCOL_TYPE_NOTIFICATION);
		sendData.addUint8(1, (byte) (msg.vibe?1:0));
		sendData.addString(2, msg.title);
		sendData.addString(3, msg.msg);
		sendData.addUint16(4, (short) messageStack.size());
		sendData.addUint16(5, (short) messageStack.indexOf(msg));
		Log.e("notification", String.format("%d size", (msg.title.length() + msg.msg.length() + 6)));
		PebbleKit.sendDataToPebbleWithTransactionId(PebbleService.this, pebbleAppId, sendData, transactionId);
		if (transactionId < Integer.MAX_VALUE) {
			transactionId++;
		} else {
			transactionId = 1;
		}
		messageStack.remove(msg);
		sendedMessageStack.add(msg);
	}
	
	void registerReceivers() {
		androidReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals("kr.omniavinco.koreanpebble.SEND_NOTIFICATION")) {
					if (intent.hasExtra("notificationData")) {
						JSONObject jsonData = null;
						try {
							jsonData = new JSONObject(intent.getStringExtra("notificationData"));
							sendMessage(jsonData.getString("title"), jsonData.getString("body"), true);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				} else if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
					Bundle bundle = intent.getExtras();
                    if (bundle == null) {
                           return;
                    }
                    
                    Object[] pdusObj = (Object[]) bundle.get("pdus");
                    if (pdusObj == null) {
                           return;
                    }

                    //message 처리
                    SmsMessage[] smsMessages = new SmsMessage[pdusObj.length];
                    for (int i = 0; i < pdusObj.length; i++) {
                           smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                           String sender = smsMessages[i].getOriginatingAddress();
                           String message = new String();
                           if (sender != null) {
                        	   message.concat(sender);
                           }
                           message.concat("\n");
                           message.concat(smsMessages[i].getMessageBody());
                           sendMessage("새 문자 메시지", message, true);
                    }
				}
			}
		};
		registerReceiver(androidReceiver, new IntentFilter(Constants.INTENT_SEND_PEBBLE_NOTIFICATION));
		ackReceiver = new PebbleKit.PebbleAckReceiver(pebbleAppId) {
			
			@Override
			public void receiveAck(Context context, int transactionId) {
				Log.e("koreanPebble", "ack");
			}
		};
        PebbleKit.registerReceivedAckHandler(this, ackReceiver);
        nackReceiver = new PebbleKit.PebbleNackReceiver(pebbleAppId) {
			
			@Override
			public void receiveNack(Context context, int transactionId) {
				if (transactionId == 0) {
					PebbleKit.startAppOnPebble(PebbleService.this, pebbleAppId);
					Log.e("service", "App is not running, start App");
				}
			}
		};
        PebbleKit.registerReceivedNackHandler(this, nackReceiver);
        dataReceiver = new PebbleKit.PebbleDataReceiver(pebbleAppId) {
			
			@Override
			public void receiveData(Context context, int transactionId,
					PebbleDictionary data) {
				if (!data.contains(0)) {
					Log.e("service", "no message");
					PebbleKit.sendNackToPebble(context, transactionId);
					return;
				}
				switch (data.getUnsignedInteger(0).intValue()) {
				case 0: // pebble app init, get latest message
				{
					if (messageStack.size() == 0) {
						PebbleKit.sendNackToPebble(context, transactionId);
					} else {
						PebbleKit.sendAckToPebble(context, transactionId);
						Message recentMessage = messageStack.get(messageStack.size() - 1);
						sendMessage(recentMessage);
					}
					break;
				}
				case 1: // read - 1 has id;
				{
					Log.e("service", "read");
					Long readId = data.getUnsignedInteger(1);
					Message foundMessage = null;
					for(Message msg : sendedMessageStack) {
						if (msg.id == readId) {
							foundMessage = msg;
						}
					}
					if (foundMessage != null) {
						PebbleKit.sendAckToPebble(context, transactionId);
						sendedMessageStack.remove(foundMessage);
					} else {
						PebbleKit.sendNackToPebble(context, transactionId);
					}
					break;
				}
				}
				
			}
		};
		PebbleKit.registerReceivedDataHandler(this, dataReceiver);
	}

}
