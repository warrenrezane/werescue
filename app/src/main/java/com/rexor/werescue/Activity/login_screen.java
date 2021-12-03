package com.rexor.werescue.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rexor.werescue.ProgressbarLoader;
import com.rexor.werescue.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Map;

public class login_screen extends AppCompatActivity {

    EditText logphone;
    Button loginbtn;
    TextView txtsign, slide_logtxt;
    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;
    ProgressbarLoader loader;
    FirebaseUser firebaseUser;
    Animation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        logphone = findViewById(R.id.edittext_phonenumber);
        loginbtn = findViewById(R.id.login_button);
        txtsign = findViewById(R.id.logtosign);
        slide_logtxt = findViewById(R.id.slide_login_text);

        //set animation
        animation = AnimationUtils.loadAnimation(login_screen.this, R.anim.fade_anim);
        animation.setDuration(1000);
        slide_logtxt.setAnimation(animation);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        firebaseUser = firebaseAuth.getCurrentUser();

        //initilize progressbar
        loader = new ProgressbarLoader(login_screen.this);

        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.showloader();
                loginlistner();
            }
        });

        txtsign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtsignuplistener();
            }
        });

    }

    private void txtsignuplistener() {
        Intent intent = new Intent(login_screen.this, signup_screen.class);
        startActivity(intent);
    }

    private void loginlistner() {
        String phonenumber = logphone.getText().toString().trim();
        if (TextUtils.isEmpty(phonenumber)) {
            Toast.makeText(login_screen.this, "Phone number cannot be empty.", Toast.LENGTH_SHORT).show();
            loader.dismissloader();
        } else {
            // FIND USER FIRST IN REALTIME FIREBASE BEFORE SIGNING IN
            databaseReference.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isFinished = false, isFound = false;
                            String email = "", pass = "";
                            Map<String, Object> users = (Map<String, Object>) snapshot.getValue();

                            try {
                                for (Map.Entry<String, Object> entry : users.entrySet()) {
                                    Map singleUser = (Map) entry.getValue();

                                    if (singleUser.get("phonenumber").toString().equals(phonenumber)) {
                                        email = singleUser.get("email").toString();
                                        pass = singleUser.get("password").toString();
                                        isFound = true;
                                        break;
                                    }
                                }

                                isFinished = true;

                                if (isFinished && !isFound) {
                                    Toast.makeText(login_screen.this, "User not found.", Toast.LENGTH_SHORT).show();
                                    loader.dismissloader();
                                }
                                else if (isFinished && isFound) {
                                    authenticate(email, pass);
                                }
                            }
                            catch (NullPointerException e) {
                                Toast.makeText(login_screen.this, "There were no users existed.", Toast.LENGTH_SHORT).show();
                                loader.dismissloader();
                            }
                        }


                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    }
            );
        }
    }

    @Override
    protected void onStart() {
        if (firebaseUser != null){
            startActivity(new Intent(login_screen.this, home.class));
            finish();
        }
        super.onStart();
    }

    private void authenticate(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        loader.dismissloader();
                        Toast.makeText(login_screen.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(login_screen.this, showcirclecode.class);
                        startActivity(intent);
                    } else {
                        loader.dismissloader();
                        Toast.makeText(login_screen.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
}
