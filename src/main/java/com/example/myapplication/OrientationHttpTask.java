package com.example.myapplication;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class OrientationHttpTask extends AsyncTask<String,Void,String> {
    public OrientationHttpTask(TextView view,String clientParam){
        diffOrientationTextView=view;
        clientOrientation = clientParam;
    }
    TextView diffOrientationTextView;
    String serverOrientation="";//Stringでなく配列
    String clientOrientation;//Stringでなく配列

    protected String doInBackground(String... url){
        try {
            serverOrientation=httpGetRequest(
                    url[0]+"?"+ clientOrientation,//urlの後ろにクライアントのパラメータ
                    "UTF-8",
                    null);
            Log.d("json",serverOrientation);
        }catch (IOException e){

        }
        return serverOrientation;
    }

    protected void onPostExecute(String result){
        //Log.d("jsonLast",serverOrientation);
        float sensorX, sensorY, sensorZ;
        JSONObject serverOrientationJson;
        JSONObject clientOrientationJson;
        try {
            serverOrientationJson=new JSONObject(result);
            clientOrientationJson=new JSONObject(clientOrientation);
//            sensorX=clientOrientationJson.getString("x")-serverOrientationJson.getString("x");
//            sensorY=clientOrientationJson-serverOrientationJson.getString("y");
//            sensorZ=clientOrientationJson-serverOrientationJson.getString("z");

        }catch(JSONException e){

        }
        //画面に計算結果を反映
//        diffOrientationTextView.setText(
//        "x"+x+"y"+y+"z"+z
//        );
    }

    public String httpGetRequest(String endpoint, String encoding, Map<String, String> headers) throws IOException {

        final int TIMEOUT_MILLIS = 0;// タイムアウトミリ秒：0は無限

        final StringBuffer sb = new StringBuffer("");

        HttpURLConnection httpConn = null;
        BufferedReader br = null;
        InputStream is = null;
        InputStreamReader isr = null;

        try {
            URL url = new URL(endpoint);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setConnectTimeout(TIMEOUT_MILLIS);// 接続にかかる時間
            httpConn.setReadTimeout(TIMEOUT_MILLIS);// データの読み込みにかかる時間
            httpConn.setRequestMethod("GET");// HTTPメソッド
            httpConn.setUseCaches(false);// キャッシュ利用
            httpConn.setDoOutput(false);// リクエストのボディの送信を許可(GETのときはfalse,POSTのときはtrueにする)
            httpConn.setDoInput(true);// レスポンスのボディの受信を許可

            // HTTPヘッダをセット
            if (headers != null) {
                for (String key : headers.keySet()) {
                    httpConn.setRequestProperty(key, headers.get(key));// HTTPヘッダをセット
                }
            }
            httpConn.connect();

            final int responseCode = httpConn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {

                is = httpConn.getInputStream();
                isr = new InputStreamReader(is, encoding);
                br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            } else {
                // If responseCode is not HTTP_OK
            }

        } catch (IOException e) {
            throw e;
        } finally {
            // fortify safeかつJava1.6 compliantなclose処理
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return sb.toString();
    }
}
