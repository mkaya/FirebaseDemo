package com.example.firebaseintro;

import static com.example.firebaseintro.UserHome.TAG;

import android.net.Uri;
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

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter< RecyclerViewAdapter.ViewHolder> {
    private final FirebaseUser currentUser;
    private final List<String> keyList;
    private final HashMap<String,PostModel> key_to_Post;
    private Marker currentMarker =null;
    private  ItemClickListener itemClickListener;

    public RecyclerViewAdapter(HashMap<String,PostModel> kp, List<String> kl, ItemClickListener _itemClickListener){
        keyList=kl;
        itemClickListener =_itemClickListener;
        key_to_Post= kp;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
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
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final FirebaseFirestore firestore_db = FirebaseFirestore.getInstance();
        final PostModel u =key_to_Post.get(keyList.get(position));
        DocumentReference image_post_ref = firestore_db.collection("ImagePosts").document(u.postKey);
        String uid = u.uid;
        holder.uref = database.getReference("Users").child(uid);
        holder.uref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.fname_v.setText("First Name: " +dataSnapshot.child("displayname").getValue().toString());
                holder.email_v.setText("Email:  " + dataSnapshot.child("email").getValue().toString());
                holder.phone_v.setText("Phone Num:  " + dataSnapshot.child("phone").getValue().toString());
                holder.date_v.setText("Date Created: "+u.date);
                if (dataSnapshot.child("profilePicture").exists()) {
                    Picasso.get().load(dataSnapshot.child("profilePicture").getValue().toString())
                            .transform(new CircleTransform()).into(holder.profileImage);
                    holder.profileImage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        image_post_ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                PhotoPreview.Post post = snapshot.toObject(PhotoPreview.Post.class);

                if (post != null) {
                    StorageReference pathReference = FirebaseStorage.getInstance().getReference("images/"+u.url);
                    pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).into(holder.imageView);
                        }
                    });
                    holder.likeCount.setText(String.format("%d Likes", post.likeCount));
                    if(post.likes.getOrDefault(currentUser.getUid(), false))
                    {
                        holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.getContext(), R.drawable.like_active));
                    }
                    else{
                        holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.getContext(), R.drawable.like_disabled));
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firestore_db.runTransaction(new com.google.firebase.firestore.Transaction.Function<Void>() {
                    @Nullable
                    @Override
                    public Void apply(@NonNull com.google.firebase.firestore.Transaction transaction) throws FirebaseFirestoreException {
                        DocumentSnapshot postSnapshot = transaction.get(image_post_ref);
                        PhotoPreview.Post post = postSnapshot.toObject(PhotoPreview.Post.class);
                        if (post.likes.containsKey(currentUser.getUid())) {
                            // Unstar the post and remove self from stars
                            post.likeCount = post.likeCount - 1;
                            post.likes.remove(currentUser.getUid());
                        } else {
                            // Star the post and add self to stars
                            post.likeCount = post.likeCount + 1;
                            post.likes.put(currentUser.getUid(), true);
                        }
                        transaction.update(image_post_ref, "likeCount", post.likeCount);
                        transaction.update(image_post_ref, "likes", post.likes);
                        return null;
                    }
                });
            }
        });
        holder.description_v.setText(u.description);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMarker!=null)
                    currentMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_grey));

                u.m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_red));
                currentMarker=u.m;
                if (itemClickListener!=null)
                    itemClickListener.onItmeClick(currentMarker.getPosition());
            }
        });
    }
    @Override
    public int getItemCount() {
        return keyList.size();
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
        public ImageView profileImage;

        public ViewHolder(View v){
            super(v);
            fname_v = v.findViewById(R.id.fname_view);
            email_v = v.findViewById(R.id.email_view);
            phone_v = v.findViewById(R.id.phone_view);
            date_v = v.findViewById(R.id.date_view);
            description_v = v.findViewById(R.id.description);
            profileImage = v.findViewById(R.id.userImage);
            imageView=v.findViewById(R.id.postImg);
            likeBtn=v.findViewById(R.id.likeBtn);
            likeCount=v.findViewById(R.id.likeCount);
        }
    }

}
