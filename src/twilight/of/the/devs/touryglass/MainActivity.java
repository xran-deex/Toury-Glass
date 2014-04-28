package twilight.of.the.devs.touryglass;

import java.util.LinkedList;
import java.util.List;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.mylibrary.Tour;
import twilight.of.the.devs.touryglass.TouryService.LocalBinder;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.ToursTableMetaData;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/*
 * This class is responsible for displaying the menu for the app.
 */
public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getName();

	protected TouryService mTouryService;
	private static final int SPEECH_REQUEST = 0;
	private Handler mHandler = new Handler();
	
	//List of tours loaded from the database
	private LinkedList<Tour> tours;
	private boolean mAttachedToWindow;
	private boolean mOptionsMenuOpen;
	
	//Used to determine whether to destroy this Activity
	private boolean shouldFinish;
	
	private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof TouryService.LocalBinder) {
                LocalBinder binder = (TouryService.LocalBinder) service;
                mTouryService = binder.getService();
                openOptionsMenu();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tours = new LinkedList<Tour>();
		bindService(new Intent(this, TouryService.class), mConnection, 0);
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "In Stop");
		super.onStop();
	}
	
	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mAttachedToWindow = false;
	}
	
	@Override
	public void openOptionsMenu() {
		if(!mOptionsMenuOpen && mAttachedToWindow && mTouryService != null) {
			Log.d(TAG, "Opening options menu...");
			super.openOptionsMenu();
			
		}
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAttachedToWindow = true;
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		if(!mTouryService.showTourList()){
			getMenuInflater().inflate(R.menu.main, menu);
			MenuItem tourListMenu = menu.getItem(0);
			Menu subMenu = tourListMenu.getSubMenu();
			subMenu.clear();
			Cursor c = getContentResolver().query(Uri.withAppendedPath(ToursTableMetaData.CONTENT_URI, "tour"),
					null, 
					null, 
					null, 
					TouryProviderMetaData.ToursTableMetaData.DEFAULT_SORT_ORDER);
			
			/*
			 * Adds the synced tours from the database as a submenu.
			 */
			while(c.moveToNext()){
				Tour t = new Tour(c.getInt(c.getColumnIndex(ToursTableMetaData._ID)), c.getString(c.getColumnIndex(ToursTableMetaData.NAME)));
				tours.add(t);
				Log.d(TAG, t.getName());
				subMenu.add(1, t.getId(), 0, t.getName());
			}
		}
		shouldFinish = true;
		return true;
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		mOptionsMenuOpen = false;
		unbindService(mConnection);
		if(shouldFinish){
			Log.d(TAG, "Finishing");
			finish();	
		}
	}
	
	/*
	 * Opens the voice recognizer for voice recognition.
	 */
	private void displaySpeechRecognizer() {
		shouldFinish = false;
        Intent searchIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(searchIntent, SPEECH_REQUEST);
        Log.d(TAG, "Speech started...");
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		Log.d(TAG, "In activity result");
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
        	/*
        	 * Grab the speech results and pass them to the search function.
        	 */
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            search(results);
        } 
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
	
	@Override
	public void onBackPressed() {
		shouldFinish = true;
		super.onBackPressed();
	}
	
	/*
	 * Search for a marker.
	 * Sets the marker id in the TouryView if found.
	 */
	public void search(List<String> results){
		for(String word : results){
			for(Marker marker : mTouryService.getCurrentMarkers()){
				if(word.toLowerCase().contains(marker.getTitle().toLowerCase())){
					mTouryService.getRenderer().getTouryView().setSearchResultId(marker.getId());
					Log.d(TAG, marker.getDescription());
					return;
				}
			}
		}
		mTouryService.getRenderer().getTouryView().setSearchResultId(null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		shouldFinish = true;
		
		/*
		 * Only load a tour if the preference is set.
		 */
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoload", false))
			for(Tour t : tours){
				if(t.getId() == item.getItemId()){
					mTouryService.loadTour(t.getId());
					PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("last_tour", t.getId()).commit();
					return true;
				}
			}
		switch(item.getItemId()){
		case R.id.stop:
			mHandler.post(new Runnable(){
				@Override
				public void run() {
					stopService(new Intent(MainActivity.this, TouryService.class));
				}
			});
			
			return true;
		case R.id.sync:
			Intent syncIntent = new Intent(this, SyncService.class);
			startService(syncIntent);
			return true;
//		case R.id.debug:
//			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("debug", false))
//				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("debug", false).commit();
//			else
//				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("debug", true).commit();
//			return true;
		case R.id.distances:
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_distances", false))
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("show_distances", false).commit();
			else
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("show_distances", true).commit();
			return true;
		case R.id.search:
			displaySpeechRecognizer();
			return true;
		case R.id.ordered:
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ordered", false)) {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("ordered", false).commit();
				mTouryService.getTTS().speak("Guided tour has been turned off. All markers will show up in your glass.", TextToSpeech.QUEUE_FLUSH, null);
			}
			else {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("ordered", true).commit();
				mTouryService.getTTS().speak("A guided tour has been initiated. Only the next marker will show up in your glass.", TextToSpeech.QUEUE_FLUSH, null);
			}
			Log.d(TAG, PreferenceManager.getDefaultSharedPreferences(this).getAll().toString());
			return true;
		case R.id.autoload:
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoload", false))
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("autoload", false).commit();
			else {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("autoload", true).commit();
				PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("last_tour", -1).commit();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
