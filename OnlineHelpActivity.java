package com.desperate.pez_android.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.desperate.pez_android.R;
import com.desperate.pez_android.other.ChatBoxAdapter;
import com.desperate.pez_android.other.Message;
import com.desperate.pez_android.other.User;
import com.desperate.pez_android.other.UserInfo;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.r0adkll.slidr.Slidr;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class OnlineHelpActivity extends AppCompatActivity {

    public RecyclerView myRecylerView;
    public List<Message> MessageList;
    public ChatBoxAdapter chatBoxAdapter;
    public EditText messagetxt;
    public Button send ;
    //declare socket object
    private Socket socket;
    public String Nickname ;
    private static String chatId = "-356159912";//   -1001474902363
    public String userId;

    User user;
    SharedPreferences  mPrefs;
    String [] accounts;
    String account;
    int spinnerPosition;
    String socketUserId;
    UserInfo savedData;
    String jsonMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        savedData = new UserInfo(getApplicationContext());
        spinnerPosition = savedData.getSelectedSpinner();
        accounts = savedData.getAccounts();
        user = savedData.getSavedUser();
        mPrefs = getPreferences(MODE_PRIVATE);

        Slidr.attach(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        messagetxt = (EditText) findViewById(R.id.message);
        send = (Button)findViewById(R.id.send);
        // get the nickame of the user
        Nickname= "Ви: ";
        //connect you socket client to the server



        try {
            socket = IO.socket("https://telegram.energo.pl.ua:8443");
            socket.connect();

            userId = generateUserId();

            if (spinnerPosition == 0) {
                account = String.valueOf(user.getAccountList().get(0).getAccount());
            } else {
                for (int i = 1; i<=accounts.length-1; i++){
                    if (spinnerPosition == i)
                        account = String.valueOf(user.getAccountList().get(i).getAccount());
                }
            }

            JSONObject object = new JSONObject();
            object.put("userId", userId);
            object.put("chatId", chatId);
            object.put("account", account);


           // Toast.makeText(getApplicationContext(),spinnerPosition+" "+ account, Toast.LENGTH_LONG).show();
            socket.emit("register", object);
            //socket.emit("join", Nickname);


        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //setting up recyler
        myRecylerView = (RecyclerView) findViewById(R.id.messagelist);
        myRecylerView.setLayoutManager(new LinearLayoutManager(this));
        myRecylerView.setItemAnimator(new DefaultItemAnimator());
        Type messagesListType = new TypeToken<List<Message>>() {}.getType();
        jsonMessages = mPrefs.getString("savedMessages", "");
        //Log.e("jsonMessages", jsonMessages);
        MessageList = new Gson().fromJson(jsonMessages,messagesListType);
        if(MessageList != null && MessageList.size() > 0) {
            //Log.e("saved", "saved");
            loadPrevioudMessages();
            //going down
            scrollToBottom();
        } else {
            //Log.e("saved", "new");
            MessageList = new ArrayList<>();
            sendInviteMsg("Добрий день. Будь ласка, опишіть Вашу проблему");
        }

        myRecylerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                myRecylerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {}

            @Override
            public void onLongClick(View view, int position) {
                String message = MessageList.get(position).getMessage();
                // place your TextView's text in clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("messageText", message);
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(OnlineHelpActivity.this, "Повідомлення скопійовано",
                        Toast.LENGTH_LONG).show();
            }
        }));

        // message send action
        send.setOnClickListener(v -> {
            //retrieve the nickname and the message content and fire the event messagedetection
            if(!messagetxt.getText().toString().isEmpty()){
                JSONObject object = new JSONObject();
                try {
                    object.put("nickname", Nickname);
                    object.put("text", messagetxt.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                socket.emit("message", object);

                messagetxt.setText(" ");
            }
        });
        //disable timeout
        socket.io().timeout(-1);
        //implementing socket listeners
        socket.on("userjoinedthechat", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String data = (String) args[0];
                        Toast.makeText(OnlineHelpActivity.this,data,Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        socket.on("userdisconnect", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String data = (String) args[0];
                        Toast.makeText(OnlineHelpActivity.this,data,Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        socket.on(userId, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(() -> {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        //extract data from fired event

                        String nickname = data.getString("senderNickname");
                        String message = data.getString("message");

                        // make instance of message

                        Message m = new Message(nickname,message);


                        //add the message to the messageList

                        MessageList.add(m);


                        // add the new updated list to the adapter
                        chatBoxAdapter = new ChatBoxAdapter(MessageList);


                        // notify the adapter to update the recycler view

                        chatBoxAdapter.notifyDataSetChanged();

                        //set the adapter for the recycler view

                        myRecylerView.setAdapter(chatBoxAdapter);

                        //going down
                        scrollToBottom();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notifications, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_notifications) {
            onClearListButtonClick();
        } else {
            finish();
        }
        return true;
    }

    private void loadPrevioudMessages() {


        chatBoxAdapter = new ChatBoxAdapter(MessageList);

        // notify the adapter to update the recycler view

        chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view

        myRecylerView.setAdapter(chatBoxAdapter);
    }

    private void sendInviteMsg(String message) {
        MessageList.add(new Message(" ", message));
        // add the new updated list to the adapter
        chatBoxAdapter = new ChatBoxAdapter(MessageList);

        // notify the adapter to update the recycler view

        chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view

        myRecylerView.setAdapter(chatBoxAdapter);
    }

    private void onClearListButtonClick() {
        SharedPreferences.Editor edit = mPrefs.edit();
            edit.putString("savedMessages", "");
            edit.commit();
            MessageList = new ArrayList<>();
        chatBoxAdapter.notifyDataSetChanged();
        sendInviteMsg("Добрий день. Будь ласка, опишіть Вашу проблему");

    }

    public interface ClickListener{
        void onClick(View view,int position);
        void onLongClick(View view,int position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        jsonMessages = new Gson().toJson(MessageList);
        //Log.e("jsonMessagesOnBack", jsonMessages);
        prefsEditor.putString("savedMessages", jsonMessages);
        prefsEditor.commit();
        socket.disconnect();
    }

    private void scrollToBottom() {
        myRecylerView.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
    }

    private String generateUserId() {
        if (!savedData.getSocketUserId().equals("")) {
            return savedData.getSocketUserId();
        } else {
            socketUserId = String.valueOf(Math.random()).substring(2, 8);
            savedData.setSocketUserId(socketUserId);

            return socketUserId;
        }

    }
}

class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{

    private OnlineHelpActivity.ClickListener clicklistener;
    private GestureDetector gestureDetector;

    public RecyclerTouchListener(Context context, final RecyclerView recycleView, final OnlineHelpActivity.ClickListener clicklistener){

        this.clicklistener=clicklistener;
        gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                if(child!=null && clicklistener!=null){
                    clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                }
            }
        });
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        View child=rv.findChildViewUnder(e.getX(),e.getY());
        if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
            clicklistener.onClick(child,rv.getChildAdapterPosition(child));
        }

        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
