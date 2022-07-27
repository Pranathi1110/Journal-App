
package com.example.journal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

import util.JournalApi;

public class MainActivity extends AppCompatActivity {
    private Button getStartedButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Users");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getStartedButton = findViewById(R.id.startButton);

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null){
                    currentUser = firebaseAuth.getCurrentUser();
                    String currentUserId = currentUser.getUid();

                    collectionReference.whereEqualTo("userId",currentUserId)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                    if (error !=null){
                                        return;
                                    }

                                    String name;
                                    if(!value.isEmpty()){
                                        for(QueryDocumentSnapshot snapshots: value) {
                                            JournalApi journalAPI = JournalApi.getInstance();
                                            journalAPI.setUserId(snapshots.getString("userId"));
                                            journalAPI.setUsername(snapshots.getString("username"));

                                            startActivity(new Intent(MainActivity.this,

                                                    JournalListActivity.class));
                                            finish();

                                        }
                                    }
                                }
                            });

                }
            }
        };

        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //we go to login
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(firebaseAuth != null){
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}

