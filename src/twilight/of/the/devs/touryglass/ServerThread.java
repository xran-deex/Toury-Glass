package twilight.of.the.devs.touryglass;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.UUID;

import twilight.of.the.devs.mylibrary.SimpleGeofence;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ServerThread extends Thread { 

	private static final String TAG = ServerThread.class.getName();
	private BluetoothServerSocket mmServerSocket = null;
	private BluetoothSocket socket = null;
	private BluetoothAdapter mBluetoothAdapter;
	private Context mContext;
	private boolean running;
	
	public ServerThread(Context context) {
		mContext = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		UUID MY_UUID = UUID.fromString("2166f331-7ff9-4f32-802a-77cf52af027e");
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("NAME", MY_UUID);
        } catch (IOException e) { }
        running = true;
	}
	
	public synchronized void quit(){
		running = false;
	}
	
	
	@Override
	public void run(){

		while(running){
			try {
				Log.d("ServerService", "Waiting for socket...");
				socket = mmServerSocket.accept();
			} catch (IOException e) {
				break;
			} 
			if(socket != null && running){
				Log.d("ServerService", "Handling socket...");
				handleSocket(socket);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
		Log.d("ServerService", "Thread is dead");
		//return null;
	}
	
	public void closeSocket(){
		try {
			mmServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void handleSocket(BluetoothSocket socket){

		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		Log.d("ServerService", "In handleSocket...");
		// Get the input and output streams, using temp objects because
		// member streams are final
		try {
		    tmpIn = socket.getInputStream();
		    tmpOut = socket.getOutputStream();
		} catch (IOException e) { 
			Log.d("ServerService", e.getMessage());
		}

		final InputStream mmInStream = tmpIn;
		final OutputStream mmOutStream = tmpOut;
		ObjectInputStream ois = null;
 
        try {
            // Read from the InputStream
        	ois = new ObjectInputStream(mmInStream);
            // Send the obtained bytes to the UI activity
        	SimpleGeofence command = (SimpleGeofence)ois.readObject();
        	
        	Log.d(TAG, command.toString());

        	Intent i = new Intent("location");
        	i.putExtra("loc", command);
        	LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);

        } catch (IOException e) {
        	Log.d("ServerService", e.getMessage());
        } catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
