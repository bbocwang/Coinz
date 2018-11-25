package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.storage.Resource;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransferFragment extends Fragment implements AdapterView.OnItemSelectedListener{
    Spinner coinSpinner;
    private final String tag = "TransferFragment";
    private ListView listView;
    private DatabaseReference walletRef;
    private List<Coin> coinList;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase database;
    private String selectedId;
    EditText receiver;
    EditText transfernote;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfer, container, false);
        coinSpinner = view.findViewById(R.id.spinner_transfer);
        coinSpinner.setOnItemSelectedListener(this);

        receiver = (EditText) view.findViewById(R.id.receiverEmail);
        transfernote = (EditText) view.findViewById(R.id.transferNote);


        walletRef = FirebaseDatabase.getInstance().getReference();
        database = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        walletRef = database.getReference("users").child(currentUser.getUid());
        coinList = new ArrayList<>();
        selectedId = null;


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
                List<StringWithTag> coinInfo = new ArrayList<StringWithTag>();
                if(coinList != null){
                    for(Coin c:coinList){
                        Double value = (Double) c.getValue();
                        String currency = (String) c.getCurrency();
                        DecimalFormat df = new DecimalFormat("#.##");
                        value = Double.valueOf(df.format(value));
                        String value_string = String.valueOf(value);
                        String info = (String) value_string +" "+ currency;
                        //Using the StringWithTag class to store the ID of the coin
                        coinInfo.add(new StringWithTag(info,c.getId()));
                    }
                }

                if(getActivity() != null){
                    ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(),
                            android.R.layout.simple_list_item_1, coinInfo);
                    coinSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return view;






    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        StringWithTag s = (StringWithTag) parent.getItemAtPosition(position);
        selectedId = s.id;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //The inner class StringWithTag for storing the id with the content
    public class StringWithTag {
        public String string;
        public String id;

        public StringWithTag(String stringPart, String tagPart) {
            string = stringPart;
            id = tagPart;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
