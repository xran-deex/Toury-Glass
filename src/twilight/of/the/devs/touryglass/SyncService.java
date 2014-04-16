package twilight.of.the.devs.touryglass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.mylibrary.Tour;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.MarkersTableMetaData;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.ToursTableMetaData;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.style.LineHeightSpan.WithDensity;
import android.util.Log;

public class SyncService extends IntentService {

	protected static final String TAG = SyncService.class.getName();

	public SyncService() {
		super("SyncService");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		Log.d(TAG, "Starting sync service...");
		ContentResolver contentResolver = getBaseContext().getContentResolver();
		
		Uri uri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour"); 
        Cursor c = contentResolver.query(uri, null, ToursTableMetaData.UNSYNCED + "=?", new String[]{"1"}, null);
        List<Tour> tours = new LinkedList<Tour>();
        while(c.moveToNext()){
        	Tour t = new Tour(c.getInt(c.getColumnIndex(ToursTableMetaData._ID)), c.getString(c.getColumnIndex(ToursTableMetaData.NAME)));
        	tours.add(t);
        }

        List<Marker> markers = new LinkedList<Marker>();
    	Uri markersuri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "markers");
    	Cursor c2 = contentResolver.query(markersuri, null, MarkersTableMetaData.UNSYNCED + "=?", new String[]{"1"}, null);
    	while(c2.moveToNext()){
    		Marker m = new Marker();
    		m.setId(c2.getInt(c.getColumnIndex(MarkersTableMetaData._ID)));
    		m.setDescription(c2.getString(c2.getColumnIndex(MarkersTableMetaData.DESCRIPTION)));
    		m.setDirection(c2.getDouble(c2.getColumnIndex(MarkersTableMetaData.DIRECTION)));
    		m.setTriggerLongitude(c2.getDouble(c2.getColumnIndex(MarkersTableMetaData.TRIGGER_LONGITUDE)));
    		m.setTriggerLatitude(c2.getDouble(c2.getColumnIndex(MarkersTableMetaData.TRIGGER_LATITUDE)));
    		m.setMarkerLongitude(c2.getDouble(c2.getColumnIndex(MarkersTableMetaData.MARKER_LONGITUDE)));
    		m.setMarkerLatitude(c2.getDouble(c2.getColumnIndex(MarkersTableMetaData.MARKER_LATITUDE)));
    		m.setTitle(c2.getString(c2.getColumnIndex(MarkersTableMetaData.TITLE)));
    		m.setRadius(c2.getDouble(c2.getColumnIndex(MarkersTableMetaData.RADIUS)));
    		m.setTourId(c2.getInt(c2.getColumnIndex(MarkersTableMetaData.TOUR_ID)));
    		m.setOrder(c2.getInt(c2.getColumnIndex(MarkersTableMetaData.ORDER)));
    		m.setSynced(c2.getInt(c2.getColumnIndex(MarkersTableMetaData.UNSYNCED)) == 0);
    		markers.add(m);
    		Log.d(TAG, "Added marker to be uploaded: " + m.toString());
    	}
        
		final TouryREST toury = new TouryREST();
		
		//Post tours and markers to web service
		toury.postTours(tours);
		toury.postMarkers(markers);
		
