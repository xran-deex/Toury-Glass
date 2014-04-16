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

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getName();

	private boolean mResumed;
	protected TouryService mTouryService;
	private static final int SPEECH_REQUEST = 0;
	
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

	private LinkedList<Tour> tours;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tours = new LinkedList<Tour>();
		bindService(new Intent(this, TouryService.class), mConnection, 0);
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "In Stop");
//		if(mTouryService.showTourList()){
//			mTouryService.showTourList(false);
//		}
		//mServerThread.closeSocket();
		super.onStop();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mResumed = true;
		openOptionsMenu();
	}
	
	@Override
	public void openOptionsMenu() {
		if(mResumed && mTouryService != null) {
			Log.d(TAG, "Opening options menu...");
			super.openOptionsMenu();
			
		}
	}
	
	@Override
	protected void onPause() {
		mResumed = false;
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		this.menu = menu;
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
			
			
			while(c.moveToNext()){
				Tour t = new Tour(c.getInt(c.getColumnIndex(ToursTableMetaData._ID)), c.getString(c.getColumnIndex(ToursTableMetaData.NAME)));
				tours.add(t);
				Log.d(TAG, t.getName());
				subMenu.add(1, t.getId(), 0, t.getName());
			}
		}

		return true;
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		//unbindService(mConnection);
		//finish();	
	}
	
	@Override
	protected void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}
	
	private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		Log.d(TAG, "In activity result");
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            search(results);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	
	public void search(List<String> results){
		for(String word : results){
			for(Marker marker : mTouryService.getCurrentMarkers()){
				if(word.toLowerCase().contains(marker.getTitle().toLowerCase())){
					mTouryService.getRenderer().getTouryView().setSearchResultId(marker.getId());
					Log.d(TAG, marker.getDescription());
					finish();
					return;
				}
			}
		}
		mTouryService.getRenderer().getTouryView().setSearchResultId(null);
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		for(Tour t : tours){
			if(t.getId() == item.getItemId()){
				mTouryService.loadTour(t.getId());
				PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("last_tour", t.getId()).commit();
				finish();
				return true;
			}
		}
		switch(item.getItemId()){
		case R.id.stop:
			stopService(new Intent(this, TouryService.class));
			finish();
			return true;
		case R.id.sync:
			Intent i2 = new Intent(this, SyncService.class);
			startService(i2);
			finish();
			return true;
		case R.id.debug:
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("debug", false))
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("debug", false).commit();
			else
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("debug", true).commit();
			finish();
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
			finish();
			return true;
		case R.id.autoload:
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoload", false))
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("autoload", false).commit();
			else {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("autoload", true).commit();
				PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("last_tour", -1).commit();
			}
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
