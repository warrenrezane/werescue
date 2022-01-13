package com.rexor.werescue.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arsy.maps_library.MapRipple;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.rexor.werescue.ProgressbarLoader;
import com.rexor.werescue.R;
import com.rexor.werescue.Fragments.invitecodefragment;
import com.rexor.werescue.Fragments.joincirclefragment;
import com.rexor.werescue.Fragments.mycirclefragment;
import com.rexor.werescue.Fragments.profilefagment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class home extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    Toolbar toolbar;
    NavigationView navigationView;
    ActionBarDrawerToggle actionBarDrawerToggle;
    GoogleMap mMap;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LatLng latLng;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    TextView header_name, header_email, header_emergency_status;
    DatabaseReference databaseReference, circleReference;
    String current_uid;
    AlertDialog.Builder builder;
    List<Boolean> hasEmergency;
    LatLngBounds.Builder latlngbuilder;
    List<Marker> markers;
    ProgressbarLoader loader;
    boolean emergency_status;
    Marker you;

    int called;
    int zoom;
    List<String> circles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerlayout);
        toolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);
        navigationView = findViewById(R.id.nev_view);
        navigationView.setNavigationItemSelectedListener(this);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.app_name,R.string.app_name);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        loader = new ProgressbarLoader(home.this);

        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        current_uid = firebaseUser.getUid();
        called = 0;
        zoom = 1;
        circles = new ArrayList<>();
        markers = new ArrayList<>();
        hasEmergency = new ArrayList<>();
        latlngbuilder = new LatLngBounds.Builder();
        emergency_status = false;

        circleReference = databaseReference.child(current_uid).child("circle_members");

        dynamicheaderlistener();
        getCircles();
        callpermissionlistener();
        update_location();
    }

    private void dynamicheaderlistener() {

        View header = navigationView.getHeaderView(0);

        header_name = header.findViewById(R.id.header_name_text);
        header_email = header.findViewById(R.id.header_email_text);
        header_emergency_status = header.findViewById(R.id.header_emergency_status);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String current_name = dataSnapshot.child(current_uid).child("firstname").getValue(String.class);
                String current_email = dataSnapshot.child(current_uid).child("email").getValue(String.class);
                boolean current_emergency = dataSnapshot.child(current_uid).child("emergency").getValue(boolean.class);

                emergency_status = current_emergency;

                String s1 = "Hi "+current_name;
                header_name.setText(s1);
                header_email.setText(current_email);

                if (current_emergency) {
                    header_emergency_status.setTextColor(Color.parseColor("#eb1313"));
                } else {
                    header_emergency_status.setTextColor(Color.parseColor("#c2c2c2"));
                }

                header_emergency_status.setText(current_emergency ? "Enabled" : "Disabled");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void update_location() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PermissionChecker.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PermissionChecker.PERMISSION_GRANTED)
        {
            fusedLocationProviderClient = new FusedLocationProviderClient(this);
            locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(3000) //update interval
                    .setFastestInterval(5000); //fastest interval
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null)
                    {
                        mMap.clear();
                        watchForEmergency();

                        final double lat = locationResult.getLastLocation().getLatitude();
                        final double log = locationResult.getLastLocation().getLongitude();
                        latLng = new LatLng(lat, log);
                        you = mMap.addMarker(new MarkerOptions().position(latLng).title("You"));

                        if (called == 0) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F));
                        }
                        else {
                            if (!hasEmergency.isEmpty()) {
                                markers.add(you);

                                Iterator itr = markers.iterator();

                                while (itr.hasNext()) {
                                    Marker marker = (Marker) itr.next();
                                    marker.showInfoWindow();
                                    latlngbuilder.include(marker.getPosition());
                                }

                                Log.d("markers", markers.toString());

                                LatLngBounds bounds = latlngbuilder.build();
                                Log.d("bounds", bounds.toString());
                                int width = getResources().getDisplayMetrics().widthPixels;
                                int height = getResources().getDisplayMetrics().heightPixels;
                                int padding = (int) (width * 0.1);

                                if (zoom == 1) {
                                    builder = new AlertDialog.Builder(home.this);
                                    builder.setMessage("You have people in your circle that needs help!")
                                            .setTitle("Emergency Alert!")
                                            .setCancelable(false)
                                            .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();

                                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                                    mMap.animateCamera(cu);
                                }

                                zoom = 0;
                                markers.clear();
                            }
                        }

                        //update latitude and longitude
                        Map <String, Object> update = new HashMap<>();
                        update.put("latitude", lat);
                        update.put("longitude", log);
                        databaseReference.child(current_uid).updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
