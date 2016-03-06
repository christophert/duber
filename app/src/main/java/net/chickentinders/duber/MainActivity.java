package net.chickentinders.duber;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;
import android.Manifest;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            TextView addressLabel = (TextView) findViewById(R.id.addressLabel);
            addressLabel.setText(resultData.getString(Constants.RESULT_DATA_KEY));
        }
    }
    private static final int REQUEST_FINE=0;
    private LocationRequest mLocReq;
    private GoogleApiClient mGoogApiClient;

    LatLng latLng;
    GoogleMap map;
    SupportMapFragment mFrag;
    Marker mCurrLoc;
    Location mLastLoc;
    private AddressResultReceiver mResultReceiver;

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DAT_EXTRA, mLastLoc);
        startService(intent);
    }


    private void loadPermissions(String perm, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, requestCode);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultReceiver = new AddressResultReceiver(new Handler());

        loadPermissions(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_FINE);

        mFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mFrag.getMapAsync(this);

        Button someButton = (Button) findViewById(R.id.someButton);
        someButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(v.getContext(), "There are no drivers in your area", Toast.LENGTH_SHORT).show();
                int PLACE_PICKER_REQUEST=1;
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
//                builder.setLatLngBounds(new LatLngBounds(new LatLng(42.823329, -78.066937), new LatLng(43.262418, -77.379604)));
                try {
                    startActivityForResult(builder.build(MainActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                if(((int) Math.floor(Math.random() * 100))%2 == 0) {
                    Toast.makeText(this, "There are no drivers in your area", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "This service is only available in Kazakhstan", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap nmap) {
        map = nmap;

        setUpMap();
    }

    public void setUpMap() throws SecurityException {
        map.setMyLocationEnabled(true);
        buildGoogApiClient();
        mGoogApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mGoogApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogApiClient, this);
        }
    }

    protected synchronized void buildGoogApiClient() {
        Toast.makeText(this, "build", Toast.LENGTH_SHORT).show();
        mGoogApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }

    @Override
    public void onConnected(Bundle bundle) throws SecurityException {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogApiClient);
        if(mLastLocation != null) {
            mLastLoc = mLastLocation;
            startIntentService();
            //place
            map.clear();
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            mCurrLoc = map.addMarker(markerOptions);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(17)
                    .bearing(90)
                    .tilt(40)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        mLocReq = new LocationRequest();
        mLocReq.setInterval(5000);
        mLocReq.setFastestInterval(3000);
        mLocReq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogApiClient, mLocReq, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //stuff
    }

    @Override
    public void onConnectionFailed(ConnectionResult connRes) {
        //stuff
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mCurrLoc != null) {
            mCurrLoc.remove();
        }
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLoc = map.addMarker(markerOptions);

        Toast.makeText(this, "Location changed", Toast.LENGTH_SHORT).show();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch(status) {
            case LocationProvider.OUT_OF_SERVICE:
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                break;
            default:
                break;
        }
    }

    public void onProviderEnabled(String str) {}

    public void onProviderDisabled(String str) {}
}
