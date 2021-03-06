package com.kawcher.mygrocery.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kawcher.mygrocery.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ProductEditSellerActivity extends AppCompatActivity implements LocationListener {

    private ImageView profileIV;
    private EditText nameET,phoneET,shopNameET,deliveryFeeET,countryET,stateET,cityET,addressET;

    private SwitchCompat shopOpenSwitch;

    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;

    //Image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    //permission arrays

    private String[] locationPermissions;
    private String[] cameraPermissions;
    private String[] storagePermissions;


    //image picked uri
    private Uri image_uri;

    private double latitude=0.0, longLatitude=0.0;

    //progress dialog

    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit_seller);


        ImageButton backBtn = findViewById(R.id.backBtn);
        ImageButton gpsBtn = findViewById(R.id.gpsBtn);
        profileIV=findViewById(R.id.profileIV);
        nameET=findViewById(R.id.nameET);
        phoneET=findViewById(R.id.phoneET);
        shopNameET=findViewById(R.id.shopNameET);
        deliveryFeeET=findViewById(R.id.deliveryFeeET);
        countryET=findViewById(R.id.countryET);
        stateET=findViewById(R.id.stateET);
        cityET=findViewById(R.id.cityET);
        addressET=findViewById(R.id.addressET);
        shopOpenSwitch=findViewById(R.id.shopOpenSwitch);

        Button updateBtn = findViewById(R.id.updateBtn);

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth=FirebaseAuth.getInstance();

        checkUser();

        //ini6
        locationPermissions=new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        cameraPermissions=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                inputData();

            }
        });

        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(ProductEditSellerActivity.this, "clicked", Toast.LENGTH_LONG).show();
                Log.e("TAG", "onClick: " );
                //detect location

                if(!checkLocationPermisson()){
                    detectLocation();
                } else {

                    requestLocationPermission();
                }
            }
        });
    }



    private String name,shopName,phoneNumber,deliveryFee,country,state,city,address;
    boolean shopOpen;

    private void inputData() {

        name=nameET.getText().toString().trim();
        shopName=shopNameET.getText().toString().trim();
        shopOpen=shopOpenSwitch.isChecked();
        phoneNumber=phoneET.getText().toString().trim();
        deliveryFee=deliveryFeeET.getText().toString().trim();
        country=countryET.getText().toString().trim();
        state=stateET.getText().toString().trim();
        city=cityET.getText().toString().trim();
        address=addressET.getText().toString().trim();

        if(TextUtils.isEmpty(name)){

            Toast.makeText(this,"Enter Name....",Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(shopName)){

            Toast.makeText(this,"Enter Shop Name....",Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(phoneNumber)){

            Toast.makeText(this,"Enter Phone Number....",Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(deliveryFee)){

            Toast.makeText(this,"Enter Delivery Fee...",Toast.LENGTH_SHORT).show();
            return;
        }
        if(latitude==0.0 || longLatitude==0.0){

            Toast.makeText(this,"pls click gps button to detect location..",Toast.LENGTH_SHORT).show();
            return;
        }

        updateProfile();
    }

    private void updateProfile() {

        progressDialog.setMessage("Updating profile....");
        progressDialog.show();

        if(image_uri==null){

            //update without image
            //set up data to update

            HashMap<String,Object>hashMap=new HashMap<>();

            hashMap.put("name",""+name);
            hashMap.put("shopName",""+shopName);
            hashMap.put("phone",""+phoneNumber);
            hashMap.put("deliveryFee",""+deliveryFee);
            hashMap.put("country",""+country);
            hashMap.put("state",""+state);
            hashMap.put("address",""+address);
            hashMap.put("city",""+city);
            hashMap.put("latitude",""+latitude);
            hashMap.put("longitude",""+longLatitude);
            hashMap.put("shopOpen",""+shopOpen);

            DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference("Users");

            databaseReference.child(firebaseAuth.getUid()).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            //updated
                            
                            progressDialog.dismiss();

                            Toast.makeText(ProductEditSellerActivity.this, "profile updated..", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed to update
                            progressDialog.dismiss();

                            Toast.makeText(ProductEditSellerActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });



        } else {

            //update with image

            String filePathAndName="profile_images/"+""+firebaseAuth.getUid();
            //get storage reference

            StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathAndName);

            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            //image uploaded , get url of  uploading  image

                            Task<Uri>uriTask=taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());

                            Uri downloadImageUri=uriTask.getResult();

                            if(uriTask.isSuccessful()){

                                //update without image
                                //set up data to update

                                HashMap<String,Object>hashMap=new HashMap<>();

                                hashMap.put("name",""+name);
                                hashMap.put("shopName",""+shopName);
                                hashMap.put("phone",""+phoneNumber);
                                hashMap.put("deliveryFee",""+deliveryFee);
                                hashMap.put("country",""+country);
                                hashMap.put("state",""+state);
                                hashMap.put("address",""+address);
                                hashMap.put("city",""+city);
                                hashMap.put("latitude",""+latitude);
                                hashMap.put("longitude",""+longLatitude);
                                hashMap.put("shopOpen",""+shopOpen);

                                hashMap.put("profileImage",""+downloadImageUri);

                                DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference("Users");

                                databaseReference.child(firebaseAuth.getUid()).updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                //updated

                                                progressDialog.dismiss();

                                                Toast.makeText(ProductEditSellerActivity.this, "profile updated..", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //failed to update
                                                progressDialog.dismiss();

                                                Toast.makeText(ProductEditSellerActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });

                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            progressDialog.dismiss();
                            Toast.makeText(ProductEditSellerActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    private void checkUser() {

        FirebaseUser firebaseUser=firebaseAuth.getCurrentUser();

        if(firebaseUser==null){

            startActivity(new Intent(getApplicationContext(),LoginActivity.class));
            finish();
        } else {

            loadMyInfo();
        }
    }

    private void loadMyInfo() {

        //load user info and set to  views

        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for(DataSnapshot ds: snapshot.getChildren()){

                            String accountType=""+ds.child("accountType").getValue();
                            String address = ""+ds.child("address").getValue();
                            String city=""+ds.child("city").getValue();
                            String state=""+ds.child("state").getValue();
                            String country=""+ds.child("country").getValue();
                            String deliveryFee=""+ds.child("deliveryFee").getValue();
                            String email=""+ds.child("email").getValue();
                            latitude = Double.parseDouble(""+ds.child("latitude").getValue());
                            longLatitude= Double.parseDouble(""+ds.child("longitude").getValue());
                            String name=""+ds.child("name").getValue();
                            String online=""+ds.child("online").getValue();
                            String phone=""+ds.child("phone").getValue();
                            String profileImage=""+ds.child("profileImage").getValue();
                            String timestamp=""+ds.child("timestamp").getValue();
                            String shopName=""+ds.child("shopName").getValue();
                            String shopOpen=""+ds.child("shopOpen").getValue();
                            String uid=""+ds.child("uid").getValue();

                            nameET.setText(name);
                            phoneET.setText(phone);
                            stateET.setText(state);
                            cityET.setText(city);
                            countryET.setText(country);
                            addressET.setText(address);
                            shopNameET.setText(shopName);
                            deliveryFeeET.setText(deliveryFee);

                            if(!shopOpen.equals("true")){

                                shopOpenSwitch.setChecked(true);
                            } else {

                                shopOpenSwitch.setChecked(false);
                            }

                            try {
                                Picasso.get().load(profileImage).placeholder(R.drawable.ic_store_gray).into(profileIV);
                            } catch (Exception e){

                                profileIV.setImageResource(R.drawable.ic_person);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void showImagePickDialog() {

        //options to display in dialog

        String[] options = {"camera", "gallery"};
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //hadle click

                        if (which == 0) {
                            //camera clicked
                            if (checkCameraPermission()) {

                                //camera permission allowed
                                pickFromCamera();
                            } else {
                                requestCameraPermission();
                            }
                        } else {

                            //gallery clicked

                            if (checkStoragePermission()) {

                                //storage permission allowed

                                pickFromGallery();

                            } else {

                                //now  allowed request

                                requestStoragePermission();

                            }
                        }
                    }
                }).show();
    }

    private void pickFromGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);

    }

    private void detectLocation() {

        Toast.makeText(this, "Please wait....", Toast.LENGTH_LONG).show();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }



    private void findAddress() {

        //find address country state city

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {

            addresses = geocoder.getFromLocation(latitude, longLatitude, 1);

            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();

            //set Address
            countryET.setText(country);
            stateET.setText(state);
            cityET.setText(city);
            addressET.setText(address);

        } catch (Exception e) {


            Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkLocationPermisson() {

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                (PackageManager.PERMISSION_GRANTED);

        return result;
    }


    @Override
    public void onLocationChanged(Location location) {

        //location detected

        latitude = location.getLatitude();
        longLatitude = location.getLongitude();

        findAddress();

    }


    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission() {

        ActivityCompat.requestPermissions
                (this, storagePermissions,
                        STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {

        boolean result = ContextCompat.
                checkSelfPermission(this,
                        Manifest.permission.CAMERA) ==
                (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission() {

        ActivityCompat.requestPermissions
                (this, cameraPermissions,
                        CAMERA_REQUEST_CODE);
    }



    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

        //gps location disabled

        Toast.makeText(this, "Please turn on loction...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {

            case LOCATION_REQUEST_CODE: {

                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted) {
                        //permisson allow

                        detectLocation();
                    } else {

                        //permission denied
                        Toast.makeText(this, "Location permission is necessary....", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            break;

            case CAMERA_REQUEST_CODE: {

                if (grantResults.length > 0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        //permisson allow


                        ;

                    } else {

                        //permission denied
                        Toast.makeText(this, "Camera permission are necessary....", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            break;

            case STORAGE_REQUEST_CODE: {

                if (grantResults.length > 0) {

                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        //permisson allow

                        pickFromGallery();

                    } else {

                        //permission denied
                        Toast.makeText(this, "storage permission are necessary....", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_GALLERY_CODE) {

                //get picked image
                image_uri = data.getData();
                profileIV.setImageURI(image_uri);

            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {

                //set to image view

                profileIV.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
