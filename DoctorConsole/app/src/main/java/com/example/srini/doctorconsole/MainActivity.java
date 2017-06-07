package com.example.srini.doctorconsole;

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
import com.microsoft.azure.sdk.iot.device.MessageCallback;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    private static String connString = "<DoctorConsoleDevice - Connectionstring>";
    private static IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private static String deviceId = "<DoctorConsoleDevice - DeviceID>";
    private static DeviceClient client;
    private static Handler handler;
    private static MessageCallback callback;
    private static String divEncloser = "<div style='margin:10px;border-style:solid;border-color:#000000;border-width:1px;' ><u><b>Alert message from IoTHub</b></u><br/>%s</div>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ScrollView scrollview = (ScrollView)findViewById(R.id.scroll_view);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                TextView myTextView =
                        (TextView)findViewById(R.id.textView2);
                myTextView.append(Html.fromHtml( String.format(divEncloser,msg.getData().getString("message"))));
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }};

        try {
            client = new DeviceClient(connString, protocol);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        callback = new AppMessageCallback(handler);
        client.setMessageCallback(callback, null);
        try {
            client.open();
        } catch (IOException e) {
            e.printStackTrace();
        }




        Button b = (Button)findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                   client.close();
                } catch (Exception e) {//catch (IOException e) {
                    e.printStackTrace();
                }
            }});

    }

    private static class AppMessageCallback implements MessageCallback {
        private static Handler handler;
        public AppMessageCallback(Handler handler){
            this.handler=handler;
        }
        public IotHubMessageResult execute(com.microsoft.azure.sdk.iot.device.Message msg, Object context) {
            Message msgUI = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("message",new String(msg.getBytes(), com.microsoft.azure.sdk.iot.device.Message.DEFAULT_IOTHUB_MESSAGE_CHARSET) );
            msgUI.setData(bundle);
            handler.sendMessage (msgUI);
            return IotHubMessageResult.COMPLETE;
        }
    }


}
