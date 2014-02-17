package twilight.of.the.devs.touryglass;

import twilight.of.the.devs.touryglass.TouryService.LocalBinder;

import com.google.android.glass.app.Card;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getName();
	private boolean mResumed, serviceStarted;
	protected LocalBinder mTouryService;
	
	private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof TouryService.LocalBinder) {
                mTouryService = (TouryService.LocalBinder) service;
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
		//Card card = new Card(this);
        //card.setText("Toury");
        //card.setFootnote("This is a footnote");
        //setContentView(card.toView());
		//serviceStarted = false;
		bindService(new Intent(this, TouryService.class), mConnection, 0);
		//openOptionsMenu();
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "In Stop");
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
		if(mResumed && mTouryService != null)
			super.openOptionsMenu();
	}
	
	@Override
	protected void onPause() {
		mResumed = false;
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		unbindService(mConnection);
		finish();
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.stop:
			stopService(new Intent(this, TouryService.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
