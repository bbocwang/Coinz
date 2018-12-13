package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
/*
* This is a register activity
*
* The user can go to this activity to register their account
* the layout is very similar to the main activity, the mechanism of checking the valid email and
* password is the same, after the register, user will be directly go to the game activity.
*
* The register service we use is provided by firebase.
*
* Acknowledgement:
* https://firebase.google.com/docs/auth/
* https://www.youtube.com/watch?v=mF5MWLsb4cg
* */
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener, PermissionsListener {

    private FirebaseAuth mAuth;
    ProgressBar progressBar;
    EditText editTextEmail, editTextPassword;
    PermissionsManager permissionsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.LoginPassword);
        findViewById(R.id.SignUpButton).setOnClickListener(this);
        findViewById(R.id.signUpButton).setOnClickListener(this);
        progressBar = findViewById(R.id.progressbar);
        findViewById(R.id.textViewLogin).setOnClickListener(this);
        permissionsManager = new PermissionsManager(this);
    }



    public Boolean registerUser(String email, String password){

        //check if the email is empty
        if(email.isEmpty()){
            if(editTextEmail != null){
                editTextEmail.setError("An email Address is required");
                editTextEmail.requestFocus();
            }
            return false;
        }

        //check if the password is empty
        if(password.isEmpty()){
            if(editTextPassword != null){
                editTextPassword.setError("Password is required");
                editTextPassword.requestFocus();
            }
            return false;
        }

        //check if the password length >= 6
        if(password.length()<6){
            if(editTextPassword != null){
                editTextPassword.setError("The length of password should greater than 6");
                editTextPassword.requestFocus();
            }
            return false;
        }

        //check if it's a valid email address
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            if(editTextEmail != null){
                editTextEmail.setError(("This is not a valid email"));
                editTextEmail.requestFocus();
            }
            return false;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(RegisterActivity.this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()){
                        storeNewUser();
                        Toast.makeText(getApplicationContext(),"Thank you for registering, as a gift,  " +
                                "You can get double gold today!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(RegisterActivity.this,GameActivity.class);
                        if(!PermissionsManager.areLocationPermissionsGranted(this)){
                            permissionsManager.requestLocationPermissions(this);
                        }else{
                            startActivity(intent);
                        }
                    }
                    else {
                        if(task.getException() instanceof FirebaseAuthUserCollisionException){
                            Toast.makeText(getApplicationContext(),"This email has already been registered", Toast.LENGTH_SHORT).show();
                        }

                        else {
                            Toast.makeText(getApplicationContext(), Objects.requireNonNull(task.getException())
                                    .getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            return true;
    }

    //store the new user id and their registing date, for new user gift
    private void storeNewUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String newUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat")
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String currentDate = dateFormat.format(calendar.getTime());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser != null) {
            DocumentReference newUserRef = db.
                    collection("NewUser").document(currentUser.getUid());
            newUserRef.set(new NewUser(newUserId,currentDate)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signUpButton:
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                registerUser(email, password);
                break;
            case R.id.SignUpButton:
                email = editTextEmail.getText().toString().trim();
                password = editTextPassword.getText().toString().trim();
                registerUser(email,password);
                break;
            case R.id.textViewLogin:
                startActivity(new Intent(this,MainActivity.class));
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this,"Sorry, we need the location permission to show you on the map",Toast.LENGTH_LONG).show();
    }

    //the get location result call back function
    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            Intent intent = new Intent(RegisterActivity.this,GameActivity.class);
            startActivity(intent);
        }else{
            Toast.makeText(this,"Sorry, we need the location permission to show you on the map",Toast.LENGTH_LONG).show();
        }
    }
}

