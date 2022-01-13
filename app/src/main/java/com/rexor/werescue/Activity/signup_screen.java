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

import com.google.common.hash.Hashing;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rexor.werescue.ProgressbarLoader;
import com.rexor.werescue.R;
import com.rexor.werescue.Model.users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class signup_screen extends AppCompatActivity {

    EditText fname, lname, eemail, password, ephonenumber;
    Button signupbtn;
    TextView signtolog, signanim;
    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;
    ProgressbarLoader loader;
    Animation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_screen);

        fname = findViewById(R.id.edittext_firstname);
        lname = findViewById(R.id.edittext_lastname);
        eemail = findViewById(R.id.edittext_email);
        ephonenumber = findViewById(R.id.edittext_phonenumber);
        signupbtn = findViewById(R.id.signup_button);
        signtolog = findViewById(R.id.signtolog_txt);
        signanim = findViewById(R.id.faded_signup_text);

        //set animation
        animation = AnimationUtils.loadAnimation(signup_screen.this, R.anim.fade_anim);
        animation.setDuration(1000);
        signanim.setAnimation(animation);

        loader = new ProgressbarLoader(signup_screen.this);
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        firebaseAuth = FirebaseAuth.getInstance();

        signupbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signuplistner();
            }
        });

        signtolog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signintologinlistener();
            }
        });
    }

    private void signintologinlistener() {

        startActivity(new Intent(signup_screen.this, login_screen.class));
        finish();
    }

    private void signuplistner() {

        loader.showloader();
        try {
            final String first_name = fname.getText().toString().trim();
            final String last_name = lname.getText().toString().trim();
            final String email = eemail.getText().toString().trim();
            final String phonenumber = ephonenumber.getText().toString().trim();
            final String password = generateSHA256();

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            final String strdate = dateFormat.format(date);

            if (TextUtils.isEmpty(first_name) || TextUtils.isEmpty(last_name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(signup_screen.this, "Fill all the following fields.", Toast.LENGTH_SHORT).show();
                loader.dismissloader();
            } else {
                // CHECK TO SEE IF NUMBER HAS ALREADY EXISTED
                databaseReference.addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                try {
                                    if (snapshot.exists()) {
                                        boolean isFinished = false, isFound = false;
                                        Map<String, Object> users = (Map<String, Object>) snapshot.getValue();

                                        for (Map.Entry<String, Object> entry : users.entrySet()) {
                                            Map singleUser = (Map) entry.getValue();

                                            if (!singleUser.containsKey("phonenumber")) {
                                                continue;
                                            }
                                            else if (singleUser.containsKey("phonenumber") && singleUser.get("phonenumber").toString().equals(phonenumber)) {
                                                isFound = true;
                                                break;
                                            }
                                        }

                                        isFinished = true;

                                        if (isFinished && !isFound) {
                                            register(email, password, first_name, last_name, strdate, phonenumber);
                                        }
                                        else if (isFinished && isFound) {
                                            loader.dismissloader();
                                            Toast.makeText(signup_screen.this, "The phone number is already taken.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else {
                                        register(email, password, first_name, last_name, strdate, phonenumber);
                                    }
                                }
                                catch (NullPointerException e) {
                                    Log.d("Debug", e.getLocalizedMessage());
                                    Toast.makeText(signup_screen.this, "There has been an internal problem. ", Toast.LENGTH_SHORT).show();
                                    loader.dismissloader();
                                }
                            }


                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        }
                );
            }
        } catch (NullPointerException e) { }
    }

    private void register(String email, String password, String first_name, String last_name, String strdate, String phonenumber) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {

                        String userid = firebaseAuth.getCurrentUser().getUid();
                        users info = new users(userid, first_name, last_name, generatecode(), email, password, strdate, 0, 0, "null", phonenumber, false);
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(userid)
                                .setValue(info).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(signup_screen.this, "Successfully registered.", Toast.LENGTH_SHORT).show();
                                loader.dismissloader();
                                firebaseAuth.signOut();
                                Intent in = new Intent(signup_screen.this, login_screen.class);
                                startActivity(in);
                            }
                        });

                    } else {
                        Toast.makeText(signup_screen.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        loader.dismissloader();
                    }
                }
            });
    }

    private String generatecode() {
        Random r = new Random();
        int intcode = 100000 + r.nextInt(900000);
        String code = String.valueOf(intcode);
        return code;
    }

    private String generateSHA256() {
        String SHA256Hex = Hashing.sha256()
                .hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8)
                .toString();

        return SHA256Hex;
    }
}
