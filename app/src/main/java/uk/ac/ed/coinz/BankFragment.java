package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private List<BankAccount> bankList;
    private String downloadDate;
    private Integer remaining;
    TextView textViewRemainingCoin;
    private String ownerId;

    private Map<String, Double> rates;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bank, container, false);
        view.findViewById(R.id.sendBankButton).setOnClickListener(this);
        textViewRemainingCoin = view.findViewById(R.id.remainingCoin);
        listView = view.findViewById(R.id.bankList);
        coinSpinner = view.findViewById(R.id.bankCoinspinner);
        coinSpinner.setOnItemSelectedListener(this);
        selectedId = null;
        selectedValue = null;
        ownerId = null;
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


        walletRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("LogNotTimber")
            @Override
            //Updating the coinList, to create the spinner of the coin list to chose
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(tag,"[On Update Coinlist] Updating the coinList");
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
                            String onwer = (String) submap.get("firstOwnerId");

                            Coin coin = new Coin(id,value,currency,onwer);
                            coinList.add(coin);
                        }
                    }
                    Log.d(tag,"[coinList size]:" + coinList.size());

                }
                List<BankFragment.StringWithTag> coinInfo = new ArrayList<>();
                if(coinList != null){
                    for(Coin c:coinList){
                        Double value = c.getValue();
                        String currency = c.getCurrency();
                        DecimalFormat df = new DecimalFormat("#.##");
                        value = Double.valueOf(df.format(value));
                        String value_string = String.valueOf(value);
                        String info = value_string +" "+ currency;
                        String owner = c.getFirstOwnerId();
                        //Using the StringWithTag class to store the ID of the coin
                        coinInfo.add(new BankFragment.StringWithTag(info,c.getId(),currency,c.getValue(),owner));
                    }
                }

                if(getActivity() != null){
                    ArrayAdapter<StringWithTag> adapter = new ArrayAdapter<>(getActivity(),
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

    @SuppressLint("LogNotTimber")
    private void updateChangeRate() {
        String preferencesFile = "LastDownloadDate";
        //get the rates from the shared preferences
        SharedPreferences settings = Objects.requireNonNull(getContext())
                .getSharedPreferences(preferencesFile,Context.MODE_PRIVATE);

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
            Log.d(tag,"[OnUpdate Change rate] current rate updated");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("LogNotTimber")
    private void updateBankinfo() {
        //fetch the data of bank account information from the database and
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference userinfoRef = database.getReference("bank");
        Log.d(tag,"[OnconnectDatabase]Conected to the database!"+ currentUser.getEmail());
        userinfoRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(bankList != null){
                    bankList.clear();
                }
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    BankAccount bankAccount = userSnapshot.getValue(BankAccount.class);
                    bankList.add(bankAccount);
                    if (bankAccount != null && bankAccount.getId().equals(currentUser.getUid())) {
                        remaining = bankAccount.getRemainingCoin();
                        String accountDate = bankAccount.getLastCountDate();
                        if (!accountDate.equals(downloadDate)){
                            remaining = 20;
                        }
                        textViewRemainingCoin.setText(remaining.toString());
                    }
                }
                Log.d(tag,"[Getting bankinfo] bankList size:"+bankList.size());

                class SortByGold implements Comparator<BankAccount> {
                    public int compare(BankAccount a, BankAccount b) {
                        if(a.getGold() < b.getGold()) return -1;
                        else if(a.getGold() == b.getGold()) return 0;
                        else return 1;
                    }
                }

                bankList.sort(Comparator.comparing(BankAccount::getGold));
                Collections.reverse(bankList);

                List<String> bankinfo = new ArrayList<>();
                for(BankAccount b:bankList){
                    DecimalFormat df = new DecimalFormat("#.##");
                    Double value = b.getGold();
                    value = Double.valueOf(df.format(value));
                    bankinfo.add("   "+b.getEmail()+":  "+value+"  gold");
                }

                if(getActivity() != null){
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
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
        ownerId = s.ownerId;
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
            @SuppressLint({"LogNotTimber", "ShowToast", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(tag,"[On storing Coin] Trying to store the coin");
                BankAccount bankAccount = dataSnapshot.getValue(BankAccount.class);
                if(selectedId != null){
                    Double gold = selectedValue * rates.get(selectedCurrency);
                    walletRef = database.getReference("users").child(currentUser.getUid()).child("wallet").child(selectedId);

                        if(bankAccount == null){
                            BankAccount b = new BankAccount(gold,currentUser.getUid(),currentUser.getEmail(),downloadDate,19);
                            bankRef.setValue(b);
                            walletRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Coin coin = dataSnapshot.getValue(Coin.class);
                                    if (coin != null) {
                                        if(coin.getFirstOwnerId().equals(currentUser.getUid())){
                                            remaining = 19;
                                        }else{
                                            remaining = 20;
                                        }
                                        textViewRemainingCoin.setText(remaining.toString());
                                    }
                                    walletRef.removeValue();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }else{
                            if(remaining == 0){
                                walletRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        Coin coin = dataSnapshot.getValue(Coin.class);
                                        if (bankAccount != null) {
                                            Double newgold = bankAccount.getGold()+gold;
                                        }
                                        if (coin != null) {
                                            if(coin.getFirstOwnerId().equals(currentUser.getUid())){
                                                Toast.makeText(getActivity(),"You can only store 20 your coins everyday, " +
                                                        "try to exchange with other user!",Toast.LENGTH_LONG).show();
                                                Log.d(tag,"[On storing Coin] reach maximum number of current day");
                                            }else{
                                                BankAccount b = new BankAccount(gold,currentUser.getUid(),currentUser.getEmail(),downloadDate,0);
                                                walletRef.removeValue();
                                                Toast.makeText(getActivity(),"Your Coin has been transfered to the Bank",Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });}else{
                                Double newgold = bankAccount.getGold()+gold;
                                walletRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        Coin coin = dataSnapshot.getValue(Coin.class);
                                        if (coin != null) {
                                            if(coin.getFirstOwnerId().equals(currentUser.getUid())){
                                                if(bankAccount.getLastCountDate().equals(downloadDate)){
                                                    remaining = bankAccount.getRemainingCoin() - 1;

                                                    Log.d(tag,"[Current selected Coin]"+selectedId);
                                                }else {
                                                    remaining = 19;

                                                }
                                                BankAccount b = new BankAccount(newgold,currentUser.getUid(),
                                                        currentUser.getEmail(),downloadDate,remaining);
                                                bankRef.setValue(b);
                                                walletRef.removeValue();
                                            }else{
                                                walletRef.removeValue();
                                                BankAccount b = new BankAccount(newgold,currentUser.getUid()
                                                        ,currentUser.getEmail(),downloadDate,remaining);
                                                bankRef.setValue(b);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });}
                        }
                            selectedId = null;

                }else {
                        Toast.makeText(getActivity(),"please select a coin",Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    //The inner class StringWithTag for storing the id with the content
    public class StringWithTag {
        public String string;
        public String id;
        public String currency;
        public Double value;
        String ownerId;

        StringWithTag(String stringPart, String tagPart, String currency, Double value, String ownerId) {
            string = stringPart;
            id = tagPart;
            this.currency = currency;
            this.value = value;
            this.ownerId = ownerId;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
