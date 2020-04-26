package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {
    //センサーを管理
    private SensorManager sensorManager;
    //テキストの表示部
    private TextView orientationTextView,diffOrientationTextView;
    private Button ioButton;
    private EditText ipFormView;

    private WebServer server;
    // サーバのポートを8080に設定
    private static final int PORT = 8080;
    String ipAddress;
    String orientationJson="";
    Boolean isServer=false;
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get an instance of the TextView
        orientationTextView= findViewById(R.id.orientation_text_view);
        diffOrientationTextView= findViewById(R.id.orientation_diff_text_view);
        ioButton=findViewById(R.id.ioButton);
        ipFormView=findViewById(R.id.ipForm);

        TextView ipTextView = findViewById(R.id.textView1);
        ipAddress = getLocalIpAddress();

        if (ipAddress != null) {
            ipTextView.setText("Please Access:" + "http://" + ipAddress + ":" + PORT);
        } else
            ipTextView.setText("Wi-Fi Network Not Available");
        server = new WebServer();
        try {
            // サーバをスタート
            server.start();
        } catch (IOException ioe) { }
        addClickListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null)
             // 注意 アクティビティを破棄する時は サーバをストップする
            server.stop();
        sensorManager.unregisterListener(this);
    }

    /* 端末のIPを取得 */
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {

                        String ipAddr = inetAddress.getHostAddress();
                        return ipAddr;
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        Sensor orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float sensorX, sensorY, sensorZ;
        sensorX = event.values[0];
        sensorY = event.values[1];
        sensorZ = event.values[2];

        String strTmp = "傾き(°)\n"
                + " X: " + sensorX + "\n"
                + " Y: " + sensorY + "\n"
                + " Z: " + sensorZ + "\n";
        orientationTextView.setText(strTmp);

        orientationJson="{"+
                 "X:" + sensorX +","+
                 "Y:" + sensorY +","+
                 "Z:" + sensorZ + "}";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void addClickListener(){
        ioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ioButton.getText().toString().equals("start")){
                    //角度差Viewを可視化
                    diffOrientationTextView.setVisibility(View.VISIBLE);
                    ioButton.setText(R.string.StopButton);
                    //String url="https://www.instagram.com/shibuya.shogo/?__a=1";
                    String url="http://"+ipFormView.getText();
                    new OrientationHttpTask(diffOrientationTextView,orientationJson).execute(url);
                    //別スレッドでループ処理
                    Log.d("fromMainActivity",url);
                }else{
                    diffOrientationTextView.setVisibility(View.GONE);
                    ioButton.setText(R.string.StartButton);
                    if(isServer){
                        //サーバーである場合は停止signalをレスポンスで送信
                        isServer=null;
                    }else{
                        //クライアントである場合はループを停止
                    }
                }
            }
        });
    }

    /*
     * NanoHTTPDを継承したクラスを作る
     */

    private class WebServer extends NanoHTTPD {
        public WebServer() {
            super(PORT);
        }

        @Override
        public Response serve(String uri, Method method,
                              Map<String, String> header, Map<String, String> parameters,
                              Map<String, String> files) {
            InputStream jsonStream=null;
            //Log.d("requestUri",parameters.get("NanoHttpd.QUERY_STRING"));
            String clientParam=parameters.get("NanoHttpd.QUERY_STRING");
            if(clientParam==null){
                clientParam="";
            }
            if(clientParam.contains("end")){
                //ボタンをstartに変える（クライアントになれる状態）
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ioButton.setText(R.string.StartButton);
                    }
                });
                isServer=false;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        diffOrientationTextView.setVisibility(View.GONE);
                    }
                });
                //Serverからendを送信したらクライアントからもendを送信
            }else{
                //clientParamを画面に反映
                float sensorX=0.0f, sensorY=0.0f, sensorZ=0.0f;
                JSONObject serverOrientationJson;
                JSONObject clientOrientationJson;
                try{
                    serverOrientationJson=new JSONObject(orientationJson);
                    clientOrientationJson=new JSONObject(clientParam);
                    sensorX=Float.parseFloat(clientOrientationJson.getString("X")) -
                            Float.parseFloat(serverOrientationJson.getString("X"));
                    sensorY=Float.parseFloat(clientOrientationJson.getString("Y")) -
                            Float.parseFloat(serverOrientationJson.getString("Y"));
                    sensorZ=Float.parseFloat(clientOrientationJson.getString("Z")) -
                            Float.parseFloat(serverOrientationJson.getString("Z"));
                }catch (JSONException e){
                    Log.d("fromMainActivity",e.toString());
                }
                final String strTmp = "傾き(°)server\n"
                        + " X: " + sensorX + "\n"
                        + " Y: " + sensorY + "\n"
                        + " Z: " + sensorZ + "\n";
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        diffOrientationTextView.setText(strTmp);
                    }
                });
                Log.d("fromMainActivity",strTmp);
                switch (ioButton.getText().toString()){
                    case "start":{
                        if(isServer!=null){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ioButton.setText(R.string.StopButton);
                                }
                            });
                            Log.d("isServer",isServer.toString());
                            isServer=true;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    diffOrientationTextView.setVisibility(View.VISIBLE);
                                }
                            });
                        }else{
                            isServer=false;
                        }
                        break;
                    }
                    case "stop":{break;}
                }
            }
            System.setProperty("http.keepAlive", "false");
            try{
                if(isServer){
                    jsonStream = new ByteArrayInputStream(orientationJson.getBytes("utf-8"));
                }else{
                    jsonStream = new ByteArrayInputStream("end".getBytes("utf-8"));
                }
            }catch (UnsupportedEncodingException error){

            }
            return NanoHTTPD.newChunkedResponse(Response.Status.OK,"application/json", jsonStream);
        }
    }
}