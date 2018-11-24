package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

import timber.log.Timber;

public class WalletActivity extends AppCompatActivity {
    private final String tag = "WalletActivity";

    private DatabaseReference walletRef;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase database;
    private List<Coin> coinList;

    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        walletRef = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        walletRef = database.getReference("users").child(currentUser.getUid());
        coinList = new ArrayList<>();
        getCoinInformation();


        ListView listViewCoins = (ListView) findViewById(R.id.listViewCoins);
        CustomAdapter customAdapter = new CustomAdapter();
        listViewCoins.setAdapter(customAdapter);
        Log.d(tag,"[coinList size1]:" + coinList.size());
    }

    private void getCoinInformation() {
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

    @Override
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        Log.d(tag,"[coinList size3]:" + coinList.size());
    }

    class CustomAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return coinList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.customlayout,null);
            ImageView imageView = (ImageView)convertView.findViewById(R.id.imageViewCoinicon);
            TextView textView_currency = (TextView) convertView.findViewById(R.id.textView_currency);
            TextView textView_value = (TextView) convertView.findViewById(R.id.textView_value);
            imageView.setImageResource(R.drawable.pound_icon);
            textView_currency.setText(coinList.get(position).getCurrency());
            textView_value.setText(coinList.get(position).getCurrency());
            return convertView;
        }
    }
}
