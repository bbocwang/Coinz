package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class GameActivity extends AppCompatActivity implements OnMapReadyCallback,PermissionsListener{
    private final String tag = "GameActivity";

    Calendar calendar = Calendar.getInstance();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    private String currentDate = dateFormat.format(calendar.getTime());
    private String downloadDate = "";//Format:YYYY/MM/DD
    private final String preferencesFile = "LastDownloadDate";//for storing preferences
    private final String geojsonFile = "json";//for storing preferences
    public String json = "";

    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    @SuppressWarnings("deprecation")
    private Location originLocation;

    private FeatureCollection featureCollection;
    private List<Feature> features;
    private Marker coinMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_game);
        mapView = (MapView)findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        if(mapboxMap == null){
            Log.d(tag,"[onMapReady] mapBox is null");
        }
        else{
            this.mapboxMap = mapboxMap;
            mapboxMap.getUiSettings().setCompassEnabled(true);
            mapboxMap.getUiSettings().setZoomControlsEnabled(true);
            enableLocationComponent();
            createMarkers(features);
        }
    }


    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(){
        if (permissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag,"Permissions are granted");
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            originLocation = locationComponent.getLastKnownLocation();
        }
        else{
            Log.d(tag,"Permissions are not granted");
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    private void setCameraPosition(Location location){
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()),13.0));
    }



    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //present toast or dialog why they need to grant location permission
        Log.d(tag,"permissions: "+permissionsToExplain.toString());
        Toast.makeText(this, R.string.user_location_permission_explanation,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocationComponent();
        }else {
            Toast.makeText(this, R.string.user_location_permission_not_granted,
                    Toast.LENGTH_LONG).show();
            finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
    @SuppressWarnings("MissingPermission")
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
        featureCollection = featureCollection.fromJson(json);
        features = featureCollection.features();
    }

    public void createMarkers(List<Feature> features) {
        if(features == null){
            Log.d(tag,"[onAddingMarkers] features = null");
        }
        int i=0;
        for (Feature feature: features){
            Geometry geometry = feature.geometry();
            Point p = (Point) geometry;
            List<Double> coordinates = p.coordinates();
            String currency = feature.properties().get("currency").toString();
            String id = feature.properties().get("id").toString();
            String value = feature.properties().get("value").toString();
            String symbol = feature.properties().get("marker-symbol").toString();
            String color = feature.properties().get("marker-color").toString();
            //MarkerOptions marker =  new MarkerOptions().title(id).snippet(value)
            //        .position(new LatLng(coordinates.get(0),coordinates.get(1)));
            if(mapboxMap == null){
                Log.d(tag,"[onAddingMarkers] mapboxMap = null");
            }
            Log.d(tag,"iteration:"+i);
            i++;
            coinMarker = mapboxMap.addMarker(new MarkerOptions().title(id).snippet(value)
                    .position(new LatLng(coordinates.get(1),coordinates.get(0))));
        }
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

