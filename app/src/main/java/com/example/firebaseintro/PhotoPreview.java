package com.example.firebaseintro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhotoPreview extends AppCompatActivity {
    private static final int REQUEST_FOR_LOCATION = 123;
    public static class Post {
        public String uid;
        public String url;
        public Object timestamp;
        public String description;
        public int likeCount = 0;
        public String lat, lng;
        public Map<String, Boolean> likes = new HashMap<>();

        public Post(String author, String url, String description, String lat, String Lng) {
            this.uid = author;
            this.url = url;
            this.description = description;
            this.timestamp = ServerValue.TIMESTAMP;
            this.lat = lat;
            this.lng = Lng;
        }

        public Object getTimestamp() {
            return timestamp;
        }

        public Post() {

        }
    }
    Uri uri;
    EditText description;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient mFusedLocationClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preview);
        String l = getIntent().getStringExtra("uri");
        uri= Uri.fromFile(new File(l));
        ImageView imageView=findViewById(R.id.previewImage);
        imageView.setImageURI(uri);
        description=findViewById(R.id.description);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FOR_LOCATION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FOR_LOCATION && ((grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) || (grantResults.length > 1 && grantResults[1] != PackageManager.PERMISSION_GRANTED))) {
            Toast.makeText(this, "We need to access your location", Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadImage(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FOR_LOCATION);
            return;
        }
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/geo_loc");
        final GeoFire geoFire = new GeoFire(ref);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                final String fileNameInStorage = UUID.randomUUID().toString();
                String path = "images/" + fileNameInStorage + ".jpg";
                final StorageReference imageRef = storage.getReference(path);
                final String lat = String.valueOf(location.getLatitude());
                final String lng = String.valueOf(location.getLongitude());
                imageRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override public void onSuccess(final Uri uri) {
                                final FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference postsRef = database.getReference("ImagePosts");
                                final DatabaseReference newPostRef = postsRef.push();
                                newPostRef.setValue(new Post(currentUser.getUid(),uri.toString(),description.getText().toString(),lat,lng))
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override public void onSuccess(Void aVoid) {
                                                geoFire.setLocation(newPostRef.getKey(), new GeoLocation(Double.parseDouble(lat),Double.parseDouble(lng)));
                                                Toast.makeText(PhotoPreview.this, "Success", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                Toast.makeText(PhotoPreview.this, "Upload completed. Your image will appear shortly.", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override public void onFailure(@NonNull Exception e) {
                                Toast.makeText(PhotoPreview.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(PhotoPreview.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PhotoPreview.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }
    public void Publish(View view){
        uploadImage();
        finish();
    }
}