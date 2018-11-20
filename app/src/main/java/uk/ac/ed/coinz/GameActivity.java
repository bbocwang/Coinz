package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class GameActivity extends AppCompatActivity implements OnMapReadyCallback,PermissionsListener,LocationEngineListener, View.OnClickListener {
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
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;

    private FeatureCollection featureCollection;
    private List<Feature> features;
    private Marker coinMarker;
    private ArrayList<Marker> markers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_game);
        mapView = (MapView)findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        findViewById(R.id.collectButton).setOnClickListener(this);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.collectButton:
                collect();
                break;
        }
    }

    private void collect() {
        for (Feature feature : features) {
            Geometry geometry = feature.geometry();
            Point p = (Point) geometry;
            List<Double> coordinates = p.coordinates();
            String currency = feature.properties().get("currency").toString();
            currency = currency.substring(1, currency.length() - 1);

            String id = feature.properties().get("id").toString();
            id = id.substring(1, id.length() - 1);

            String value = feature.properties().get("value").toString();
            value = value.substring(1, value.length() - 1);

            String symbol = feature.properties().get("marker-symbol").toString();
            symbol = symbol.substring(1, symbol.length() - 1);

            String color = feature.properties().get("marker-color").toString();
            color = color.substring(1, currency.length() - 1);



            Location markerLoc = new Location("");
            markerLoc.setLatitude(coordinates.get(1));
            markerLoc.setLongitude(coordinates.get(0));

            float distanceInMeters = originLocation.distanceTo(markerLoc);
            if(distanceInMeters <= 20){
                Log.d(tag,"get coin"+currency+value+"current location"+originLocation.toString());
            }else{
                Log.d(tag,"no coins in 20m"+"current location"+originLocation.toString());
            }
        }
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
            enableLocation();

        }
    }

    private void enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            //initializeLacationLayer();
        }
        else {
            Log.d(tag,"Permissions are not granted");
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void initializeLocationEngine(){
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        @SuppressLint("MissingPermission")
        Location lastLocation = locationEngine.getLastLocation();
        if(lastLocation != null){
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        }else{
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void initializeLacationLayer() {
        locationLayerPlugin = new LocationLayerPlugin(mapView,mapboxMap,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS);
        Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(locationLayerPlugin);
    }

    private void setCameraPosition(Location location){
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude()
                ,location.getLongitude()),13.0));
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

        if(locationEngine != null){
            try{
                locationEngine.requestLocationUpdates();
            }catch (SecurityException ignored){}
            locationEngine.addLocationEngineListener(this);
        }

    }

    @SuppressLint("LogNotTimber")
    public void createMarkers(List<Feature> features) {
        if(features == null){
            Log.d(tag,"[onAddingMarkers] features = null");
        }
        int i=0;
        markers = new ArrayList<Marker>();
        for (Feature feature: features){
            Geometry geometry = feature.geometry();
            Point p = (Point) geometry;
            List<Double> coordinates = p.coordinates();
            String currency = feature.properties().get("currency").toString();
            currency = currency.substring(1,currency.length()-1);

            String id = feature.properties().get("id").toString();
            id = id.substring(1,id.length()-1);

            String value = feature.properties().get("value").toString();
            value = value.substring(1,value.length()-1);

            String symbol = feature.properties().get("marker-symbol").toString();
            symbol = symbol.substring(1,symbol.length()-1);

            String color = feature.properties().get("marker-color").toString();
            color = color.substring(1,currency.length()-1);
            //MarkerOptions marker =  new MarkerOptions().title(id).snippet(value)
            //        .position(new LatLng(coordinates.get(0),coordinates.get(1)));
            if(mapboxMap == null){
                Log.d(tag,"[onAddingMarkers] mapboxMap = null");
            }
            Log.d(tag,"iteration:"+i);
            i++;
            IconFactory iconFactroy = IconFactory.getInstance(this);
            Icon blue_icon = iconFactroy.fromResource(R.drawable.blue_marker);
            Icon gree_icon = iconFactroy.fromResource(R.drawable.green_marker);
            Icon purple_icon = iconFactroy.fromResource(R.drawable.purple_marker);
            Icon yellow_icon = iconFactroy.fromResource(R.drawable.yellow_marker);
            switch (currency){
                case "QUID": coinMarker = mapboxMap.addMarker(new MarkerOptions().title(currency).snippet("value:"+value)
                        .position(new LatLng(coordinates.get(1),coordinates.get(0))).icon(blue_icon));
                    markers.add(coinMarker);
                case "DOLR": coinMarker = mapboxMap.addMarker(new MarkerOptions().title(currency).snippet("value:"+value)
                        .position(new LatLng(coordinates.get(1),coordinates.get(0))).icon(gree_icon));
                    markers.add(coinMarker);
                case "SHIL": coinMarker = mapboxMap.addMarker(new MarkerOptions().title(currency).snippet("value:"+value)
                        .position(new LatLng(coordinates.get(1),coordinates.get(0))).icon(purple_icon));
                    markers.add(coinMarker);
                case "PENY": coinMarker = mapboxMap.addMarker(new MarkerOptions().title(currency).snippet("value:"+value)
                        .position(new LatLng(coordinates.get(1),coordinates.get(0))).icon(yellow_icon));
                    markers.add(coinMarker);
            }
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

        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
            locationEngine.removeLocationEngineListener(this);

        }
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
        if(locationEngine != null){
            locationEngine.deactivate();
        }
    }

    void downloadComplete(String result){
        json = result;
        Log.d(tag,"[downloadComplete] Storing json file"+json);
    }



    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            originLocation = location;
            setCameraPosition(location);
        }
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

