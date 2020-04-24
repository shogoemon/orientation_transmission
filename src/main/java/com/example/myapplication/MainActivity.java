package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.util.Log;
import android.util.Log;
import android.widget.TextView;

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

import static java.net.HttpURLConnection.HTTP_OK;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {
    //センサーを管理
    private SensorManager sensorManager;
    //テキストの表示部
    private TextView textInfo, accelTextView, lightTextView, orientationTextView, proximityTextView, ipTextView;

    private static final String TAG = "MYSERVER";
    private WebServer server;
    // サーバのポートを8080に設定
    private static final int PORT = 8080;
    String ipAddress;
    String orientationSt="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        textInfo = findViewById(R.id.text_info);

        // Get an instance of the TextView
        accelTextView = findViewById(R.id.accel_text_view);
        lightTextView= findViewById(R.id.light_text_view);
        orientationTextView= findViewById(R.id.orientation_text_view);
        proximityTextView= findViewById(R.id.proximity_text_view);

        ipTextView = (TextView) findViewById(R.id.textView1);
        ipAddress = getLocalIpAddress();
        if (ipAddress != null) {
            ipTextView.setText("Please Access:" + "http://" + ipAddress + ":" + PORT);
        } else
            ipTextView.setText("Wi-Fi Network Not Available");
        server = new WebServer();
        try {
            // サーバをスタート
            server.start();
        } catch (IOException ioe) {
            Log.w(TAG, "The server could not start.");
        }
        Log.w(TAG, "Web server initialized.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null)
            /*
             * 注意 アクティビティを破棄する時は サーバをストップする
             */
            server.stop();
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
            Log.d(TAG, ex.toString());
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        Sensor proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
//        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
//        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Listenerを解除
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float sensorX, sensorY, sensorZ;
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:{

                sensorX = event.values[0];
                sensorY = event.values[1];
                sensorZ = event.values[2];

                String strTmp = "加速度センサー\n"
                        + " X: " + sensorX + "\n"
                        + " Y: " + sensorY + "\n"
                        + " Z: " + sensorZ + "\n";
                accelTextView.setText(strTmp);

                showInfo(event);
                break;
            }
            case Sensor.TYPE_LIGHT:{
                //Log.i("light", Integer.toString(Sensor.TYPE_LIGHT));
                String strTmp="明るさ(lux)\n"+(int)(event.values[0])+"lux\n";
                lightTextView.setText(strTmp);
                break;
            }
            case Sensor.TYPE_ORIENTATION:{
                //Log.i("orientation", Integer.toString(Sensor.TYPE_orientation_VECTOR));
                sensorX = event.values[0];
                sensorY = event.values[1];
                sensorZ = event.values[2];

                String strTmp = "傾き(°)\n"
                        + " X: " + sensorX + "\n"
                        + " Y: " + sensorY + "\n"
                        + " Z: " + sensorZ + "\n";
                orientationTextView.setText(strTmp);

                orientationSt="orientation<br>"
                        + " X: " + sensorX + "<br>"
                        + " Y: " + sensorY + "<br>"
                        + " Z: " + sensorZ + "<br>";

                break;
            }
            case Sensor.TYPE_PROXIMITY:{
                String strTmp="近接(cm)\n"+(int)(event.values[0])+"cm\n";
                proximityTextView.setText(strTmp);
                break;
            }
        }

    }

    // （お好みで）加速度センサーの各種情報を表示
    private void showInfo(SensorEvent event){
        // センサー名
        StringBuffer info = new StringBuffer("Name: ");
        info.append(event.sensor.getName());
        info.append("\n");

        // ベンダー名
        info.append("Vendor: ");
        info.append(event.sensor.getVendor());
        info.append("\n");

        // 型番
        info.append("Type: ");
        info.append(event.sensor.getType());
        info.append("\n");

        // 最小遅れ
        int data = event.sensor.getMinDelay();
        info.append("Mindelay: ");
        info.append(data);
        info.append(" usec\n");

//        // 最大遅れ
//        data = event.sensor.getMaxDelay();
//        info.append("Maxdelay: ");
//        info.append(data);
//        info.append(" usec\n");

//        // レポートモード
//        data = event.sensor.getReportingMode();
//        String stinfo = "unknown";
//        if(data == 0){
//            stinfo = "REPORTING_MODE_CONTINUOUS";
//        }else if(data == 1){
//            stinfo = "REPORTING_MODE_ON_CHANGE";
//        }else if(data == 2){
//            stinfo = "REPORTING_MODE_ONE_SHOT";
//        }
//        info.append("ReportingMode: ");
//        info.append(stinfo);
//        info.append("\n");

        // 最大レンジ
        info.append("MaxRange: ");
        float fData = event.sensor.getMaximumRange();
        info.append(fData);
        info.append("\n");

        // 分解能
        info.append("Resolution: ");
        fData = event.sensor.getResolution();
        info.append(fData);
        info.append(" m/s^2\n");

        // 消費電流
        info.append("Power: ");
        fData = event.sensor.getPower();
        info.append(fData);
        info.append(" mA\n");

        textInfo.setText(info);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
            String answer = "<html><head><head><body><h1>"+orientationSt+"</h1></body></html>";
            InputStream ansStream=null;

            try{
                ansStream = new ByteArrayInputStream(answer.getBytes("utf-8"));
            }catch (UnsupportedEncodingException error){

            }
            Log.d("WebServerClass","before return Response");
            return NanoHTTPD.newChunkedResponse(Response.Status.OK, MIME_HTML, ansStream);
        }
    }

}