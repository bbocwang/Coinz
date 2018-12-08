package uk.ac.ed.coinz;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

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
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    ProgressBar progressBar;
    EditText editTextEmail, editTextPassword;

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
                        Toast.makeText(getApplicationContext(),"Registered Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this,GameActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
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
}

