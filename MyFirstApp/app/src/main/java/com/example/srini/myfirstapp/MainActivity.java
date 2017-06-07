package com.example.srini.myfirstapp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
//import com.microsoft.azure.iothub.Message;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class MainActivity extends AppCompatActivity {
    int i;
    private static String connString = "<PulseCheckerDevice - ConnectionString>";
    private static IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private static String deviceId = "<PulseCheckerDevice - DeviceId>";
    private static DeviceClient client;
    private Handler handler;
    private MessageSender sender;
    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final TextView view = (TextView) findViewById(R.id.textView2);
        final ScrollView scrollview = (ScrollView)findViewById(R.id.textPanel);
        view.setText("");
        Button b = (Button)findViewById(R.id.buttonSTOP);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sender.stopThread=true;
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }});

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                TextView myTextView =
                        (TextView)findViewById(R.id.textView2);
                myTextView.append(Html.fromHtml(msg.getData().getString("message")+"\n"));
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }};

        try {
            client = new DeviceClient(connString, protocol);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            client.open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sender = new MessageSender(handler);
        sender.start();


    }
    @Override
    protected  void onStart()
    {
        super.onStart();


    }
    private static class TelemetryDataPoint {
        public String deviceId;
        public double pulseRate;

        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    private static class EventCallback implements IotHubEventCallback
    {

        public Handler handler;
        public void execute(IotHubStatusCode status, Object context) {
            //System.out.println("IoT Hub responded to message with status: " + status.name());

            Message msgUI = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("message","<b>IoT Hub responded to message with status:</b> " + status.name()+"<br/>----------EOM----------<br/><br/>" );
            msgUI.setData(bundle);
            handler.sendMessage(msgUI);
            if (context != null) {
                synchronized (context) {
                    context.notify();
                }
            }
        }
    }
    private static class MessageSender extends Thread {
        public volatile boolean stopThread = false;
        private Handler handler;
        public MessageSender(Handler handler)
        {
            this.handler=handler;
        }
        public void run()  {
            try {
                double avgPulseRate = 10; // m/s
                Random rand = new Random();

                while (!stopThread) {
                    double pulseRate = avgPulseRate + rand.nextDouble() * 4 - 4;
                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.deviceId = deviceId;
                    telemetryDataPoint.pulseRate = pulseRate;

                    String msgStr = telemetryDataPoint.serialize();
                    com.microsoft.azure.sdk.iot.device.Message msg = new com.microsoft.azure.sdk.iot.device.Message(msgStr);
                    if(pulseRate<6.5) {
                        msg.setProperty("level", "critical");
                        msgStr = "<font color='Red'>"+msgStr+"</font>";
                        //msgStr = Html.fromHtml(msgStr);
                    }

                    Message msgUI = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("message","<br/>"+"<b>Sending:</b> " + msgStr+"<br/>");
                    msgUI.setData(bundle);
                    handler.sendMessage(msgUI);


                    Object lockobj = new Object();
                    EventCallback callback = new EventCallback();
                    callback.handler=handler;
                    client.sendEventAsync(msg, callback, lockobj);

                    synchronized (lockobj) {
                        lockobj.wait();
                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {

                Message msgUI = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("message",e.getMessage() );
                msgUI.setData(bundle);
                handler.sendMessage(msgUI);
                //handler.sendMessage(msgUI);
            }
        }
    }


}
