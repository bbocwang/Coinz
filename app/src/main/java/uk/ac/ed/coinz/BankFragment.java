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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
* This is the Bank fragment.
*
* User can send their coins to the central bank using this fragment.
* there's a spinner to chose which coin to send.
* A text view will show user how many coins they can still store in the bank today.
* A leaderboard is showed as a bonus feature, the user has higest gold value in their bank account
* will have a higher ranking and everyone can see the leader board.
*
* Acknowledgement:
* https://www.youtube.com/watch?v=BO4zdmkTi48
* https://www.youtube.com/watch?v=M2dVJgGNs3U
* https://developer.android.com/guide/topics/ui/controls/spinner
* */
public class BankFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    //layout related stuff
    Spinner coinSpinner;
    private final String tag = "TransferFragment";
    private String selectedId;
    private String selectedCurrency;
    private Double selectedValue;
    private List<BankAccount> bankList;
    private String downloadDate;
    private Integer remaining;
    TextView textViewRemainingCoin;
    private ListView listView;
    private int isNewUser;

    //Database related stuff
    private DatabaseReference walletRef;
    private List<Coin> coinList;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase database;
    private Map<String, Double> rates;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //initialize the variables
        database = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        walletRef = database.getReference("users").child(currentUser.getUid());
        coinList = new ArrayList<>();
        selectedId = null;
        selectedValue = null;
        selectedCurrency = null;
        selectedId = null;
        selectedValue = null;
        selectedCurrency = null;
        bankList = new ArrayList<>();
        rates = new HashMap<>();
        isNewUser = 1;

        //fetch bank account information from database
        updateBankinfo();
        //update the current rate
        updateChangeRate();
        //check if current user is a new user
        checkNewUser();

        //initialize the layout
        View view = inflater.inflate(R.layout.fragment_bank, container, false);
        view.findViewById(R.id.sendBankButton).setOnClickListener(this);
        textViewRemainingCoin = view.findViewById(R.id.remainingCoin);
        listView = view.findViewById(R.id.bankList);
        coinSpinner = view.findViewById(R.id.bankCoinspinner);
        coinSpinner.setOnItemSelectedListener(this);


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
                        coinInfo.add(new BankFragment.StringWithTag(info,c.getId()
                                ,currency,c.getValue(),owner));
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


    //check if this is the first day of the current user, if it is, when sending the coins to the
    //bank, the user will get double gold
    private void checkNewUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newUserRef = db.collection("NewUser").document(currentUser.getUid());
        newUserRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot != null){
                    NewUser newUser = documentSnapshot.toObject(NewUser.class);
                    if (newUser != null && newUser.getNewUserId()
                            .equals(currentUser.getUid()) && newUser
                            .getRegisterDate().equals(downloadDate)) {
                        isNewUser = 2;
                        Toast.makeText(getActivity(),"You can get double gold coins " +
                                "when you send some coin to the bank today!",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
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
            // get the rates from the json file
            // rates are stored in a hash map
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
        //fetch the data of bank account information from the database
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference userinfoRef = database.getReference("bank");
        Log.d(tag,"[OnconnectDatabase]Conected to the database!"+ currentUser.getEmail());
        userinfoRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(bankList != null){
                    bankList.clear();//make sure to clean the banklist before update
                }
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    BankAccount bankAccount = userSnapshot.getValue(BankAccount.class);
                    bankList.add(bankAccount);
                    if (bankAccount != null && bankAccount.getId().equals(currentUser.getUid())) {
                        remaining = bankAccount.getRemainingCoin();
                        String accountDate = bankAccount.getLastCountDate();
                        if (!accountDate.equals(downloadDate)){
                            remaining = 25;
                        }
                        textViewRemainingCoin.setText(remaining.toString());
                    }
                }
                Log.d(tag,"[Getting bankinfo] bankList size:"+bankList.size());

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
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


    /*When click the store button, try to send the coin to the bank
    *
    * There are several things to consider:
    * 1. If current user doesn't have a bank account(there's no data about user in bank database)
    *    ,create a new bank account for user in the database, store the coin.
    *
    * 2. Every user can only store 25 of their own coins to the bank per day.
    *
    * 3. If the coin is not collected by the current user, the remaining coin number will not
    *   change, and coin will be sent to the bank
    *
    * 4. If the remaining coin number is 0, and the current coin is collected by the current user,
    *    reject, and make a toast: please share your spare change with other players
    * */
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

                    //here, if the current user is a new user, the gold value is doubled.
                    Double gold = selectedValue * rates.get(selectedCurrency)*isNewUser;
                    walletRef = database.getReference("users").child(currentUser
                            .getUid()).child("wallet").child(selectedId);


                    if(bankAccount == null){
                        BankAccount b = new BankAccount(gold,currentUser.getUid()
                                ,currentUser.getEmail(),downloadDate,24);

                        bankRef.setValue(b);//create new bank account for new user

                        walletRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Coin coin = dataSnapshot.getValue(Coin.class);
                                if (coin != null) {
                                    if(coin.getFirstOwnerId().equals(currentUser.getUid())){
                                        remaining = 24;
                                    }else{
                                        remaining = 25;
                                    }
                                    textViewRemainingCoin.setText(remaining.toString());
                                }
                                walletRef.removeValue();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {}
                        });
                    }else{

                        //if the current user already have a bank account in the bank
                        if(remaining == 0){
                            walletRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Coin coin = dataSnapshot.getValue(Coin.class);
                                    if (coin != null) {
                                        if(coin.getFirstOwnerId().equals(currentUser.getUid())){
                                            //if the user trying to send more than 25 of their own coin
                                            Toast.makeText(getActivity(),"You can only store 20 your coins everyday, " +
                                                    "try to share spare change with other users!",Toast.LENGTH_LONG).show();
                                            Log.d(tag,"[On storing Coin] reach maximum number of current day");
                                        }else{
                                            Double newgold = bankAccount.getGold()+gold;
                                            BankAccount b = new BankAccount(newgold,currentUser.getUid()
                                                    ,currentUser.getEmail(),downloadDate,0);
                                            bankRef.setValue(b);
                                            walletRef.removeValue();
                                            Toast.makeText(getActivity(),"Your Coin has been transfered to the Bank"
                                                    ,Toast.LENGTH_LONG).show(); }
                                    }
                                }


                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {}
                            });}else{

                            //remaining coin is not 0, store the coin
                            Double newgold = bankAccount.getGold()+gold;
                            walletRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Coin coin = dataSnapshot.getValue(Coin.class);
                                    if (coin != null) {
                                        if(coin.getFirstOwnerId().equals(currentUser.getUid())){
                                            //if the coin is collected by the current user,
                                            //remaining coin will -1
                                            if(bankAccount.getLastCountDate().equals(downloadDate)){
                                                remaining = bankAccount.getRemainingCoin() - 1;
                                                Log.d(tag,"[Current selected Coin]"+selectedId);
                                            }else {
                                                //if current date is not the last download date,
                                                //reset the remaining coin number
                                                remaining = 24;
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

                    //set select id to null, to avoid double send of the same coin
                    selectedId = null;
                }else {
                    //if nothing selected, show a toast
                    if(remaining != 0){
                        //if remaining != 0 , means no coin selected, show the toast
                        Toast.makeText(getActivity(),"please select a coin",Toast.LENGTH_LONG).show();
                    }else {
                        //if remaining is 0, means user can't store their own coins any more
                        Toast.makeText(getActivity(),"You can only store 20 of your coins everyday" +
                                ". Please exchange coins with other users, or store it tomorrow"
                                ,Toast.LENGTH_LONG).show();
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
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
