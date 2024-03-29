package com.example.firebaseintro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginSignup extends AppCompatActivity {
    private EditText email, password, displayname, phonenumber;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    Button signupBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);
        email=findViewById(R.id.emailText);
        password=findViewById(R.id.passwordText);
        phonenumber=findViewById(R.id.phoneNumberText);
        displayname=findViewById(R.id.displayNameText);
        signupBtn=findViewById(R.id.singupBtn);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        updateUI();
    }
    private void updateUI(){
        if(currentUser!=null){
            findViewById(R.id.displayNameLayout).setVisibility(View.GONE);
            findViewById(R.id.phoneNumberLayout).setVisibility(View.GONE);
            signupBtn.setVisibility(View.GONE);
        }
    }
    private void saveUserDataToDB(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("Users");
        usersRef.child(currentUser.getUid()).setValue(
                new User(displayname.getText().toString(),
                         email.getText().toString(),
                         phonenumber.getText().toString()));

    }
    public void ResetPassword(View view) {
        if(email.getText().toString().equals("")){
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.sendPasswordResetEmail(email.getText().toString()).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginSignup.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(LoginSignup.this, "Email sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void sendEmailVerification(View view) {
        if(mAuth.getCurrentUser()==null){
            Toast.makeText(this, "Please login first to resend verification email.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUser.sendEmailVerification()
                .addOnSuccessListener(LoginSignup.this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(LoginSignup.this, "Verification email Setn!", Toast.LENGTH_SHORT).show();
                        updateUI();
                    }
                }).addOnFailureListener(LoginSignup.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginSignup.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void Login(View view) {
        if(email.getText().toString().equals("")|| password.getText().toString().equals("")){
            Toast.makeText(this, "Please provide all information", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        currentUser=authResult.getUser();
                        if (currentUser == null){
                            // This should not happen
                            Toast.makeText(LoginSignup.this, "Error while handling your request. Please try again.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(currentUser.isEmailVerified()){ //for testing
                            Toast.makeText(LoginSignup.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginSignup.this, UserHome.class));
                            finish();
                        }
                        else
                        {
                            Toast.makeText(LoginSignup.this, "Please verify your email and login again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginSignup.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void Signup(View view) {
        if(email.getText().toString().equals("")|| password.getText().toString().equals("")
                || phonenumber.getText().toString().equals("")|| displayname.getText().toString().equals("")){
            Toast.makeText(this, "Please provide all information", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        currentUser=authResult.getUser();
                        if (currentUser == null){
                            // This should not happen
                            Toast.makeText(LoginSignup.this, "Error while handling your request. Please try again.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        currentUser.sendEmailVerification().addOnSuccessListener(LoginSignup.this, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(LoginSignup.this, "Signup successful. Verification email Sent!", Toast.LENGTH_SHORT).show();
                                saveUserDataToDB();
                                updateUI();
                            }
                        }).addOnFailureListener(LoginSignup.this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(LoginSignup.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginSignup.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}