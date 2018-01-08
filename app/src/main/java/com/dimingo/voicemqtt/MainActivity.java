package com.dimingo.voicemqtt;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

	private TextView txvResult;
	private TextView rxvResult;
	private CheckBox checkBoxAudio;
	private TextToSpeech tts;
	private MqttAndroidClient mqttAndroidClient;
	private PahoMqttClient pahoMqttClient;
	private String topicPrefix;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
		setContentView(R.layout.activity_main);
		txvResult = (TextView) findViewById(R.id.txvResult);
		rxvResult = (TextView) findViewById(R.id.rxvResult);
		checkBoxAudio = (CheckBox) findViewById(R.id.checkBoxAudio);
		checkBoxAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				EventBus.getDefault().post(new BooleanEvent(isChecked));
			}
		});
		tts = new TextToSpeech(this, this);
		Log.i("TTS", "onCreate");
		Log.i("TTS", tts.getEngines().toString());

		pahoMqttClient = new PahoMqttClient();
		mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String s) {
                if (reconnect) {
                    setMessageNotification("debug", "main: reconnected");
                }
                try {
                    pahoMqttClient.subscribe(mqttAndroidClient, topicPrefix + Constants.SUBSCRIBE_TOPIC, Constants.QOS);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                setMessageNotification("debug", "main: connection lost");
                try {
                    mqttAndroidClient.connect(pahoMqttClient.getMqttConnectionOption());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String message = new String(mqttMessage.getPayload());
                setMessageNotification(topic, message);
//                EventBus.getDefault().post(new MessageEvent(message));
//                if (checkBoxAudio) {
//                    setMessageNotification("debug", "checkBoxAudio = true");
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
//                    }
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });

		if (savedInstanceState == null) {
			Bundle extras = getIntent().getExtras();
			if (extras == null) {
				Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
				topicPrefix = "public";
			} else {
				topicPrefix = extras.getString("email");
			}
		} else {
			topicPrefix = savedInstanceState.getString("topicPrefix");
		}

        setMessageNotification("debug", "topixPrefix = " + topicPrefix);

		Intent intent = new Intent(MainActivity.this, MqttMessageService.class);
		intent.putExtra("topicPrefix", topicPrefix);
		startService(intent);
	}

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("topicPrefix", topicPrefix);
//        outState.putBoolean("isAudioEnabled", checkBoxAudio.isChecked());
    }


    public void getSpeechInput(View view) {

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(intent, 10);
		} else {
			Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case 10:
				if (resultCode == RESULT_OK && data != null) {
					ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					txvResult.setText(result.get(0));
					speakOut();
				}
				break;
		}
	}

	@Override
	public void onInit(int status) {

		if (status == TextToSpeech.SUCCESS) {

			int result = tts.setLanguage(Locale.getDefault());
//			int result = tts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "This Language is not supported");
                Toast.makeText(this, "This Language is not supported", Toast.LENGTH_SHORT).show();
			} else {
//				speakOut();
                Toast.makeText(this, "TTS initialized successfully", Toast.LENGTH_SHORT).show();
			}

		} else {
			Log.e("TTS", "Initialization Failed!");
		}
	}

	private void speakOut() {

		CharSequence text = txvResult.getText();

		if (checkBoxAudio.isChecked()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
			}
		}

        EventBus.getDefault().post(new BooleanEvent(checkBoxAudio.isChecked()));

		String msg = text.toString();
		Log.i("TTS", msg);

        if (!mqttAndroidClient.isConnected()) {
            Toast.makeText(this, "disconnected --> reconnecting...", Toast.LENGTH_SHORT).show();
//			mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
            try {
                mqttAndroidClient.connect(pahoMqttClient.getMqttConnectionOption());
            } catch (MqttException e) {
                Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
//		else {
//            try {
//                mqttAndroidClient.connect(pahoMqttClient.getMqttConnectionOption());
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        }

		try {
            Toast.makeText(this, "publishing to " + topicPrefix, Toast.LENGTH_SHORT).show();
			pahoMqttClient.publishMessage(mqttAndroidClient, msg, Constants.QOS, topicPrefix + Constants.PUBLISH_TOPIC);
		} catch (MqttException | UnsupportedEncodingException e) {
            Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	// This method will be called when a MessageEvent is posted
	@Subscribe
	public void onMessageEvent(MessageEvent event) {
		rxvResult.setText(event.message.trim());
	}

	@Override
	protected void onStart() {
		super.onStart();
//		EventBus.getDefault().register(this);
	}

//	@Override
//	protected void onResume() {
//		super.onResume();
//		if (!mqttAndroidClient.isConnected()) {
//			mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
//		}
//	}

	@Override
	protected void onStop() {
//		EventBus.getDefault().unregister(this);
		super.onStop();
	}

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    private void setMessageNotification(@NonNull String topic, @NonNull String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_message_black_24dp)
                        .setContentTitle(topic)
                        .setContentText(msg);
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(100, mBuilder.build());
    }
}
