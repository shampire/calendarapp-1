package com.jwson.calendarapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

//import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
//import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.client.Firebase;
import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Sets;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.jwson.calendarapp.R;
import com.jwson.calendarapp.domain.User;
import com.jwson.calendarapp.domain.UserEvents;
import com.jwson.calendarapp.utils.Constants;
import com.jwson.calendarapp.utils.DateUtils;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateNewEventActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText startDateText;
    private EditText endDateText;
    private EditText friendsText;
    private String userId;
    private ArrayList<User> friendArray;

    private SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm aa");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        Firebase.setAndroidContext(this);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        startDateText = (EditText) findViewById(R.id.create_start_date);
        startDateText.setInputType(InputType.TYPE_NULL);

        endDateText = (EditText) findViewById(R.id.create_end_date);
        endDateText.setInputType(InputType.TYPE_NULL);

        friendsText = (EditText) findViewById(R.id.create_new_friends);

        setDateTimeField();
        setFriendField();
    }

    private void setFriendField(){
        friendsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateNewEventActivity.this, ShareEventActivity.class);
                startActivityForResult(intent,600);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (600) : {
                if (resultCode == Activity.RESULT_OK) {
                    if(data.getExtras() != null){
                        Log.v("", new Gson().toJson(data.getExtras().getStringArray("friendList")));
                        friendArray = (ArrayList<User>) data.getSerializableExtra("friendList");
                        List<String> fdEmails = new ArrayList<>();
                        for(int i = 0 ; i<friendArray.size(); i ++){
                            fdEmails.add(friendArray.get(i).getEmail());
                        }
                        friendsText.setText(StringUtils.join(fdEmails," , "));
                    }

                }
                break;
            }
        }
    }

    private void setDateTimeField() {
        startDateText.setOnClickListener(this);
        endDateText.setOnClickListener(this);
    }

    @Override
    public void onClick(final View view) {
        Date defaultDate = DateUtils.clearTime(new Date());
        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(new SlideDateTimeListener() {
                    @Override
                    public void onDateTimeSet(Date date) {
                        if (view instanceof EditText) {
                            EditText editText = (EditText) view;
                            editText.setText(mFormatter.format(date));
                        }
                    }
                })
                .setInitialDate(defaultDate)
                //.setMinDate(minDate)
                //.setMaxDate(maxDate)
                .setIs24HourTime(true)
                .setTheme(SlideDateTimePicker.HOLO_DARK)
                .setIndicatorColor(Color.parseColor("#FF8C00"))
                .build()
                .show();
    }

    public void confirmButtonClicked(View view) throws ParseException {
        EditText eventName = (EditText) findViewById(R.id.create_new_name);

        EditText startDateText = (EditText) findViewById(R.id.create_start_date);
        EditText endDateText = (EditText) findViewById(R.id.create_end_date);

        EditText location = (EditText) findViewById(R.id.create_new_location);
        EditText friends = (EditText) findViewById(R.id.create_new_friends);

        String message = eventName.getText().toString() + ", " + startDateText.getText().toString() + ", " + location.getText().toString();
        Log.v("Event created", message);

        String eventNameStr = eventName.getText().toString();
        String startDateStr = startDateText.getText().toString();
        String endDateStr = endDateText.getText().toString();
        String locationStr = location.getText().toString();
        String friendStr = friends.getText().toString();

        Date startDate = mFormatter.parse(startDateStr);
        Date endDate = mFormatter.parse(endDateStr);

        UserEvents newEvent = new UserEvents();
        newEvent.setStartDate(startDate.getTime());
        newEvent.setEndDate(endDate.getTime());
        newEvent.setCreateDate(new Date().getTime());
        newEvent.setLocationName(locationStr);
        newEvent.setName(eventNameStr);
        newEvent.setIconId(R.drawable.day0);
        List<String> uIds = new ArrayList<>();
        uIds.add(userId);
        newEvent.setAdmins(uIds);

        List<String> fdsId = new ArrayList<>();
        for(int i = 0 ; i<friendArray.size(); i ++){
            fdsId.add(friendArray.get(i).getuId());
        }
        newEvent.setConfirmedFriends(fdsId);

        String docId = UUID.randomUUID().toString();
        newEvent.setId(docId);

        writeEventToDB(newEvent);

        Intent intent = new Intent(CreateNewEventActivity.this, MainActivity.class);
//        intent.putExtra("com.jwson.calendarapp.domain.UserEvents", documentId);
        startActivity(intent);
        finish();
    }

    private void writeEventToDB(UserEvents event) {
        // Create a new document and add data
        Firebase ref = new Firebase(Constants.FIREBASE_URL);

        Map<String, Object> eventsToUpdate = new HashMap<String, Object>();
        eventsToUpdate.put("/eventList/" + event.getId(), new ObjectMapper().convertValue(event, Map.class));
        eventsToUpdate.put("/userEvents/" + userId+ "/" + event.getId(), new ObjectMapper().convertValue(event, Map.class));
        eventsToUpdate.put("/pendingEvents/" + userId + "/" + event.getId(), new ObjectMapper().convertValue(event, Map.class));

        //Write to friends' event DB
        List<String> fdsIds = event.getConfirmedFriends();
        for(String fdsId : fdsIds){
            eventsToUpdate.put("/userEvents/" + fdsId + "/" + event.getId(), new ObjectMapper().convertValue(event, Map.class));
        }

//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference eventListRef = database.getReference("eventList");
//        DatabaseReference userEventsRef = database.getReference("userEvents");
//        DatabaseReference pendingEventsRef = database.getReference("pendingEvents");

        ref.updateChildren(eventsToUpdate);

    }
}
