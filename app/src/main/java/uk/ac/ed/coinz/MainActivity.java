package uk.ac.ed.coinz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.geojson.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.android.gms.common.internal.safeparcel.SafeParcelable.NULL;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener{
    private final String tag = "MainActivity";

    private TextView helloworld;
    private FirebaseAuth mAuth;
    EditText loginUsername;
    EditText loginPassword;
    ProgressBar progressBar;

    SharedPreferences usercoins;
    String stringcoins = "wallet";

    private final String preferencesFile = "UserCoin";//for storing preferences
    private List<String> coinList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        findViewById(R.id.TextViewRegister).setOnClickListener(this);
        findViewById(R.id.loginButton).setOnClickListener(this);
        loginUsername = (EditText) findViewById(R.id.LoginUsername);
        loginPassword = (EditText) findViewById(R.id.LoginPassword);
        progressBar = (ProgressBar) findViewById(R.id.progressbar0);

    }

    private void login() {
        String email = loginUsername.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();
        if(email.isEmpty()){
            loginUsername.setError("An email Address is required");
            loginUsername.requestFocus();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            loginUsername.setError(("This is not a valid email"));
            loginUsername.requestFocus();
            return;
        }
        if(password.isEmpty()){
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }
        if(password.length()<6){
            loginPassword.setError("The length of password should greater than 6");
            loginPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility((View.GONE));
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            //connectDatabase(user);
                            Intent intent = new Intent(MainActivity.this,GameActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.TextViewRegister:
                startActivity(new Intent(this,RegisterActivity.class));

            case R.id.loginButton:
                login();
                break;
        }
    }

    /*private void connectDatabase(FirebaseUser currentUser) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userAccountRef = database.getReference("users").child(currentUser.getUid());
        if(currentUser != null){
            Log.d(tag,"[got current user!]"+currentUser.getEmail().toString());
        }else{
            Log.d(tag,"[current use is null!!]");
        }
        if(userAccountRef != null){
            Log.d(tag,"[Conected to the database!]"+currentUser.getEmail().toString());
        }else{
            Log.d(tag,"[database ref is null!!]");
        }
        userAccountRef.addValueEventListener(new ValueEventListener() {
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
                List<Feature> repetition = new ArrayList<Feature>();
                for(DataSnapshot coinsSnapshot: dataSnapshot.getChildren()){
                    coinList.clear();
                    Map<String,Map<String,Object>> map = (Map<String, Map<String, Object>>) coinsSnapshot.getValue();
                    for(Map<String,Object> submap: map.values())
                    {
                        String currency = (String) submap.get("currency");
                        String id = (String) submap.get("id");
                        Double  value = (Double) submap.get("value");

                        Coin coin = new Coin(id,value,currency);
                        coinList.add(coin.getId());
                    }
                    Log.d(tag,"[!!!!coinList value]:"+coinList.toString());

                }

                //for(Coin i: coinList){
                //Log.d(tag,"[In the coinList class]:"+i.getClass().toString());
                //Log.d(tag,"[In the coinList]:"+i.toString());
                //Log.d(tag, String.format("[coin value]:%s", i.getValue()));
                //Log.d(tag, String.format("[coin currency]:%s", i.getCurrency()));
                //Log.d(tag, String.format("[coin id]:%s", i.getId()));
                //Log.d(tag, String.format("[the size of coinList:%d", coinList.size()));
                //}


                Log.d(tag, "[Realtime Database] Wallet updated" );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(tag, "Failed to read value.", error.toException());
            }
        });
        usercoins = getSharedPreferences("usercoins",0);
        SharedPreferences.Editor editor = usercoins.edit();
        editor.putStringSet(stringcoins, (Set<String>) coinList);
        editor.apply();

    }*/

}
