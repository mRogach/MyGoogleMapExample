package com.example.user.mygooglemapexample;

import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG  = MainActivity.class.getSimpleName();

    private GoogleMap mMap;
    private Marker mLondonMarker, mMyMarker;
    private LocationManager locationManager;
    private Location myLocation;
    LatLng origin,latLng2;
    private String url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.v(TAG, Keys.getCertificateSHA1Fingerprint(MainActivity.this));
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        MapListeners listeners = new MapListeners();
        mMap.setOnMapLongClickListener(listeners);
        mMap.setOnMarkerDragListener(listeners);

    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gm_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                return true;
            case R.id.my_location:
                myLocation(myLocation);
                return true;
            case R.id.draw_line:
                getDirections(url);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void myLocation(Location location) {
        if (mLondonMarker == null) {
            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(myLocation);
            markerOptions.draggable(false);
            markerOptions.title(getString(R.string.london));
            markerOptions.snippet(getString(R.string.london_snippet));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(139.0f));
            mLondonMarker = mMap.addMarker(markerOptions);
            mLondonMarker.showInfoWindow();

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation, 17);
            mMap.animateCamera(cameraUpdate);
        }
        else {
            if (mLondonMarker.isVisible()) {
                mLondonMarker.remove();
                mLondonMarker = null;
            }
        }

    }

    private class MapListeners implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener {

        @Override
        public void onMapLongClick(LatLng latLng) {
            if (mMyMarker != null) {
                mMyMarker.remove();
            }

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            latLng2 = latLng;
            markerOptions.draggable(true);
            markerOptions.title("THIS IS MY MARKER!");
            markerOptions.snippet("I can do what I want");
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pins9));
            mMyMarker = mMap.addMarker(markerOptions);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate);

            String str = getDirectionsUrl(origin, latLng2);
            url = str;
           // getDirections(url);
        }

        @Override
        public void onMarkerDragStart(Marker marker) { }

        @Override
        public void onMarkerDrag(Marker marker) { }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            mMap.clear();
            myLocation(myLocation);
            LatLng latLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate);

            String url = getDirectionsUrl(origin, latLng);
            getDirections(url);
        }
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            myLocation(location);
            myLocation = location;
            origin = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());

        }

        @Override
        public void onProviderDisabled(String provider) {
            myLocation(locationManager.getLastKnownLocation(provider));
         }

        @Override
        public void onProviderEnabled(String provider) {
            myLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    private String getDirectionsUrl(LatLng origin,LatLng dest){
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin+"&"+str_dest+"&"+sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();
            br.close();
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }
            mMap.addPolyline(lineOptions);
        }
    }
    public void getDirections(String url){
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }
}