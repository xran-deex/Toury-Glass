package twilight.of.the.devs.touryglass;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.mylibrary.Tour;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class TouryREST {

	protected static final String TAG = TouryREST.class.getName();
	private Callback callback;
	private List<Tour> tours;
	private Tour tour;
	
	public TouryREST() {
		tours = new LinkedList<Tour>();
	}
	
	public void setCallback(Callback callback){
		this.callback = callback;
	}
	
	public TouryREST(Callback callback) {
		this.callback = callback;
		tours = new LinkedList<Tour>();
	}
	
	public List<Tour> getTours(){
		return tours;
	}
	
	public Tour getTour(int id){
		return tour;
	}
	
	public CharSequence[] getToursAsCharSequence(){
		CharSequence[] result = new CharSequence[tours.size()];
		for(int i = 0; i < tours.size(); i++){
			result[i] = tours.get(i).getName();
		}
		return result;
	}
	
	public void postTours(final List<Tour> tours){

			HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            HttpResponse response = null;
            JSONArray array = new JSONArray();
            JSONObject json = new JSONObject();
            String authorizationString = "Basic " + Base64.encodeToString(
			        ("randy" + ":" + "greenday").getBytes(),
			        Base64.NO_WRAP); 
            
            for(Tour t : tours){
            try {
                HttpPost post = new HttpPost("http://valis.strangled.net:7000/api/tours/");
                                    
                post.addHeader("Authorization", authorizationString);
                post.addHeader("Content-Type", "application/json");

            	json.put("id", t.getId());
                json.put("name", t.getName());
                StringEntity se = new StringEntity( json.toString());  
                post.setEntity(se);
                response = client.execute(post);
                
//                for(Marker m : t.getMarkers()){
//                	if(!m.isSynced()){
//                    	post = new HttpPost("http://valis.strangled.net:7000/api/markers/");
//                    	JSONObject jsonParam = new JSONObject();
//	    				jsonParam.put("title", m.getTitle());
//	    				jsonParam.put("description", m.getDescription());
//	    				jsonParam.put("latitude",m.getLatitude());
//	    				jsonParam.put("longitude",m.getLongitude());
//	    				jsonParam.put("radius", m.getRadius());
//	    				jsonParam.put("direction", m.getDirection());
//	    				jsonParam.put("tour_id", m.getTourId());
//	                    se = new StringEntity( jsonParam.toString());  
//	                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//	                    post.addHeader("Authorization", authorizationString);
//	                    post.setEntity(se);
//	                    response = client.execute(post);
//                	}
//                }
                
                /*Checking response */
                if(response!=null){
                    InputStream in = response.getEntity().getContent(); //Get the data in the entity
                    BufferedReader br = new BufferedReader( new InputStreamReader(in, "UTF-8") );
                    Log.d(TAG, br.readLine());
                }

            } catch (Exception e){}
        }
	}
	
	public void postMarkers(List<Marker> markers){
		HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
        HttpResponse response = null;
        JSONObject json = new JSONObject();
        String authorizationString = "Basic " + Base64.encodeToString(
		        ("randy" + ":" + "greenday").getBytes(),
		        Base64.NO_WRAP); 
        HttpPost post;
        StringEntity se;

        try {
            Log.d(TAG, "Uploading unsynced markers");
            for(Marker m : markers){
            	Log.d(TAG, "Syncing Marker: " + m.toString());
            	if(!m.isSynced()){
                	post = new HttpPost("http://valis.strangled.net:7000/api/markers/");
                	JSONObject jsonParam = new JSONObject();
    				jsonParam.put("title", m.getTitle());
    				jsonParam.put("description", m.getDescription());
    				jsonParam.put("trigger_latitude",m.getTriggerLatitude());
    				jsonParam.put("trigger_longitude",m.getTriggerLongitude());
    				jsonParam.put("marker_latitude",m.getMarkerLatitude());
    				jsonParam.put("marker_longitude",m.getMarkerLongitude());
    				jsonParam.put("radius", m.getRadius());
    				jsonParam.put("direction", m.getDirection());
    				jsonParam.put("tour", m.getTourId());
    				jsonParam.put("order", m.getOrder());
                    se = new StringEntity( jsonParam.toString());  
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    post.addHeader("Authorization", authorizationString);
                    post.setEntity(se);
                    response = client.execute(post);
                    
                    if(response!=null){
                        InputStream in = response.getEntity().getContent(); //Get the data in the entity
                        BufferedReader br = new BufferedReader( new InputStreamReader(in, "UTF-8") );
                        Log.d(TAG, br.readLine());
                    }
            	}
            }

        } catch (Exception e){}
    }
	
	public void fetchTours(){
		new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
                HttpResponse response;

                try {
//                    HttpGet post = new HttpGet("http://valis.strangled.net:7000/api/tours/");
//                    String authorizationString = "Basic " + Base64.encodeToString(
//    				        ("randy" + ":" + "greenday").getBytes(),
//    				        Base64.NO_WRAP); 
//                                        
//                    post.addHeader("Authorization", authorizationString);
                	HttpGet post = new HttpGet("http://valis.strangled.net:7000/tours?format=json");
                    response = client.execute(post);

                    /*Checking response */
                    if(response!=null){
                        InputStream in = response.getEntity().getContent(); //Get the data in the entity
                        String res = new DataInputStream(in).readLine();
//                        JSONObject obj = new JSONObject(res);
//                        JSONArray results = obj.getJSONArray("results");
                        JSONArray results = new JSONArray(res);
                        Log.d(TAG, results.toString());

                        
                       for(int i = 1; i <= results.length(); i++){
                        	JSONObject j = results.getJSONObject(i-1);
                        	
//                        	Tour tour = new Tour(j.getInt("id"), j.getString("name"));
                        	Tour tour = new Tour(j.getInt("pk"), j.getJSONObject("fields").getString("name"));
                        	post = new HttpGet("http://valis.strangled.net:7000/tour/"+tour.getId() +"/?format=json");

                            response = client.execute(post);
                            List<Marker> markers = new LinkedList<Marker>();
                            if(response!=null){
                                in = response.getEntity().getContent(); //Get the data in the entity
                                res = new DataInputStream(in).readLine();
                                JSONArray markerArray = new JSONArray(res);
                                for(int k = 0; k < markerArray.length(); k++){
                                	JSONObject marker = markerArray.getJSONObject(k);
                                	JSONObject json = marker.getJSONObject("fields");
                                	Marker m = new Marker();
                                	m.setId(marker.getInt("pk"));
                                	m.setDescription(json.getString("description"));
//                                	m.setDirection(json.getDouble("direction"));
                                	m.setRadius(json.getDouble("radius"));
                                	m.setTriggerLatitude(json.getDouble("trigger_latitude"));
                                	m.setTriggerLongitude(json.getDouble("trigger_longitude"));
                                	m.setMarkerLatitude(json.getDouble("marker_latitude"));
                                	m.setMarkerLongitude(json.getDouble("marker_longitude"));
                                	m.setTitle(json.getString("title"));
                                	m.setTourId(json.getInt("tour"));
                                	m.setOrder(json.getInt("order"));
                                	m.setSynced(true);
                                	markers.add(m);
                                }
                            }
                            tour.setMarkers(markers);
                        	tours.add(tour);
                        }
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                }
                
                
				return null;
			}
			
			protected void onPostExecute(Void result) {
				callback.callback();
			};
    		
    	}.execute();
	}
	
}
