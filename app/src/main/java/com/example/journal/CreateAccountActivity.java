package com.example.journal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.JournalApi;

public class CreateAccountActivity extends AppCompatActivity {
    private Button loginButton;
    private Button createAcctButton;
    private FirebaseAuth firebaseAuth;//gateway to the firebase authentication
    private FirebaseAuth.AuthStateListener authStateListener;//to listen all of the events firebase is firing.
    private FirebaseUser currentUser;//able to fetch the current user who is logged in.

    //Firestore Connection
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference collectionReference = db.collection("Users");//add a collection named users.

    private EditText emailEditText;
    private EditText passwordEditText;
    private ProgressBar progressBar;//loading symbol
    private EditText userNameEditText;

    String regularexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[$_@])[A-Za-z\\d$_@]{8,}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();// the entry point of firebase authentication sdk.

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        createAcctButton = findViewById(R.id.create_acct_button);
        progressBar = findViewById(R.id.create_acct_progress);
        emailEditText = findViewById(R.id.email_account);
        passwordEditText = findViewById(R.id.password_account);
        userNameEditText = findViewById(R.id.username_account);


//to update the changes in authentication state.
       authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();

                if (currentUser != null) {
                    //user is already logged in
                } else {
                    //no user yet
                }
            }
        };


        createAcctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!TextUtils.isEmpty(emailEditText.getText().toString())
                        && !TextUtils.isEmpty(passwordEditText.getText().toString())
                        && !TextUtils.isEmpty(userNameEditText.getText().toString())) {

                    String email = emailEditText.getText().toString().trim();
                    String password = passwordEditText.getText().toString().trim();
                    String username = userNameEditText.getText().toString().trim();
                    if(validatepassword(password)){
                        Toast.makeText(getBaseContext(),"Valid password",Toast.LENGTH_LONG).show();
                        Bundle bundle = new Bundle();
                        bundle.putString("User",username);
                        bundle.putString("Email",email);
                        bundle.putString("Password",password);
                    }
                    else
                    {
                        startActivity(new Intent(CreateAccountActivity.this,CreateAccountActivity.class));
                        finish();
                        Toast.makeText(getBaseContext(),"Invalid password",Toast.LENGTH_LONG).show();
                    }

                    createUserEmailAccount(email, password, username);

                 } else {
                    Toast.makeText(CreateAccountActivity.this, "Empty Fields Not Allowed", Toast.LENGTH_LONG).show();

                }
            }
        });
    }

    private boolean validatepassword(String password) {
        Pattern pattern = Pattern.compile(regularexp);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private void createUserEmailAccount(String email, String password, String username) {
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(username)) {

            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                //we take user to PostJournalActivity
                                currentUser = firebaseAuth.getCurrentUser();
                                assert currentUser != null;
                                String currentUserId = currentUser.getUid();

                                //Create a user Map so we can create a user in the User Collection
                                Map<String, String> userObj = new HashMap<>();
                                userObj.put("userId", currentUserId);
                                userObj.put("username", username);

                                //save to out firestore database
                                collectionReference.add(userObj).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                              if(Objects.requireNonNull(task.getResult()).exists()){
                                                  progressBar.setVisibility(View.INVISIBLE);
                                                  String name  = task.getResult().getString("username");

                                                  JournalApi journalApi = JournalApi.getInstance();//Global api
                                                  journalApi.setUserId(currentUserId);
                                                  journalApi.setUsername(name);

                                                  Intent intent = new Intent(CreateAccountActivity.this, PostJournalActivity.class);
                                                  intent.putExtra("username",name);
                                                  intent.putExtra("userId",currentUserId);
                                                  startActivity(intent);

                                              }else {
                                                  progressBar.setVisibility(View.INVISIBLE);

                                              }
                                            }
                                        });

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });


                            } else {
                                //something went wrong
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });

        }else{

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}
