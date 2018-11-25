package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {
    private final String tag = "WalletActivity";


    private DatabaseReference walletRef;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase database;
    private List<Coin> coinList;
    //private ListView listView;

    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        //listView = (ListView) findViewById(R.id.coin_list_view);
        walletRef = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        walletRef = database.getReference("users").child(currentUser.getUid());
        coinList = new ArrayList<>();
        //getCoinInformation();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new WalletFragment()).commit();



        Log.d(tag,"[coinList size1]:" + coinList.size());
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment selectedFragment = null;

                    switch (menuItem.getItemId()){
                        case R.id.nav_wallet:
                            selectedFragment = new WalletFragment();
                            break;
                        case R.id.nav_mail:
                            selectedFragment = new TransferFragment();
                            break;
                        case R.id.nav_bank:
                            selectedFragment = new BankFragment();
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                    return true;
                }
            };

    /*private void getCoinInformation() {

        walletRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (dataSnapshot.getValue() != null){
                    Log.d(tag, "[getValue]" +dataSnapshot.getValue().toString());
                    Log.d(tag, "[getChildren]" +dataSnapshot.getChildren().toString());
                    Log.d(tag, "[getChildrenClass]" +dataSnapshot.getChildren().getClass().toString());
                }
                coinList.clear();
                for(DataSnapshot coinsSnapshot: dataSnapshot.getChildren()){
                    coinList.clear();
                    Map<String,Map<String,Object>> map = (Map<String, Map<String, Object>>) coinsSnapshot.getValue();
                    for(Map<String,Object> submap: map.values())
                    {
                        String currency = (String) submap.get("currency");
                        String id = (String) submap.get("id");
                        Double  value = (Double) submap.get("value");

                        Coin coin = new Coin(id,value,currency);
                        coinList.add(coin);
                    }
                    Log.d(tag,"[coinList value]:"+coinList.toString());

                }

                Log.d(tag, "[Realtime Database] Wallet updated" );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(tag, "Failed to read value.", error.toException());
            }
        });
    }*/

    /*@Override
    protected void onStart() {
        super.onStart();
        walletRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                coinList.clear();
                for(DataSnapshot coinsSnapshot: dataSnapshot.getChildren()){
                    coinList.clear();
                    Map<String,Map<String,Object>> map = (Map<String, Map<String, Object>>) coinsSnapshot.getValue();
                    for(Map<String,Object> submap: map.values())
                    {
                        String currency = (String) submap.get("currency");
                        String id = (String) submap.get("id");
                        Double  value = (Double) submap.get("value");

                        Coin coin = new Coin(id,value,currency);
                        coinList.add(coin);
                    }
                    Log.d(tag,"[coinList size2]:" + coinList.size());

                }
                //CoinsAdapter coinsAdapter = new CoinsAdapter(WalletActivity.this,coinList);
                //listView.setAdapter(coinsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Log.d(tag,"[coinList size3]:" + coinList.size());
    }*/


}
