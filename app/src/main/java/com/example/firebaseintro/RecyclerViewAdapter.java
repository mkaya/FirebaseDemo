package com.example.firebaseintro;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter< RecyclerViewAdapter.ViewHolder> {
    SimpleDateFormat localDateFormat= new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private class PostModel{
        public String postKey;
        public String uid;
        public String description;
        public String url;
        public String date;
        public PostModel(String uid, String description, String url, String date, String key) {
            this.uid=uid;
            this.description=description;
            this.url=url;
            this.date=date;
            this.postKey=key;
        }
    }
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference allPostsRef = database.getReference("Posts");
    ChildEventListener usersRefListener;
    private final FirebaseAuth mAuth;
    private final FirebaseUser currentUser;
    private final List<PostModel> postsList;
    private final RecyclerView r;

    public RecyclerViewAdapter(RecyclerView recyclerView){
        postsList =new ArrayList<>();
        r=recyclerView;
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        allPostsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                PostModel postModel=new PostModel(dataSnapshot.child("uid").getValue().toString(),
                        dataSnapshot.child("description").getValue().toString(),
                        dataSnapshot.child("url").getValue().toString(),
                        localDateFormat.format(new Date(Long.parseLong(dataSnapshot.child("timestamp").getValue().toString()))) ,
                        dataSnapshot.getKey());
                postsList.add(postModel);
                RecyclerViewAdapter.this.notifyItemInserted(postsList.size()-1);
                r.scrollToPosition(postsList.size()-1);
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent,false);
        final ViewHolder vh = new ViewHolder(v);
        return vh;
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final PostModel u =postsList.get(position);
        String uid=u.uid;
        if(holder.uref!=null && holder.urefListener!=null)
        {
            holder.uref.removeEventListener(holder.urefListener);
        }
        if(holder.likesRef!=null && holder.likesRefListener!=null)
        {
            holder.likesRef.removeEventListener(holder.likesRefListener);
        }
        if(holder.likeCountRef!=null && holder.likeCountRefListener!=null)
        {
            holder.likeCountRef.removeEventListener(holder.likeCountRefListener);
        }
        Picasso.get().load(u.url).into(holder.imageView);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        holder.uref = database.getReference("Users").child(uid);
        holder.uref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.fname_v.setText("First Name: " +dataSnapshot.child("displayname").getValue().toString());
                holder.email_v.setText("Email:  " + dataSnapshot.child("email").getValue().toString());
                holder.phone_v.setText("Phone Num:  " + dataSnapshot.child("phone").getValue().toString());
                holder.date_v.setText("Date Created: "+u.date);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.likeCountRef=
                database.getReference("Posts/"+u.postKey+"/likeCount");
        Log.d("LIKEC ", u.postKey);
        holder.likeCountRefListener = holder.likeCountRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d("CRASH", dataSnapshot.toString());
                if(dataSnapshot.getValue()!=null)
                    holder.likeCount.setText(dataSnapshot.getValue().toString()+" Likes");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.likesRef=database.getReference("Posts/"+u.postKey+"/likes/"+currentUser.getUid());
        holder.likesRefListener=holder.likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getValue().toString().equals("true"))
                {
                    holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(r.getContext(), R.drawable.like_active));
                }
                else{
                    holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(r.getContext(), R.drawable.like_disabled));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.getReference("Posts/"+u.postKey).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                        PhotoPreview.Post p = mutableData.getValue(PhotoPreview.Post.class);
                        if (p == null) {
                            return Transaction.success(mutableData);
                        }

                        if (p.likes.containsKey(currentUser.getUid())) {
                            // Unstar the post and remove self from stars
                            p.likeCount = p.likeCount - 1;
                            p.likes.remove(currentUser.getUid());
                        } else {
                            // Star the post and add self to stars
                            p.likeCount = p.likeCount + 1;
                            p.likes.put(currentUser.getUid(), true);
                        }

                        // Set value and report transaction success
                        mutableData.setValue(p);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                    }
                });
            }
        });
        holder.description_v.setText(u.description);
    }

    public void removeListener(){
        if(allPostsRef !=null && usersRefListener!=null)
            allPostsRef.removeEventListener(usersRefListener);
    }
    @Override
    public int getItemCount() {
        return postsList.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView fname_v;
        public TextView email_v;
        public TextView phone_v;
        public TextView date_v;
        public TextView description_v;
        public ImageView imageView;
        public  ImageView likeBtn;
        public TextView likeCount;
        DatabaseReference uref;
        ValueEventListener urefListener;

        DatabaseReference likeCountRef;
        ValueEventListener likeCountRefListener;

        DatabaseReference likesRef;
        ValueEventListener likesRefListener;
        public ViewHolder(View v){
            super(v);
            fname_v = v.findViewById(R.id.fname_view);
            email_v = v.findViewById(R.id.email_view);
            phone_v = v.findViewById(R.id.phone_view);
            date_v = v.findViewById(R.id.date_view);
            description_v=v.findViewById(R.id.description);
            imageView=v.findViewById(R.id.postImg);
            likeBtn=v.findViewById(R.id.likeBtn);
            likeCount=v.findViewById(R.id.likeCount);
        }
    }

}