//                                Toast.makeText(home.this, "Location updated", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else {
                        Toast.makeText(home.this,"location not found", Toast.LENGTH_SHORT).show();
                    }
                    called = 1;
                }
            },getMainLooper());
        }
        else
        {
            callpermissionlistener();
        }
    }

    private void callpermissionlistener() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        String rationale = "Please provide location permission so that you can ...";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("location permission")
                .setSettingsDialogTitle("warning");

        Permissions.check(this, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                mapFragment.getMapAsync(home.this);
                update_location();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                callpermissionlistener();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            backpressedwarrning();
        }
        //super.onBackPressed();
    }
    public void backpressedwarrning(){
        builder = new AlertDialog.Builder(home.this);
        builder.setMessage("Do you want to close this application ?")
                .setTitle(R.string.dialog_title)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.setTitle("Exit");
        alert.show();
    }

    public void getCircles() {
//        circleReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DataSnapshot> task) {
//                if (!task.isSuccessful()) {
//                    Log.e("circlereference", "Error getting data", task.getException());
//                }
//                else {
//                    DataSnapshot snapshot = task.getResult();
//                    if (snapshot.exists()) {
//                        for (DataSnapshot dss : snapshot.getChildren()) {
//                            String person = dss.child("circlememberid").getValue(String.class);
//                            circles.add(person);
//                        }
//                    }
//                }
//            }
//        });

        circleReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dss : snapshot.getChildren()) {
                        String person = dss.child("circlememberid").getValue(String.class);
                        circles.add(person);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void watchForEmergency() {
       Iterator itr = circles.iterator();
       while (itr.hasNext()) {
           String person = (String) itr.next();

           databaseReference.child(person).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
               @Override
               public void onComplete(@NonNull Task<DataSnapshot> task) {
                   if (!task.isSuccessful()) {
                       Log.e("databasereference", "Error getting data", task.getException());
                   }
                   else {
                       DataSnapshot snapshot = task.getResult();

                       boolean emergency = snapshot.child("emergency").getValue(boolean.class);
                       if (emergency) {
                           hasEmergency.add(true);
                           double latitude = snapshot.child("latitude").getValue(double.class);
                           double longitude = snapshot.child("longitude").getValue(double.class);
                           String name = snapshot.child("firstname").getValue(String.class) + " " + snapshot.child("lastname").getValue(String.class);
                           final LatLng person = new LatLng(latitude, longitude);

                           Marker marker = mMap.addMarker(new MarkerOptions().position(person).title(name));
                           markers.add(marker);
                       }
                   }
               }
           });
       }
//        circleReference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot circleSnapshot) {
//                if (circleSnapshot.exists())
//                {
//                    LatLngBounds.Builder latlngbuilder = new LatLngBounds.Builder();
//                    for (DataSnapshot dss : circleSnapshot.getChildren())
//                    {
//                        databaseReference.child(dss.child("circlememberid").getValue(String.class)).addValueEventListener(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot dbSnapshot) {
//                                try {
//                                    boolean emergency = dbSnapshot.child("emergency").getValue(boolean.class);
//                                    if (emergency) {
//                                        double latitude = dbSnapshot.child("latitude").getValue(double.class);
//                                        double longitude = dbSnapshot.child("longitude").getValue(double.class);
//                                        String name = dbSnapshot.child("firstname").getValue(String.class) + " " + dbSnapshot.child("lastname").getValue(String.class);
//                                        final LatLng person = new LatLng(latitude, longitude);
//
//                                        Marker marker = googleMap.addMarker(new MarkerOptions().position(person).title(name));
//                                        latlngbuilder.include(marker.getPosition());
//                                    }
//
//                                }
//                                catch (NullPointerException e) {
//                                    Log.d("Debug", e.getLocalizedMessage());
//                                    Toast.makeText(home.this, "There has been an internal problem. ", Toast.LENGTH_SHORT).show();
//                                }
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError databaseError) { }
//                        });
//                    }
//

//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        /**
         * TODO: Create a functionality that will get all the people that has the emergency data set to "true".
         * Write code below.
         */
//        watchForEmergency(googleMap);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.nev_home:
                startActivity(new Intent(home.this, home.class));
                finish();
                break;
            case R.id.nev_profile:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new profilefagment()).commit();
                /*getSupportActionBar().setTitle("profile");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/
                break;
            case R.id.nev_joiningc:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new joincirclefragment()).commit();
                break;
            case R.id.nev_invite:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new invitecodefragment()).commit();
                break;
            case R.id.nev_mycircle:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new mycirclefragment()).commit();
                break;
            case R.id.nev_logout:
                firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null)
                {
                    firebaseAuth.signOut();
                    startActivity(new Intent(home.this, login_screen.class).setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK
                    ));
                    this.finish();
                }
                break;
            case R.id.zoom_to_position:
                loader.showloader();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F));
                loader.dismissloader();
                break;

            case R.id.zoom_to_emergency:
                if (!hasEmergency.isEmpty()) {
                    markers.add(you);

                    Iterator itr = markers.iterator();

                    while (itr.hasNext()) {
                        Marker marker = (Marker) itr.next();
                        marker.showInfoWindow();
                        latlngbuilder.include(marker.getPosition());
                    }

                    Log.d("markers", markers.toString());

                    LatLngBounds bounds = latlngbuilder.build();
                    Log.d("bounds", bounds.toString());
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int padding = (int) (width * 0.1);

                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                    mMap.animateCamera(cu);

                    zoom = 0;
                    markers.clear();
                }
                else {
                    Toast.makeText(home.this, "You don't have any emergencies on your circle.", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.toggle_emergency:
                if (emergency_status) {
                    builder = new AlertDialog.Builder(home.this);
                    builder.setMessage("Disable Emergency status?")
                            .setTitle("Enable/Disable Emergency Status")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    loader.showloader();
                                    Map <String, Object> update = new HashMap<>();
                                    update.put("emergency", false);

                                    databaseReference.child(current_uid).updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            header_emergency_status.setText("Disabled");
                                            loader.dismissloader();
                                            Toast.makeText(home.this, "Emergency Status Disabled.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else {
                    builder = new AlertDialog.Builder(home.this);
                    builder.setMessage("Enable Emergency status?")
                            .setTitle("Enable/Disable Emergency Status")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    loader.showloader();
                                    Map <String, Object> update = new HashMap<>();
                                    update.put("emergency", true);

                                    databaseReference.child(current_uid).updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            header_emergency_status.setText("Enabled");
                                            loader.dismissloader();
                                            Toast.makeText(home.this, "Emergency Status Enabled.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
