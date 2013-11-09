package org.assessment.mapsapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.ObjectInputStream;
import java.io.InputStreamReader;
import java.net.URL; 
import java.net.HttpURLConnection; 

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.OverlayItem;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.util.Log;

public class MainActivity extends Activity {

   private static final GeoPoint BERLIN = new GeoPoint(52.51, 13.40);
   private static final double SEARCH_RADIUS_KM = 2;
   private static final int CONECTION_TIMEOUT   = 10000;
   private static final String URL_CAR_SERVICE  = 
      "https://www.drive-now.com/php/metropolis/json.vehicle_filter?cit=6099";
   private final String DEBUG_TAG = this.getClass().
      getSimpleName();
   private static GeoPoint newLocation = null;

   private IMapController controller;
   private MapView map;
   private EditText edtLatitude;
   private EditText edtLongitude;

   private static class Car {

      public GeoPoint position;
      public String name;
      public String address;

      public Car(GeoPoint position, String name, String address) {
         this.position = position;
         this.name = name;
         this.address = address;
      }
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      edtLatitude  = (EditText)findViewById(R.id.latitude);
      edtLongitude = (EditText)findViewById(R.id.longitude);

      initLocation();
   }

   public void goClick(View view) {
      boolean validation = true;
      String latitude  = edtLatitude.getText().toString().trim();
      String longitude = edtLongitude.getText().toString().trim();

      // Validate inputs
      if(latitude == null || latitude.equals("")) {
         validation = false;
      }

      if(longitude == null || longitude.equals("")) {
         validation = false;
      }

      if(validation) {
         newLocation = new GeoPoint(
               Double.parseDouble(latitude),Double.parseDouble(longitude));
         updateLocation(newLocation);
      } else {
         Toast.makeText(this,getResources().getString(R.string.btn_txt_go),
               Toast.LENGTH_SHORT).show();
      }
   }

   private void initLocation() {
      this.map = (MapView) this.findViewById(R.id.mapview);
      this.map.setBuiltInZoomControls(true);
      this.controller = map.getController();

      newLocation = BERLIN;
      updateLocation(newLocation);
   }

   private void updateLocation(GeoPoint location){
      this.controller.setZoom(12);
      this.controller.setCenter(location);

      // Map is updated in AsyncTask
      new CarsAsyncTask().execute(URL_CAR_SERVICE);
   }

   // AsyncTask gets the car list
   private class CarsAsyncTask extends AsyncTask<String, Void, List<Car>> {
       private ProgressDialog mDialog;

       public CarsAsyncTask() {
          mDialog = new ProgressDialog(MainActivity.this);
       }

       @Override
       protected void onPreExecute() {
          super.onPreExecute();

          this.mDialog.setMessage("Please wait...");
          this.mDialog.show();
       }

      @Override
      protected List<Car> doInBackground(String... values) {
         try {
            // getCars
            return loadCarsFromNetwork(values[0]);
         } catch (IOException e) {
            Log.e(DEBUG_TAG,"Error: " + e);
            return new ArrayList<Car>();
         } catch(JSONException e) {
            Log.e(DEBUG_TAG,"Error: " + e);
            return new ArrayList<Car>();
         }
      }

      @Override
       protected void onPostExecute(List<Car> lstCars) {
          super.onPostExecute(lstCars);
          mDialog.dismiss();

          // Get cars in radius and display them on the map
          List<Car> carsInCloseRadius = filterCarsByDistance(lstCars, 
                newLocation, SEARCH_RADIUS_KM);
          showCarsOnMap(carsInCloseRadius);
       }

      private List<Car> loadCarsFromNetwork(String urlString) 
         throws IOException,JSONException {
         String jsonResult = null;
         List<Car> lstCars = null;

         jsonResult = downloadUrl(urlString);
         if(jsonResult != null) {
            Log.d(DEBUG_TAG,jsonResult);
            lstCars = parseJson(jsonResult);
         }

         return lstCars;
      }

      // Gets JSON response from server through HTTP connection
      private String downloadUrl(String urlString) 
         throws IOException {
         String jsonResult = null;
         String line       = null;
         StringBuilder sb  = new StringBuilder(); 

         URL serverAddress             = new URL(urlString);
         HttpURLConnection connection  = (HttpURLConnection)serverAddress
            .openConnection();
         connection.setConnectTimeout(CONECTION_TIMEOUT);
         connection.connect();

         BufferedReader br = new BufferedReader(
               new InputStreamReader(connection.getInputStream()));
         while((line = br.readLine()) != null) {
            sb.append(line);
         }
         br.close();
         jsonResult = sb.toString();

         return jsonResult;
      }

      // Parse JSON result and get list of cars
      private List<Car> parseJson(String jsonResult) throws JSONException {
         List<Car> lstCars = new ArrayList<Car>();

         JSONObject root = new JSONObject(jsonResult);
         if(root.has("rec")) {
            JSONObject recObj = root.getJSONObject("rec");
            JSONObject vehiclesObj = recObj.getJSONObject("vehicles");
            JSONArray vehiclesArray = vehiclesObj.getJSONArray("vehicles");
            for(int i = 0;i < vehiclesArray.length();i++) {
               JSONObject vehicleObj = vehiclesArray.getJSONObject(i);
               JSONObject positionObj = vehicleObj.getJSONObject("position");

               double latitude = positionObj.getDouble("latitude");
               double longitude = positionObj.getDouble("longitude");
               GeoPoint coords = new GeoPoint(latitude, longitude);

               lstCars.add(new Car(coords,vehicleObj.getString("carName"),
                        positionObj.getString("address")));
            }
         }
         return lstCars;
      }
   }


   public List<Car> filterCarsByDistance(List<Car> cars, GeoPoint location, double searchRadiusKm) {

      //  ###################################### TO IMPLEMENT  ###########################################

      // Filter the list of cars by a location within the specified radius

      Log.d(DEBUG_TAG,"getCars: "+cars.size());

      List<Car> lstFiltered = new ArrayList<Car>();
      for(Car cr:cars) {
         int distance = cr.position.distanceTo(location);
         // If distance between car and center location is inside radius
         if(distance / 1000 <= SEARCH_RADIUS_KM) {
            lstFiltered.add(cr);
         }
      }

      return lstFiltered;
   }

   // Display cars in map
   public void showCarsOnMap(List<Car> cars) {
      List<OverlayItem> items = new ArrayList<OverlayItem>();

      for (Car car : cars){
         OverlayItem olItem = new OverlayItem(car.name, car.address, car.position); 
         items.add(olItem);
      }

      OnItemGestureListener<OverlayItem> listener = new OnItemGestureListener<OverlayItem>() {
         @Override
         public boolean onItemLongPress(int arg0, OverlayItem item) {
            Toast toast = Toast.makeText(getApplicationContext(), item.getTitle() + ", " + item.getSnippet(), Toast.LENGTH_LONG);
            toast.show();
            return false;
         }
         @Override
         public boolean onItemSingleTapUp(int arg0, OverlayItem item) {
            Toast toast = Toast.makeText(getApplicationContext(), item.getTitle() + ", " + item.getSnippet(), Toast.LENGTH_LONG);
            toast.show();
            return false;
         }};
      ItemizedIconOverlay<OverlayItem> overlay = new ItemizedIconOverlay<OverlayItem>(getApplicationContext(), items, listener);


      this.map.getOverlays().clear();
      this.map.getOverlays().add(overlay);
      this.map.invalidate();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);

      return true;
   }

}
