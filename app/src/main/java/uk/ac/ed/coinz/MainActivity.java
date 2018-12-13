package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

import java.util.List;

/*This is a activity for login
*
* There are two textview which user can input their email address and password to login
* after click the sign in button, the activity will try to sign in.
*
* The activity will check if the username and passward is valid, if it's not, show
* a toast to user, if it is, then sign in and open the game activity
*
* If user doesn't have a accrount, they click the register, then app will go to register
* activity to let user regist their account.
*
*  Acknowledgement:
* https://firebase.google.com/docs/auth/
* https://www.youtube.com/watch?v=mF5MWLsb4cg
* */
public class MainActivity extends AppCompatActivity  implements View.OnClickListener, PermissionsListener {
    private final String tag = "MainActivity";

    private FirebaseAuth mAuth;
    EditText loginUsername;
    EditText loginPassword;
    ProgressBar progressBar;
    PermissionsManager permissionsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        findViewById(R.id.TextViewRegister).setOnClickListener(this);
        findViewById(R.id.loginButton).setOnClickListener(this);
        loginUsername = findViewById(R.id.LoginUsername);
        loginPassword = findViewById(R.id.LoginPassword);
        progressBar = findViewById(R.id.progressbar0);
        permissionsManager = new PermissionsManager(this);
    }


    @SuppressLint("LogNotTimber")
    public Boolean login(String email, String password) {

        //check if the email is empty
        if(email.isEmpty()){
            if(loginUsername != null){
                loginUsername.setError("An email Address is required");
                loginUsername.requestFocus();
            }
            return false;
        }

        //check if the password is empty
        if(password.isEmpty()){
            if(loginPassword != null){
                loginPassword.setError("Password is required");
                loginPassword.requestFocus();
            }
            return false;
        }

        //check if the password length >= 6
        if(password.length()<6){
            if(loginPassword != null){
                loginPassword.setError("The length of password should greater than 6");
                loginPassword.requestFocus();
            }
            return false;
        }

        //check if it's a valid email address
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            if(loginUsername != null){
                loginUsername.setError(("This is not a valid email"));
                loginUsername.requestFocus();
            }
            return false;
        }

        //set the progressBar visible
        progressBar.setVisibility(View.VISIBLE);
        Log.d(tag,"[On login] logging in");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    //set the progressBar invisible
                    progressBar.setVisibility((View.GONE));
                    if (task.isSuccessful()) {

                        // Sign in success, update UI with the signed-in user's information
                        Intent intent = new Intent(MainActivity.this,GameActivity.class);
                        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //start the game activity
                        if(!PermissionsManager.areLocationPermissionsGranted(this)){
                            permissionsManager.requestLocationPermissions(this);
                        }else{
                            startActivity(intent);
                        }
                    } else {

                        // If sign in fails, display a message.
                        Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }

                });
        return true;//if
    }

    @SuppressLint("LogNotTimber")
    public void onClick(View view){
        switch (view.getId()){
            case R.id.TextViewRegister:
                //if the regiter was clicked, go to register activity
                Log.d(tag,"[OnClick sign up] Opening sign up activity");
                startActivity(new Intent(this,RegisterActivity.class));

            case R.id.loginButton:
                //if the login was clicked, call login function to login
                String email = loginUsername.getText().toString().trim();
                String password = loginPassword.getText().toString().trim();
                Boolean loginBool = login(email,password);
                if (loginBool){
                    break;
                }
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this,"Sorry, we need the location permission to show you on the map",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            Intent intent = new Intent(MainActivity.this,GameActivity.class);
            startActivity(intent);
        }else{
            Toast.makeText(this,"Sorry, we need the location permission to show you on the map",Toast.LENGTH_LONG).show();
        }

    }
}
