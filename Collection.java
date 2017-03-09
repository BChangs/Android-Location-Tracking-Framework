// Insert your package here : package.****;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;

public class Collection extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    Button myB;
    String uid;
    private LocationManager locationManager;
    private String latitude;
    private String longitude;
    private CountDownTimer count;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    Location lastKnownLocation;
    GoogleApiClient mGoogleApiClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Start up activity, make sure screen stays on, setup layout, and button to stop tracking
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myB = (Button) findViewById(R.id.my_b);
        myB.setText("Stop Tracking");

        // Set button response
        setListeners();

        // Load in unique identifier if exists, create if not
        // This identifier helps keep location data for each user seperated while also keeping them anonymous
        uid = "invalid";
        SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        uid = sp.getString("uid", uid);
        if (uid.equals("invalid")) {
            uid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("uid", uid);
            editor.commit();
        }

        // Setup calendar to get time and database to save data
        // Note that you will have to have previously linked your application to your own firebase database
        final Calendar c = Calendar.getInstance();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Data");
        final DatabaseReference listLoc = myRef.child(uid);
        DatabaseReference newListLoc = listLoc.push();
        newListLoc.setValue("Beginning of journey");

        // Create an instance of GoogleAPIClient to be used in location tracking
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();

        // Loop tracking to send data every 5 seconds, can change the value below to change time updates
        count = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (checkLocationPermission()) {
                    if (ContextCompat.checkSelfPermission(Collection.this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (lastKnownLocation != null) {
                            latitude = String.valueOf(lastKnownLocation.getLatitude());
                            longitude = String.valueOf(lastKnownLocation.getLongitude());
                        }
                    }
                }
                String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                DatabaseReference newListLoc = listLoc.push();
                newListLoc.setValue(currentDateTimeString + ": " + latitude + ", " + longitude);
                this.start();
            }
        };

        count.start();
    }

    private void setListeners() {
        // End tracking when button is pressed
        myB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count.cancel();
                final FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("Data");
                final DatabaseReference listLoc = myRef.child(uid);
                DatabaseReference newListLoc = listLoc.push();
                newListLoc.setValue("End of journey");
                /*Optionally have it switch to new activity
                setContentView(****);
                Intent intent = new Intent(Collection.this, ****);
                startActivity(intent);
                finish();*/
            }
        });
    }

    // Request location permission if needed
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
            return false;
        } else {
            return true;
        }
    }

    // Check permission request results and begin getting location if allowed
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    
                    // permission was granted
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        
                        // Setup Location Manager to find GPS coordinates
                        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (lastKnownLocation != null) {
                            latitude = String.valueOf(lastKnownLocation.getLatitude());
                            longitude = String.valueOf(lastKnownLocation.getLongitude());
                        }
                    }
                }
                return;
            }
        }
    }

    // When connected, setup location updates
    @Override
    public void onConnected(Bundle connectionHint) {
        LocationRequest mLocationRequest = new LocationRequest();
        // Value in milliseconds for updating location
        mLocationRequest.setInterval(2500);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                if (lastKnownLocation != null) {
                    latitude = String.valueOf(lastKnownLocation.getLatitude());
                    longitude = String.valueOf(lastKnownLocation.getLongitude());
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                
                lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                if (lastKnownLocation != null) {
                    latitude = String.valueOf(lastKnownLocation.getLatitude());
                    longitude = String.valueOf(lastKnownLocation.getLongitude());
                }
            }
        }
    }
}
