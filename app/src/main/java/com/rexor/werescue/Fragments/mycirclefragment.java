package com.rexor.werescue.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rexor.werescue.R;
import com.rexor.werescue.Adapter.membersadepter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class mycirclefragment extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;
    //users cuser;
    ArrayList<String> namelist, idlist, phonelist;

    DatabaseReference databaseReference, currentreference;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_circle_fragment, container, false);

        recyclerView = view.findViewById(R.id.view_recyclerview);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        String useruid = firebaseUser.getUid();

        namelist = new ArrayList<>();
        idlist = new ArrayList<>();
        phonelist = new ArrayList<>();

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        currentreference = FirebaseDatabase.getInstance().getReference("users").child(useruid).child("circle_members");

        currentreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                namelist.clear();
                idlist.clear();
                phonelist.clear();

                if (dataSnapshot.exists())
                {
                    for (DataSnapshot dss: dataSnapshot.getChildren())
                    {
                        String circleid = dss.child("circlememberid").getValue(String.class);

                        databaseReference.child(circleid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //users cuser = dataSnapshot.getValue(users.class);
                                String fetchname = dataSnapshot.child("firstname").getValue(String.class);
                                String fetchid = dataSnapshot.child("id").getValue(String.class);
                                String fetchphone = dataSnapshot.child("phonenumber").getValue(String.class);
                                namelist.add(fetchname);
                                idlist.add(fetchid);
                                phonelist.add(fetchphone);
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        adapter = new membersadepter(namelist, idlist, phonelist, getActivity());
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        return view;
    }
}
