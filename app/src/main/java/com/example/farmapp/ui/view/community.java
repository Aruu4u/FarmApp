package com.example.farmapp.ui.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.farmapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class community extends AppCompatActivity {
    View sendBtn, micbtn;
    EditText msgEt;
    ListView listView;
    DatabaseReference myRef;
    ChatAdapter msgAdapter;
    private ActivityResultLauncher<Intent> speechResultLauncher;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        Toolbar toolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.farmerCommunity);
        }

        listView = findViewById(R.id.listview);
        sendBtn = findViewById(R.id.sendBtn);
        msgEt = findViewById(R.id.msgEt);
        micbtn = findViewById(R.id.micBtn);

        ArrayList<ChatMessage> msgList = new ArrayList<>();
        msgAdapter = new ChatAdapter(this, msgList);
        listView.setAdapter(msgAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra(LoginActivity.EXTRA_OPEN_COMMUNITY, true);
            startActivity(loginIntent);
            finish();
            return;
        }
        user.reload();
        fetchSignedInUsername(user.getUid());

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("groupchat");
        loadMsg();

        speechResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> speechResult = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speechResult != null && !speechResult.isEmpty()) {
                            msgEt.setText(speechResult.get(0));
                        }
                    }
                }
        );

        sendBtn.setOnClickListener(v -> {
            String msg = msgEt.getText().toString().trim();
            if (msg.length() > 0) {
                ChatMessage chatMessage = new ChatMessage(getSenderName(), msg);
                myRef.push().setValue(chatMessage);
                msgEt.setText("");
            }
        });

        micbtn.setOnClickListener(v -> startSpeechToText());
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            speechResultLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Speech not supported: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMsg() {
        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                if (chatMessage != null && msgAdapter != null) {
                    msgAdapter.add(chatMessage);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(community.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSenderName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        // 1. Try Firebase Auth Display Name
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        // 2. Try SharedPreferences (Saved during Login/Register)
        String savedUsername = getSharedPreferences("farmapp_prefs", MODE_PRIVATE).getString("username", null);
        if (savedUsername != null && !savedUsername.trim().isEmpty()) {
            return savedUsername.trim();
        }

        // 3. Fallback to name in Prefs
        String savedName = getSharedPreferences("farmapp_prefs", MODE_PRIVATE).getString("user_name", null);
        if (savedName != null && !savedName.trim().isEmpty()) {
            return savedName.trim();
        }

        // 4. Final Fallback to email prefix or Anonymous
        if (user != null && user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().split("@")[0];
        }

        return "Farmer";
    }

    private void fetchSignedInUsername(String uid) {
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.getValue(String.class);
                        if (username != null && !username.trim().isEmpty()) {
                            getSharedPreferences("farmapp_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("username", username.trim())
                                    .apply();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
}
