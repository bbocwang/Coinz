package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.util.IOUtils;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import io.grpc.internal.IoUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GameActivity extends AppCompatActivity {
    private final String tag = "GameActivity";

    Calendar calendar = Calendar.getInstance();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    private String currentDate = dateFormat.format(calendar.getTime());
    private String downloadDate = "";//Format:YYYY/MM/DD
    private final String preferencesFile = "LastDownloadDate";//for storing preferences
    private final String geojsonFile = "json";//for storing preferences
    public String json = "";

    private MapView mapView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_game);
        mapView = (MapView)findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);


    }

    private void downloadjson() {
        DownloadFileTask downloadFileTask = new DownloadFileTask();
        //http://homepages.inf.ed.ac.uk/stg/coinz/2018/09/28/coinzmap.geojson
        if(currentDate != downloadDate) {
            downloadDate = currentDate;
            Log.d(tag, "[Update downloadDATE]  downloadDate="+downloadDate);
            SharedPreferences settings = getSharedPreferences(preferencesFile,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("lastDownloadDate",downloadDate);
            editor.apply();
            downloadFileTask.execute("http://homepages.inf.ed.ac.uk/stg/coinz/"
                    + currentDate+"/coinzmap.geojson");
            try {
                json = downloadFileTask.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(tag, "[Downloadjson]  downloading from "+"http://homepages.inf.ed.ac.uk/stg/coinz/"
                    + currentDate+"/coinzmap.geojson");
        }
        else {
            //restore preferences
            SharedPreferences settings = getSharedPreferences(preferencesFile,
                    Context.MODE_PRIVATE);

            //use""as default value
            json = settings.getString("json","");
            Log.d(tag, "[downloadFileTask] Recalled json file");
        }

    }

    @Override
    public void onStart(){
        super.onStart();
        mapView.onStart();

        //restore preferences
        SharedPreferences settings = getSharedPreferences(preferencesFile,
                Context.MODE_PRIVATE);

        //use""as default value
        downloadDate = settings.getString("lastDownloadDate","");
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '"+downloadDate+"'");
        //get geojson file
        downloadjson();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop(){
        super.onStop();
        mapView.onStop();
        Log.d(tag,"[onStop] Storing lastDownloadDate of "+downloadDate);
        SharedPreferences settings = getSharedPreferences(preferencesFile,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("lastDownloadDate",downloadDate);
        editor.apply();


        //storing json file of current day
        Log.d(tag,"[onStop] Storing json file"+json);
        SharedPreferences settings2 = getSharedPreferences(preferencesFile,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor2 = settings.edit();
        editor2.putString("json",json);
        editor2.apply();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    void downloadComplete(String result){
        json = result;
        Log.d(tag,"[downloadComplete] Storing json file"+json);

    }







    @SuppressLint("StaticFieldLeak")
    public class DownloadFileTask extends AsyncTask<String, Void, String> {

        DownloadCompleteRunner downloadCompleteRunner = new DownloadCompleteRunner();

        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadFileFromNetwork(urls[0]);
            } catch (IOException e) {
                return "Unable to load data. Please check your network connection";
            }
        }

        private String loadFileFromNetwork(String urlString) throws IOException {
            return readStream(downloadUrl(new URL(urlString)));
        }

        private InputStream downloadUrl(URL url) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            return conn.getInputStream();
        }

        @NonNull
        private String readStream(InputStream stream) throws IOException {
            Scanner s = new Scanner(stream).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            DownloadCompleteRunner.downloadComplete(result);
        }



    }
}

