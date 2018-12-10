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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressLint("LogNotTimber")
public class TransferFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    Spinner coinSpinner;
    Spinner receiverSpinner;
    private final String tag = "TransferFragment";
    private DatabaseReference walletRef;
    private List<Coin> coinList;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase database;
    private String selectedId;
    private String selectedCurrency;
    private Double selectedValue;
    private String receiverEmail;
    EditText transfernote;
    private List<User> userList;

    //check if the coin is a spare change
    private String downloadDate;
    boolean checkSpareChange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        updateUserinfo();
        View view = inflater.inflate(R.layout.fragment_transfer, container, false);
        coinSpinner = view.findViewById(R.id.spinner_transfer);
        coinSpinner.setOnItemSelectedListener(this);
        receiverSpinner = view.findViewById(R.id.receiverSpinner);
        receiverSpinner.setOnItemSelectedListener(this);


        transfernote = view.findViewById(R.id.transferNote);
        view.findViewById(R.id.transferButton).setOnClickListener(this);

        checkSpareChange = checkSpareChange();


        walletRef = FirebaseDatabase.getInstance().getReference();
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

                            Coin coin = new Coin(id,value,currency,currentUser.getUid());
                            coinList.add(coin);
                        }
                    }
                    Log.d(tag,"[coinList size]:" + coinList.size());

                }
                List<StringWithTag> coinInfo = new ArrayList<>();
                if(coinList != null){
                    for(Coin c:coinList){
                        Double value = c.getValue();
                        String currency = c.getCurrency();
                        DecimalFormat df = new DecimalFormat("#.##");
                        value = Double.valueOf(df.format(value));
                        String value_string = String.valueOf(value);
                        String info = value_string +" "+ currency;
                        //Using the StringWithTag class to store the ID of the coin
                        coinInfo.add(new StringWithTag(info,c.getId(),currency,c.getValue()));
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.getId() == R.id.spinner_transfer){
            StringWithTag s = (StringWithTag) parent.getItemAtPosition(position);
            selectedId = s.id;
            selectedValue = s.value;
            selectedCurrency = s.currency;
        }
        if(parent.getId() == R.id.receiverSpinner){
            receiverEmail = (String) parent.getItemAtPosition(position);
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.transferButton:
                transfer();
        }
    }

    @SuppressLint("LogNotTimber")
    private void transfer() {
        String receiverId = null;
        Boolean found = false;
        for(User u: userList){
            if(u.getEmail().equals(receiverEmail)){
                receiverId = u.getUid();
                found = true;
            }
        }
        if(!found){
            Toast.makeText(getActivity(),"Receiver Email not found, please double check",Toast.LENGTH_SHORT).show();
        }else if(selectedId != null && checkSpareChange) {
            Log.d(tag,"[onTransfer]: coin has been sent to "+receiverEmail);
            Toast.makeText(getActivity(), "Your coin has been sent to" + receiverEmail,Toast.LENGTH_SHORT).show();
            DatabaseReference receiverRef = database.getReference("users").child(receiverId);
            walletRef.child("wallet").child(selectedId).removeValue();
            Coin coin = new Coin(selectedId,selectedValue,selectedCurrency,currentUser.getUid());
            receiverRef.child("wallet").child(selectedId).setValue(coin);
        }else {
            if(!checkSpareChange){

                Toast.makeText(getActivity(),"Sorry, You can only share your spare change!",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getActivity(), "Please Select a Coin",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //check if the user are sending the spare change
    private boolean checkSpareChange() {

        String preferencesFile = "LastDownloadDate";
        //get the rates from the shared preferences
        SharedPreferences settings = Objects.requireNonNull(getContext())
                .getSharedPreferences(preferencesFile, Context.MODE_PRIVATE);

        downloadDate = settings.getString("lastDownloadDate","");
        Log.d(tag, "[OnUpdate Change rate] Recalled lastDownloadDate is '"+downloadDate+"'");

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference bankRef = database.getReference("bank").child(currentUser.getUid());

        bankRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                BankAccount bankAccount = dataSnapshot.getValue(BankAccount.class);
                if(bankAccount == null){
                    checkSpareChange = false;
                    Toast.makeText(getActivity(),"Please go to bank create a bank account first",Toast.LENGTH_LONG).show();
                }
                if (bankAccount != null && !bankAccount.getLastCountDate().equals(downloadDate)) {
                    //it's another day
                    checkSpareChange = false;
                    Toast.makeText(getActivity(),"Sorry, You can only share the spare change!",Toast.LENGTH_LONG).show();
                }
                if (bankAccount != null && bankAccount.getLastCountDate().equals(downloadDate)) {
                    if (bankAccount.getRemainingCoin() == 0) {
                        checkSpareChange = true;
                    } else if (bankAccount.getRemainingCoin() > 0) {
                        checkSpareChange = false;
                        Toast.makeText(getActivity(),"Sorry, You can only share the spare change!",Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return checkSpareChange;
    }

    //The inner class StringWithTag for storing the id with the content
    public class StringWithTag {
        public String string;
        public String id;
        public String currency;
        public Double value;

        StringWithTag(String stringPart, String tagPart, String currency, Double value) {
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

    @SuppressLint("LogNotTimber")
    private void updateUserinfo() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        DatabaseReference userinfoRef = database.getReference("userinfo").child("user");
        Log.d(tag,"[OnconnectDatabase]Conected to the database!"+ currentUser.getEmail());
        userinfoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(userList != null){
                    userList.clear();
                }
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    User user = userSnapshot.getValue(User.class);
                    userList.add(user);
                    if (user != null) {
                        Log.d(tag,"[Getting userinfo]"+user.getEmail());
                    }
                }
                if(userList != null){
                    //noinspection StatementWithEmptyBody
                    if(userList.contains(new User(currentUser.getEmail(),currentUser.getUid()))){
                        //Do nothing
                    }else{
                        //add current user to the userinfo database
                        User user = new User(currentUser.getEmail(),currentUser.getUid());
                        userinfoRef.child(currentUser.getUid()).setValue(user);
                    }

                }
                Log.d(tag,"[Getting userinfo] Number of users:"+userList.size());

                List<String> emailList = new ArrayList<>();
                for(User u:userList){
                    emailList.add(u.getEmail());
                }

                if(getActivity() != null){
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                            android.R.layout.simple_list_item_1, emailList);
                    receiverSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
