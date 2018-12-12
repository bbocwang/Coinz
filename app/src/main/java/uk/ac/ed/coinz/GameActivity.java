package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/*This activity is the main game activity
*
*it contains a map view and two bottons,User can find themself on the map, and
* the map will show the coins around user, different currency has different icoins
* when they walk near a coin, if there's some coins is within 25 m of user, the coin
* will be collected by the app, and will be send to the wallet. User can also collect
* the coin by themselfe by clicking the coin icon at the right bottom of the map.
*
* if the user click the wallet icon, it will open the wallet activity.
*
* Acknowledgement:
* https://course.inf.ed.ac.uk/ilp/
* https://www.youtube.com/watch?v=cIQdB7Lz6tY/
* https://www.mapbox.com/help/tutorials/
* https://www.mapbox.com/help/markers/
* https://www.mapbox.com/help/android-navigation-sdk/
* https://www.mapbox.com/help/first-steps-android-sdk/
* */

@SuppressWarnings("ALL")
@SuppressLint("LogNotTimber")
public class GameActivity extends AppCompatActivity implements OnMapReadyCallback,PermissionsListener,LocationEngineListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private final String tag = "GameActivity";

    //Geojson related stuff
    Calendar calendar = Calendar.getInstance();
    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    private String currentDate = dateFormat.format(calendar.getTime());// get the current date
    private String downloadDate = "";// date format:YYYY/MM/DD
    private final String preferencesFile = "LastDownloadDate";//for storing preferences
    public String json = "";

    //location related stuff
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    @SuppressWarnings("deprecation")
    private Location originLocation;//store the origin location
    private LocationEngine locationEngine;

    //marker related stuff
    private FeatureCollection featureCollection;
    private List<Feature> features;
    private Marker coinMarker;
    private ArrayList<Marker> markers;

    //Database related stuff
    private FirebaseDatabase database;
    FirebaseFirestore db;
    private DatabaseReference userAccountRef;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private HashMap coinsInWallet;
    public List<Coin> coinList;//current coins on the map
    private List<User> userList;
    DocumentReference historyRef;

    //Layout related stuff
    Switch aSwitch;
    private boolean auto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_game);
        initializeLayout(savedInstanceState);
        downloadjson();
        connectDatabase();//connect to the database
        updateUserinfo();//fetch the user information
    }

    private void initializeLayout(Bundle savedInstanceState) {
        auto = true;
        coinList = new ArrayList<>();
        userList = new ArrayList<>();
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        aSwitch = findViewById(R.id.switch1);
        aSwitch.setChecked(true);
        aSwitch.setOnCheckedChangeListener(this);
        findViewById(R.id.collectButton).setOnClickListener(this);
        findViewById(R.id.wallet).setOnClickListener(this);
    }

    //this method is called when the app starts, to get the user's wallet information
    //stored in the firebase database
    @SuppressLint("LogNotTimber")
    private void updateUserinfo() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference userinfoRef = database.getReference("userinfo").child("user");
        if(currentUser != null){
            Log.d(tag,"[OnconnectDatabase]current user:"+ currentUser.getEmail());
        }else{
            Log.d(tag,"[OnconnectDatabase]current user is null!");
        }
        Log.d(tag,"[OnconnectDatabase]Conected to the database!"+ currentUser.getEmail());
        userinfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){

                    Log.d(tag,"[Getting userinfo]"+userSnapshot.toString());
                    User user = userSnapshot.getValue(User.class);
                    userList.add(user);
                    if (user != null) {
                        Log.d(tag,"[Getting userinfo]"+user.getEmail());
                    }
                }
                if(userList != null){
                    if(userList.contains(new User(currentUser.getEmail(),currentUser.getUid()))){

                    }else{
                        //add current user to the userinfo database
                        User user = new User(currentUser.getEmail(),currentUser.getUid());
                        userinfoRef.child(currentUser.getUid()).setValue(user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //there are two buttons: collect coin and go to wallet
    public void onClick(View view){
        switch (view.getId()){
            case R.id.collectButton:
                //When click the collect button, collect the coin
                collect();
                if(auto){
                    Toast.makeText(this,"No coin around you, try to walk around",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.wallet:
                //When click the wallet button, go to wallet
                startActivity(new Intent(this,WalletActivity.class));
        }
    }

    //connect to the database, fetch the coin information from firebase
    private void connectDatabase() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        db = FirebaseFirestore.getInstance();
        historyRef = db.collection("CoinzHistory").document(currentUser.getUid());
        userAccountRef = database.getReference("users").child(currentUser.getUid());

        if(currentUser != null){
            Log.d(tag,"[OnconnectDatabase]current user:"+ currentUser.getEmail());
        }else{
            Log.d(tag,"[OnconnectDatabase]current user is null!");
        }
        if(userAccountRef != null){
            Log.d(tag,"[OnconnectDatabase]Conected to the database!"+ currentUser.getEmail());
        }else{
            Log.d(tag,"[OnDataChange] database ref is null");
        }
        userAccountRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                coinList.clear();
                for(DataSnapshot coinsSnapshot: dataSnapshot.getChildren()){
                    coinList.clear();
                    Map<String,Map<String,Object>> map = (Map<String, Map<String, Object>>) coinsSnapshot.getValue();
                    if (map != null) {
                        for(Map<String,Object> submap: map.values())
                        {
                            String currency = (String) submap.get("currency");
                            String id = (String) submap.get("id");
                            Double  value = (Double) submap.get("value");
                            Coin coin = new Coin(id,value,currency,currentUser.getUid());
                            if(!coinList.contains(coin)){
                                coinList.add(coin);
                            }
                        }
                    }
                }

                /*if(mapboxMap != null){
                    mapboxMap.clear();
                    createMarkers(features);
                    Log.d(tag, "[Realtime Database] Wallet updated" );
                }*/
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w(tag, "[OnDataChange]Failed to read value.", error.toException());
            }
        });

       /* database = FirebaseDatabase.getInstance();
        DatabaseReference historyRef = database.getReference("history").child(currentUser.getUid()).child(downloadDate);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                CoinList historyList = dataSnapshot.getValue(CoinList.class);
                if(historyList != null){
                    for(Coin coin: historyList.getCoinList()){
                        if(!coinList.contains(coin)){
                            coinList.add(coin);
                        }
                    }
                }
                Log.d(tag,"[Coinlist]"+coinList.size());
                if(mapboxMap != null){
                    mapboxMap.clear();
                    createMarkers(features);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/


        this.historyRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                CoinList historyList = documentSnapshot.toObject(CoinList.class);
                if(historyList != null){
                    Log.d(tag,"[Cloud firestore connected]"+historyList.toString());
                    for(Coin coin: historyList.getCoinList()){
                        if(!coinList.contains(coin)){
                            coinList.add(coin);
                        }
                    }
                    Log.d(tag,"[Coinlist]"+coinList.size());
                }
                if(mapboxMap != null){
                    mapboxMap.clear();
                    createMarkers(features);
                }
            }
        });


    }

    //This method is called when user click the coin button
    //this method will detect the coin around the user, if there are some coins,
    //it will collect the coin to the user'wallet stored on the firebase database
    private void collect() {
        Log.d(tag,"[size of features]:"+features.size());
        boolean result = false;
        Feature foundfeature = null;
        List<Marker> chosedMarker = new ArrayList<>();

        if(markers != null){
            //scan the coin list to see if there's any coin around user
            for (Marker marker : markers) {


                String markerid = marker.getSnippet();
                Location markerLoc = new Location("");
                if (marker != null) {
                    markerLoc.setLatitude(marker.getPosition().getLatitude());
                    markerLoc.setLongitude(marker.getPosition().getLongitude());
                }

                String currency = new String();
                String value = new String();

                //check the distance between the user and the coin
                float distanceInMeters = originLocation.distanceTo(markerLoc);
                if(distanceInMeters <= 25){
                    Boolean repetition = false;
                    for(Feature feature: features){
                        //get the id of the coin
                        String id = Objects.requireNonNull(feature.properties()).get("id").toString();
                        id = id.substring(1, id.length() - 1);

                        if(id.equals(markerid)){
                            //get the currency of the coin
                            currency = Objects.requireNonNull(feature.properties()).get("currency").toString();
                            currency = currency.substring(1, currency.length() - 1);

                            //get the value of the coin
                            value = Objects.requireNonNull(feature.properties()).get("value").toString();
                            value = value.substring(1, value.length() - 1);
                            foundfeature = feature;
                            mapboxMap.removeMarker(marker);
                            chosedMarker.add(marker);
                        }

                    }
                    for(Coin c:coinList){
                        if(c.getId().equals(markerid)){
                            repetition = true;
                            Toast.makeText(this,"You have already collected this " +
                                    "coin!",Toast.LENGTH_LONG).show();
                        }
                    }

                    //if this is a new coin
                    if(!repetition)
                    {
                        Log.d(tag,"[get coin!]"+currency+value+"current location"+originLocation.toString());
                        Toast.makeText(this,"You Got some Coin!",Toast.LENGTH_SHORT).show();
                        Coin coin = new Coin(markerid,value,currency,currentUser.getUid());
                        userAccountRef.child("wallet").child(markerid).setValue(coin);
                        result = true;
                    }
                }
            }
            if(chosedMarker != null){
                for (Marker marker:chosedMarker){
                    markers.remove(marker);
                }
            }
        }


        if(foundfeature != null) {
            //after the collect, recreate the markers to remove the collected coin
            features.remove(foundfeature);
        }
        //if there's no coins around user, show toast
        if(!result){
            Log.d(tag,"No coins around you");
            if(!auto){
                Toast.makeText(this,"No coins around you! try to walk around",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //when the map ready
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
            //createMarkers(features);//create the marker of coins on the map
            enableLocation();//using the location servise to get the location

        }
    }

    private void enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
        }
        else {
            Log.d(tag,"Permissions are not granted");
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    //initialize the location engine service to get the location
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


    //change the camera position, when location changed this will function will be called
    private void setCameraPosition(Location location){
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude()
                ,location.getLongitude()),15.0));
    }

    //enable location component to display the current position
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(){
        if (PermissionsManager.areLocationPermissionsGranted(this)){
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

    //get the permission result
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

    //this function will be called to downloadjson
    private void downloadjson() {
        DownloadFileTask downloadFileTask = new DownloadFileTask();
        //the website pattern:
        //http://homepages.inf.ed.ac.uk/stg/coinz/YYYY/MM/DD/coinzmap.geojson
        if(!Objects.equals(currentDate, downloadDate)) {
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
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

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

        connectDatabase();
        //restore preferences
        SharedPreferences settings = getSharedPreferences(preferencesFile,
                Context.MODE_PRIVATE);

        //use""as default value
        downloadDate = settings.getString("lastDownloadDate","");
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '"+downloadDate+"'");
        //get geojson file


        //downloadjson();
        featureCollection = FeatureCollection.fromJson(json);
        if(featureCollection != null){
            Log.d(tag,"[OnCreateFeatureCollection]featurecollection != null");
            features = featureCollection.features();
        }else{
            Log.d(tag,"[OnCreateFeatureCollection]featurecollection == null"+json);
            if(json == null){
                Log.d(tag,"[OnCreateFeatureCollection] json == null");
            }else{

                Log.d(tag,"[OnCreateFeatureCollection] json != null");
            }
        }

        if(locationEngine != null){
            try{
                locationEngine.requestLocationUpdates();
            }catch (SecurityException ignored){}
            locationEngine.addLocationEngineListener(this);
        }

    }

    //create the markers of the coins on the map
    @SuppressLint("LogNotTimber")
    public void createMarkers(List<Feature> features) {
        if(features == null){
            Log.d(tag,"[onAddingMarkers] features = null");
        }
        List<String> coinIdlist = new ArrayList<>();
        Log.d(tag,"[onCreateMarkers] Creating Markers");
        markers = new ArrayList<>();
        List<Feature> alreadyHaveList = new ArrayList<>();
        coinIdlist.clear();
        for(Coin c:coinList){
            coinIdlist.add(c.getId());
        }
        Log.d(tag,"[coinList before creating marker]:"+coinIdlist.toString());
        if (features != null) {
            for (Feature feature: features){
                Geometry geometry = feature.geometry();
                Point p = (Point) geometry;
                List<Double> coordinates = null;
                if (p != null) {
                    coordinates = p.coordinates();
                }

                //get currency of the current coin
                String currency = Objects.requireNonNull(feature.properties()).get("currency").toString();
                currency = currency.substring(1,currency.length()-1);

                //get id of the current coin
                String id = Objects.requireNonNull(feature.properties()).get("id").toString();
                id = id.substring(1,id.length()-1);

                //get value of the current coin
                String value = Objects.requireNonNull(feature.properties()).get("value").toString();
                value = value.substring(1,5);

                //avoid the null pointer exception
                if(mapboxMap == null){
                    Log.d(tag,"[onAddingMarkers] mapboxMap = null");
                }

                if(coinIdlist.contains(id)){
                    alreadyHaveList.add(feature);
                    Log.d(tag,"[onAddingMarkers] repetition marker, id="+id);
                }else {
                    //create the different markers
                    IconFactory iconFactroy = IconFactory.getInstance(this);

                    // create the marker using different icoins depends on the currency
                    switch (currency) {
                        case "QUID":
                            if (coordinates != null) {
                                Icon blue_icon = iconFactroy
                                        .fromResource(R.drawable.blue_marker);

                                coinMarker = mapboxMap.addMarker(new MarkerOptions()
                                        .title(value+currency).snippet(id)
                                        .position(new LatLng(coordinates.get(1)
                                                , coordinates.get(0))).icon(blue_icon));
                            }
                            markers.add(coinMarker);
                        case "DOLR":
                            if (coordinates != null) {
                                Icon gree_icon = iconFactroy
                                        .fromResource(R.drawable.green_marker);

                                coinMarker = mapboxMap.addMarker(new MarkerOptions()
                                        .title(value+currency).snippet(id)
                                        .position(new LatLng(coordinates.get(1)
                                                , coordinates.get(0))).icon(gree_icon));
                            }
                            markers.add(coinMarker);
                        case "SHIL":
                            if (coordinates != null) {
                                Icon purple_icon = iconFactroy
                                        .fromResource(R.drawable.purple_marker);

                                coinMarker = mapboxMap.addMarker(new MarkerOptions()
                                        .title(value+currency).snippet(id)
                                        .position(new LatLng(coordinates.get(1)
                                                , coordinates.get(0))).icon(purple_icon));
                            }
                            markers.add(coinMarker);
                        case "PENY":
                            if (coordinates != null) {
                                Icon yellow_icon = iconFactroy
                                        .fromResource(R.drawable.yellow_marker);

                                coinMarker = mapboxMap.addMarker(new MarkerOptions()
                                        .title(value+currency).snippet(id)
                                        .position(new LatLng(coordinates.get(1)
                                                , coordinates.get(0))).icon(yellow_icon));
                            }
                            markers.add(coinMarker);
                    }
                }

            }
        }
        //remove the collected coins from the feature list
        for(Feature feature:alreadyHaveList){
            if (features != null) {
                features.remove(feature);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        /*if(mapboxMap != null){
            mapboxMap.clear();
            createMarkers(features);
        }*/
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
        Log.d(tag,"[onStop] Storing json file");
        SharedPreferences.Editor editor2 = settings.edit();
        editor2.putString("json",json);
        editor2.apply();

        database = FirebaseDatabase.getInstance();
        DatabaseReference historyRef = database.getReference("history").child(currentUser.getUid()).child(downloadDate);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                historyRef.setValue(new CoinList(coinList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        this.historyRef.set(new CoinList(coinList)).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(tag, "DocumentSnapshot successfully written!");


            }
        });



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
        //deactivate the location engine
        if(locationEngine != null){
            locationEngine.deactivate();
        }
    }

    void downloadComplete(String result){
        json = result;
        Log.d(tag,"[Getting json] Storing json file");
    }



    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {}

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

            //if current is auto mode, collect the coin every time when location changed
            if(auto){
                collect();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        auto = isChecked;
        if(auto){
            Toast.makeText(this,"Auto mode: the coin will be collected automatically"
                    ,Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"Auto collect disabeled",0).show();
        }
    }

    //inner class extends asyncTask, to download the json file
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
        private String readStream(InputStream stream) {
            Scanner s = new Scanner(stream).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            DownloadCompleteRunner.downloadComplete(result);
        }



    }
}
