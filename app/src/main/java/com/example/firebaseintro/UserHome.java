package com.example.firebaseintro;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserHome extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private final Uri imageUri = null;
    private RecyclerViewAdapter myRecyclerAdapter;
    String currentPhotoPath;
    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher  = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onActivityResult);

    private final ActivityResultLauncher<String[]> multiplePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onActivityResult);
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        RecyclerView recyclerView=findViewById(R.id.recylcer_view);
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        layoutManager.scrollToPosition(0);
        recyclerView.setLayoutManager(layoutManager);
        myRecyclerAdapter=new RecyclerViewAdapter(recyclerView);
        recyclerView.setAdapter(myRecyclerAdapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int itemId = item.getItemId();
        if (itemId == R.id.signout) {
            mAuth.signOut();
            finish();
            return true;
        } else if (itemId == R.id.newUser) {
            createTestEntry();
            return true;
        } else if (itemId == R.id.edit_profile) {
            startActivity(new Intent(this, EditProfile.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void createTestEntry(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("Users");
        String pushKey = usersRef.push().getKey();
        usersRef.child(pushKey).setValue(new User("Test Display Name",
                "Test Email", "Test Phone"));
    }
    public void uploadNewPhoto(View view){
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            Toast.makeText(this, "This feature requires camera.", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        myRecyclerAdapter.removeListener();
    }
    private void takePhoto(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Toast.makeText(UserHome.this, "Failed to create file", Toast.LENGTH_SHORT).show();
            return;
        }
        // Continue only if the File was successfully created
        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.firebaseintro.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            cameraActivityResultLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Couldn't found a camera Activity.", Toast.LENGTH_SHORT).show();
        }
    }
    private void requestPermissions(){
        // https://developer.android.com/media/camera/camera-deprecated/photobasics
        // https://developer.android.com/training/permissions/requesting
        // Permission request logic
        /*
        List<String> permsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permsToRequest.add(READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED){
            permsToRequest.add(CAMERA);
        }
        if(!permsToRequest.isEmpty()){
            String[] perms = new String[permsToRequest.size()];
            perms = permsToRequest.toArray(perms);
            multiplePermissionLauncher.launch(perms);
        } else {
            takePhoto();
        }
        */
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            multiplePermissionLauncher.launch(new String[]{READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED});
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            multiplePermissionLauncher.launch(new String[]{READ_MEDIA_IMAGES, READ_MEDIA_VIDEO});
        } else {
            multiplePermissionLauncher.launch(new String[]{READ_EXTERNAL_STORAGE, CAMERA});
        }
        */
        // Alternatively perms can be requested using requestPermissons and overriding onRequestPermissionsResult
        // https://developer.android.com/training/permissions/requesting#principles
        // requestPermissions(new String[]{READ_EXTERNAL_STORAGE, CAMERA}, 10);
        takePhoto();
    }
    private void onActivityResult(Map<String, Boolean> isGranted) {
        Log.d("PERMISSIONS", "Launcher result: " + isGranted.toString());

        if (isGranted.containsValue(false)) {
            Toast.makeText(this, "At least one of the permissions was not granted, launching again...", Toast.LENGTH_SHORT).show();
            return;
        }
        takePhoto();
    }
    private void onActivityResult(ActivityResult result) {
        if (result.getData() == null){
            Toast.makeText(this, "Couldn't capture the image. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PhotoPreview.class);
        intent.putExtra("uri", currentPhotoPath);
        startActivity(intent);
    }
}