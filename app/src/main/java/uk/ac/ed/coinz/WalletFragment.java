package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

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

public class WalletFragment extends Fragment {
    private final String tag = "WalletFragment";
    private ListView listView;
    private List<Coin> coinList;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_wallet, null);
        DatabaseReference walletRef = FirebaseDatabase.getInstance().getReference();
        listView = (ListView) view.findViewById(R.id.wallet_listView);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            walletRef = database.getReference("users").child(currentUser.getUid());
        }
        coinList = new ArrayList<>();


        walletRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("LogNotTimber")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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

                            Coin coin = null;
                            if (currentUser != null) {
                                coin = new Coin(id,value,currency, currentUser.getUid());
                            }
                            coinList.add(coin);
                        }
                    }
                    Log.d(tag,"[coinList size2]:" + coinList.size());

                }
                if(getActivity() != null){
                    CoinsAdapter coinsAdapter = new CoinsAdapter(getActivity(),coinList);
                    listView.setAdapter(coinsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return view;
    }
}
