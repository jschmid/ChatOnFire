package pro.schmid.android.chatonfire;

import pro.schmid.android.androidonfire.DataSnapshot;
import pro.schmid.android.androidonfire.Firebase;
import pro.schmid.android.androidonfire.FirebaseEngine;
import pro.schmid.android.androidonfire.Query;
import pro.schmid.android.androidonfire.callbacks.DataEvent;
import pro.schmid.android.androidonfire.callbacks.EventType;
import pro.schmid.android.androidonfire.callbacks.FirebaseLoaded;
import pro.schmid.android.androidonfire.callbacks.Transaction;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class MainChatActivity extends Activity {

	private FirebaseEngine mFirebaseEngine;
	private Firebase mFirebase;
	private Firebase mChatFirebase;
	private Firebase mMessages;

	private EditText mEt;
	private LinearLayout mMessagesView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mEt = (EditText) findViewById(R.id.message_et);
		mMessagesView = (LinearLayout) findViewById(R.id.messages);

		mFirebaseEngine = FirebaseEngine.getInstance();
		mFirebaseEngine.setLoadedListener(mFirebaseLoadedListener);
		mFirebaseEngine.loadEngine(this);

		mEt.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
					sendMessage();
					return true;
				}
				return false;
			}
		});

		Button send = (Button) findViewById(R.id.send_button);
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFirebaseEngine.onDestroy();
	}

	private void sendMessage() {
		String name = "Android";
		String message = mEt.getText().toString();
		mEt.setText("");
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", name);
		obj.addProperty("text", message);

		mMessages.push(obj);
	}

	private final FirebaseLoaded mFirebaseLoadedListener = new FirebaseLoaded() {

		@Override
		public void firebaseLoaded() {
			mFirebase = mFirebaseEngine.newFirebase("https://neqo.firebaseio.com");
			mChatFirebase = mFirebase.child("chat");
			mMessages = mChatFirebase.child("messages");

			// Last 5 comments
			Query query = mMessages.limit(5).endAt();

			query.on(EventType.child_added, messageAddedListener);
			query.on(EventType.child_removed, messageRemovedListener);

			query.once(EventType.child_removed, new DataEvent() {
				@Override
				public void callback(DataSnapshot snapshot, String prevChildName) {
					Toast.makeText(MainChatActivity.this, "First child removed", Toast.LENGTH_SHORT).show();
				}
			});

			Firebase presence = mChatFirebase.child("presence").child("Android");
			presence.removeOnDisconnect();
			presence.set(new JsonPrimitive(true));

			Firebase counter = mChatFirebase.child("count");
			counter.transaction(new Transaction() {
				@Override
				public JsonElement transaction(JsonElement obj) {
					if (obj.isJsonNull()) {
						return new JsonPrimitive(1);
					} else {
						int count = obj.getAsInt();

						return new JsonPrimitive(1 + count);
					}
				}
			});
		}
	};

	private final DataEvent messageAddedListener = new DataEvent() {
		private int count = 0;

		@Override
		public void callback(final DataSnapshot snapshot, String prevChildName) {
			++count;

			final String name = snapshot.child("name").val().getAsString();
			final String message = snapshot.child("text").val().getAsString();

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String line = String.format("%s: %s\n", name, message);
					TextView tv = new TextView(MainChatActivity.this);
					tv.setText(line);
					tv.setTag(snapshot.name());
					mMessagesView.addView(tv);
				}
			});
		}
	};

	private final DataEvent messageRemovedListener = new DataEvent() {

		@Override
		public void callback(DataSnapshot snapshot, String prevChildName) {
			final View messageToRemoveView = mMessagesView.findViewWithTag(snapshot.name());

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mMessagesView.removeView(messageToRemoveView);
				}
			});
		}
	};
}
