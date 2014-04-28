package twilight.of.the.devs.touryglass;

import com.google.android.glass.app.Card;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TestActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Card card = new Card(this);
		card.setText("Testing");
		setResult(RESULT_OK, new Intent());
		setContentView(card.getView());
		super.onCreate(savedInstanceState);
	}
}
