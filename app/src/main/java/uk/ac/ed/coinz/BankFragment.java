package uk.ac.ed.coinz;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    Spinner coinSpinner;
    private final String tag = "TransferFragment";
    private ListView listView;
    private DatabaseReference walletRef;
    private List<Coin> coinList;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase database;
    private String selectedId;
    private String selectedCurrency;
    private Double selectedValue;
    private String receiverEmail;
    EditText receiver;
    private List<User> userList;
    private List<BankAccount> bankList;
    private String downloadDate;
    private final String preferencesFile = "LastDownloadDate";//for storing preferences
    public String json = "";
    private FeatureCollection featureCollection;
    private List<Feature> features;
    private Map<String, Double> rates;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bank, container, false);
        view.findViewById(R.id.sendBankButton).setOnClickListener(this);
        listView = (ListView) view.findViewById(R.id.bankList);
        coinSpinner = view.findViewById(R.id.bankCoinspinner);
        coinSpinner.setOnItemSelectedListener(this);
        selectedId = null;
        selectedValue = null;
        selectedCurrency = null;
        bankList = new ArrayList<>();
        rates = new HashMap<>();
        updateBankinfo();
        updateChangeRate();


        database = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        walletRef = database.getReference("users").child(currentUser.getUid());
        coinList = new ArrayList<>();
        selectedId = null;
        selectedValue = null;
        selectedCurrency = null;
        receiverEmail = null;
        userList = new ArrayList<>();


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
                List<BankFragment.StringWithTag> coinInfo = new ArrayList<BankFragment.StringWithTag>();
                if(coinList != null){
                    for(Coin c:coinList){
                        Double value = (Double) c.getValue();
                        String currency = (String) c.getCurrency();
                        DecimalFormat df = new DecimalFormat("#.##");
                        value = Double.valueOf(df.format(value));
                        String value_string = String.valueOf(value);
                        String info = (String) value_string +" "+ currency;
                        //Using the StringWithTag class to store the ID of the coin
                        coinInfo.add(new BankFragment.StringWithTag(info,c.getId(),currency,c.getValue()));
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

    private void updateChangeRate() {
        SharedPreferences settings = getContext().getSharedPreferences(preferencesFile,
                Context.MODE_PRIVATE);

        downloadDate = settings.getString("lastDownloadDate","");
        Log.d(tag, "[OnUpdate Change rate] Recalled lastDownloadDate is '"+downloadDate+"'");
        String jsonSource = settings.getString("json","");
        try {
            JSONObject j = new JSONObject(jsonSource);
            Log.d(tag,"[Getting Change rate]"+j.getJSONObject("rates").toString());
            rates.put("SHIL", Double.valueOf(j.getJSONObject("rates").getString("SHIL")));
            rates.put("DOLR", Double.valueOf(j.getJSONObject("rates").getString("DOLR")));
            rates.put("QUID", Double.valueOf(j.getJSONObject("rates").getString("QUID")));
            rates.put("PENY", Double.valueOf(j.getJSONObject("rates").getString("PENY")));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void updateBankinfo() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference userinfoRef = database.getReference("bank");
        if(currentUser != null){
            Log.d(tag,"[OnconnectDatabase]current user:"+currentUser.getEmail().toString());
        }else{
            Log.d(tag,"[OnconnectDatabase]current user is null!");
        }
        if(userinfoRef != null){
            Log.d(tag,"[OnconnectDatabase]Conected to the database!"+currentUser.getEmail().toString());
        }else{
            Log.d(tag,"[OnDataChange] database ref is null");
        }
        userinfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(bankList != null){
                    bankList.clear();
                }
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    Log.d(tag,"[Getting bankinfo] bankList size:"+userSnapshot.toString());
                    BankAccount bankAccount = userSnapshot.getValue(BankAccount.class);
                    bankList.add(bankAccount);
                }
                Log.d(tag,"[Getting bankinfo] bankList size:"+bankList.size());

                List<String> bankinfo = new ArrayList<>();
                for(BankAccount b:bankList){
                    DecimalFormat df = new DecimalFormat("#.##");
                    Double value = b.getGold();
                    value = Double.valueOf(df.format(value));
                    bankinfo.add("   "+b.getEmail()+":  "+value+"  gold");
                }

                if(getActivity() != null){
                    ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(),
                            android.R.layout.simple_list_item_1, bankinfo);
                    listView.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        BankFragment.StringWithTag s = (BankFragment.StringWithTag) parent.getItemAtPosition(position);
        selectedId = s.id;
        selectedValue = s.value;
        selectedCurrency = s.currency;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference bankRef = database.getReference("bank").child(currentUser.getUid());
        bankRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                BankAccount bankAccount = dataSnapshot.getValue(BankAccount.class);
                Log.d(tag,"[Current Datashot]:"+dataSnapshot.toString());
                Double gold = 0.0;
                gold = selectedValue * rates.get(selectedCurrency);
                if(selectedId != null){
                    if(bankAccount == null){
                        BankAccount b = new BankAccount(gold,currentUser.getUid(),currentUser.getEmail());
                        bankRef.setValue(b);
                        deleteCoin();
                    }else{
                        BankAccount b = new BankAccount(bankAccount.getGold()+gold,currentUser.getUid(),currentUser.getEmail());
                        bankRef.setValue(b);
                        deleteCoin();
                    }
                }else {
                    Toast.makeText(getActivity(),"please select a coin",Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }






    private void deleteCoin() {
        database = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        walletRef = database.getReference("users").child(currentUser.getUid());
        walletRef.child("wallet").child(selectedId).removeValue();
    }

    //The inner class StringWithTag for storing the id with the content
    public class StringWithTag {
        public String string;
        public String id;
        public String currency;
        public Double value;

        public StringWithTag(String stringPart, String tagPart,String currency,Double value) {
            string = stringPart;
            id = tagPart;
            this.currency = currency;
            this.value = value;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
