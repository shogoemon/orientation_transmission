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
        implements SensorEventListener,OrientationHttpTask.CallBackTask {
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
    String url="";

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
            if(ipFormView.getText().toString().equals(ipAddress + ":" + PORT)){
                String lastChar=ipAddress.substring(ipAddress.length()-1);
                ipAddress=ipAddress.substring(0,ipAddress.length()-1)+(Integer.parseInt(lastChar)+1);
                ipFormView.setText(ipAddress + ":" + PORT);
            }
            Log.d("fromOnCreate",ipFormView.getText().toString());
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
        final OrientationHttpTask.CallBackTask callBackTask=this;
        ioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ioButton.getText().toString().equals("start")){
                    //角度差Viewを可視化
                    diffOrientationTextView.setVisibility(View.VISIBLE);
                    ioButton.setText(R.string.StopButton);

                    url="http://"+ipFormView.getText();
                    OrientationHttpTask ohTask= new OrientationHttpTask(diffOrientationTextView,orientationJson);
                    ohTask.setCallbackTask(callBackTask);
                    ohTask.execute(url);

                    Log.d("fromMainActivity",url);
                }else{
                    ioButton.setText(R.string.StartButton);
                    isServer=null;
                    //サーバーである場合は停止signalをレスポンスで送信
                    //クライアントである場合はループを停止
                }
            }
        });
    }

    @Override
    public void callbackFunction(String result){
        if(result.equals("end")){
            //ループ停止
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ioButton.setText(R.string.StartButton);
                    diffOrientationTextView.setVisibility(View.GONE);
                }
            });
        }else{
            if(isServer==null){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ioButton.setText(R.string.StartButton);
                    }
                });
                isServer=false;
                OrientationHttpTask ohTask= new OrientationHttpTask(diffOrientationTextView,"end");
                ohTask.setCallbackTask(this);
                ohTask.execute(url);
            }else{
                //ループ
                OrientationHttpTask ohTask= new OrientationHttpTask(diffOrientationTextView,orientationJson);
                ohTask.setCallbackTask(this);
                ohTask.execute(url);
            }
        }
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
            String clientParam=parameters.get("NanoHttpd.QUERY_STRING");
            if(clientParam==null){
                clientParam="";
            }
            if(clientParam.equals("end")||isServer==null){
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

                if(ioButton.getText().toString().equals("start")){
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