		//execute sync in callback from web service fetch
		toury.setCallback(new Callback() {
			
			@Override
			public void callback() {
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... tours) {
				        final ContentResolver contentResolver = getBaseContext().getContentResolver();

				        Log.i(TAG, "Parsing tours");
				        
				        final List<Tour> tours_list = toury.getTours();
				        Log.i(TAG, "Downloading complete. Found " + tours_list.size() + " tours");

				        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

				        // Build hash table of incoming entries
				        HashMap<Integer, Tour> tourMap = new HashMap<Integer, Tour>();
				        for (Tour tour : tours_list) {
				            tourMap.put(tour.getId(), tour);
				        }

				        // Get list of all items
				        Log.i(TAG, "Fetching local entries for merge");
				        Uri uri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour"); // Get all entries
				        Cursor c = contentResolver.query(uri, null, null, null, null);
				        
				        assert c != null;
				        Log.i(TAG, "Found " + c.getCount() + " local tours. Computing merge solution...");

				        // Find stale data
				        int id;
				        String name;

				        while (c.moveToNext()) {
				            id = c.getInt(c.getColumnIndex(ToursTableMetaData._ID));
				            name = c.getString(c.getColumnIndex(ToursTableMetaData.NAME));
				            Tour match = tourMap.get(id);
				            if (match != null) {
				                // Tour exists. Remove from tour map to prevent insert later.
				                tourMap.remove(id);
				                // Check to see if the tour needs to be updated
				                Uri existingUri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour").buildUpon()
				                        .appendPath(Integer.toString(id)).build();
				                if ((match.getName() != null && !match.getName().equals(name)) ) {
				                    // Update existing record
				                    Log.i(TAG, "Scheduling update: " + existingUri);
				                    batch.add(ContentProviderOperation.newUpdate(existingUri)
				                            .withValue(ToursTableMetaData.NAME, match.getName())
				                            .build());
				                } else {
				                    Log.i(TAG, "No action: " + existingUri);
				                }
				            } else {
				                // Tour doesn't exist. Remove it and its markers from the database.
				                Uri deleteUri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour").buildUpon()
				                        .appendPath(Integer.toString(id)).build();
				                Log.i(TAG, "Scheduling delete: " + deleteUri);
				                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
				                Uri deleteUri2 = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour").buildUpon()
				                        .appendPath(Integer.toString(id)).appendPath("markers").build();
				                Log.i(TAG, "Scheduling delete: " + deleteUri2);
				                batch.add(ContentProviderOperation.newDelete(deleteUri2).build());
				            }
				        }
				        c.close();

				        // Add new items
				        for (Tour tour : tourMap.values()) {
				            Log.i(TAG, "Scheduling insert: tour_id=" + tour.getId());
				            batch.add(ContentProviderOperation.newInsert(Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour"))
				                    .withValue(ToursTableMetaData._ID, tour.getId())
				                    .withValue(ToursTableMetaData.NAME, tour.getName())
				                    .withValue(ToursTableMetaData.UNSYNCED, 0)
				                    .build());
				        }
				        Log.i(TAG, "Merge solution ready. Applying batch update");
				        try {
							contentResolver.applyBatch(TouryProviderMetaData.AUTHORITY, batch);
						} catch (RemoteException e1) {
							e1.printStackTrace();
						} catch (OperationApplicationException e1) {
							e1.printStackTrace();
						}
				        contentResolver.notifyChange(
				        		Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour"), 
				                null,                           
				                false);                        
				        
				        
				        //Now repeat the process for markers
				        List<Marker> markers = new LinkedList<Marker>();
				        for(Tour tour : tours_list){
				        	markers.addAll(tour.getMarkers());
				        }
				        
				        batch = new ArrayList<ContentProviderOperation>();

				        // Build hash table of incoming markers
				        HashMap<Integer, Marker> markerMap = new HashMap<Integer, Marker>();
				        for (Marker marker : markers) {
				            markerMap.put(marker.getId(), marker);
				        }
				        Log.i(TAG, "Found " + markerMap.size() + " markers");

				        // Get list of all items
				        Log.i(TAG, "Fetching local markers for merge");
				        uri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "markers"); // Get all markers
				        c = contentResolver.query(uri, null, null, null, null);
				        
				        assert c != null;
				        Log.i(TAG, "Found " + c.getCount() + " local markers. Computing merge solution...");

				        // Find stale data
				        //int id;
				        String title;
				        double direction;
				        String description;
				        double trigger_latitude, marker_latitude;
				        double trigger_longitude, marker_longitude;
				        double radius;
				        int tour_id, order;

				        while (c.moveToNext()) {
				            id = c.getInt(c.getColumnIndex(MarkersTableMetaData._ID));
				            title = c.getString(c.getColumnIndex(MarkersTableMetaData.TITLE));
				            description = c.getString(c.getColumnIndex(MarkersTableMetaData.DESCRIPTION));
				            direction = c.getDouble(c.getColumnIndex(MarkersTableMetaData.DIRECTION));
				            trigger_latitude = c.getDouble(c.getColumnIndex(MarkersTableMetaData.TRIGGER_LATITUDE));
				            trigger_longitude = c.getDouble(c.getColumnIndex(MarkersTableMetaData.TRIGGER_LONGITUDE));
				            marker_latitude = c.getDouble(c.getColumnIndex(MarkersTableMetaData.MARKER_LATITUDE));
				            marker_longitude = c.getDouble(c.getColumnIndex(MarkersTableMetaData.MARKER_LONGITUDE));
				            radius = c.getDouble(c.getColumnIndex(MarkersTableMetaData.RADIUS));
				            tour_id = c.getInt(c.getColumnIndex(MarkersTableMetaData.TOUR_ID));
				            order = c.getInt(c.getColumnIndex(MarkersTableMetaData.ORDER));
				            
				            Marker match = markerMap.get(id);
				            if (match != null) {
				                // Marker exists. Remove from marker map to prevent insert later.
				                markerMap.remove(id);
				                // Check to see if the marker needs to be updated
				                Uri existingUri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour").buildUpon()
				                        .appendPath(Integer.toString(tour_id)).appendPath("markers").appendPath(Integer.toString(id)).build();
				                if ((match.getTitle() != null && !match.getTitle().equals(title))||
				                		match.getDescription() != null && !match.getDescription().equals(description)||
				                		((int)match.getDirection()) != ((int)direction) ||
				                		match.getTriggerLatitude() != trigger_latitude||
				                		match.getTriggerLongitude() != trigger_longitude||
		                				match.getMarkerLatitude() != marker_latitude||
				                		match.getMarkerLongitude() != marker_longitude||
				                		match.getRadius() != radius||
				                		match.getOrder() != order ||
				                		match.getTourId() != tour_id) {
				                    // Update existing record
				                    Log.i(TAG, "Scheduling update: " + existingUri);
				                    batch.add(ContentProviderOperation.newUpdate(existingUri)
				                    		.withValue(MarkersTableMetaData.TITLE, match.getTitle())
						                    .withValue(MarkersTableMetaData.DESCRIPTION, match.getDescription())
						                    .withValue(MarkersTableMetaData.DIRECTION, match.getDirection())
						                    .withValue(MarkersTableMetaData.TRIGGER_LATITUDE, match.getTriggerLatitude())
						                    .withValue(MarkersTableMetaData.TRIGGER_LONGITUDE, match.getTriggerLongitude())
						                    .withValue(MarkersTableMetaData.MARKER_LATITUDE, match.getMarkerLatitude())
						                    .withValue(MarkersTableMetaData.MARKER_LONGITUDE, match.getMarkerLongitude())
						                    .withValue(MarkersTableMetaData.RADIUS, match.getRadius())
						                    .withValue(MarkersTableMetaData.TOUR_ID, match.getTourId())
						                    .withValue(MarkersTableMetaData.ORDER, match.getOrder())
				                            .build());
				                } else {
				                    Log.i(TAG, "No action: " + existingUri);
				                }
				            } else {
				                // Marker doesn't exist. Remove it from the database.
				                Uri deleteUri = Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour").buildUpon()
				                        .appendPath(Integer.toString(tour_id)).appendPath("markers").appendPath(Integer.toString(id)).build();
				                Log.i(TAG, "Scheduling delete: " + deleteUri);
				                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
				            }
				        }
				        c.close();

				        // Add new items
				        for (Marker e : markerMap.values()) {
				            Log.i(TAG, "Scheduling insert: marker_id=" + e.getId());
				            batch.add(ContentProviderOperation.newInsert(Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour/" + e.getTourId() + "/markers"))
				                    .withValue(MarkersTableMetaData._ID, e.getId())
				                    .withValue(MarkersTableMetaData.TITLE, e.getTitle())
				                    .withValue(MarkersTableMetaData.DESCRIPTION, e.getDescription())
				                    .withValue(MarkersTableMetaData.DIRECTION, e.getDirection())
				                    .withValue(MarkersTableMetaData.TRIGGER_LATITUDE, e.getTriggerLatitude())
				                    .withValue(MarkersTableMetaData.TRIGGER_LONGITUDE, e.getTriggerLongitude())
				                    .withValue(MarkersTableMetaData.MARKER_LATITUDE, e.getMarkerLatitude())
				                    .withValue(MarkersTableMetaData.MARKER_LONGITUDE, e.getMarkerLongitude())
				                    .withValue(MarkersTableMetaData.RADIUS, e.getRadius())
				                    .withValue(MarkersTableMetaData.TOUR_ID, e.getTourId())
				                    .withValue(MarkersTableMetaData.ORDER, e.getOrder())
				                    .withValue(MarkersTableMetaData.UNSYNCED, 0)
				                    .build());

				        }
				        Log.i(TAG, "Merge solution ready. Applying batch update");
				        try {
							contentResolver.applyBatch(TouryProviderMetaData.AUTHORITY, batch);
						} catch (RemoteException e1) {
							e1.printStackTrace();
						} catch (OperationApplicationException e1) {
							e1.printStackTrace();
						}
				        contentResolver.notifyChange(
				        		Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "markers"), 
				                null,                           
				                false);                         
				        
				        //Set all markers and tours as synced
				        ContentValues values = new ContentValues();
			            values.put(ToursTableMetaData.UNSYNCED, 0);
			            contentResolver.update(Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour"), values, MarkersTableMetaData.UNSYNCED + "=?", new String[]{"1"});
				        
				        values = new ContentValues();
				        values.put(MarkersTableMetaData.UNSYNCED, 0);
				        contentResolver.update(Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "markers"), values, ToursTableMetaData.UNSYNCED + "=?", new String[]{"1"});
				        
						return null;
					}
					
				}.execute();
			}
		});
		toury.fetchTours();
	}

}
