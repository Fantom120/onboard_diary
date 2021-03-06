package com.example.Onboard_diary.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.app.DatePickerDialog.OnDateSetListener;
import com.example.Onboard_diary.*;
import com.example.Onboard_diary.fragment.record_play_audio.AudioPlayFragment;
import com.example.Onboard_diary.fragment.record_play_audio.AudioRecordFragment;
import com.example.Onboard_diary.fragment.record_play_audio.Record;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.software.shell.fab.ActionButton;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


public class EditDataFragment extends  Fragment  {

    private EditText editTheme, editDiscription;
    private static final int REQUEST_RECORD = 1;
    private static final int REQUEST_PLAY = 2;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private ArrayList pathKeysSet;
    private static final String APP_PREFERENCES = "audioPath";


    private DataItem item;
    private Db_Main db;
    private MainActivity activity;
    private int day;
    private int month;
    private int year;
    private boolean newItem;
    private Button btnRec, btnPlay;
    private ActionButton fab;


    private static final SimpleDateFormat FORMAT_TITLE = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat FORMAT_SUBTITLE = new SimpleDateFormat("EEEE", Locale.getDefault());
    private Calendar calendar = Calendar.getInstance();
    private View view;

    public EditDataFragment() {
        this.setRetainInstance(true);
    }

    public static EditDataFragment getInstance(Bundle args){
        EditDataFragment dataFragment = new EditDataFragment();
        dataFragment.setArguments(args);
        return dataFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

 if(view == null) view = inflater.inflate(R.layout.input, container, false);
 else ((ViewGroup) view.getParent()).removeView(view);

        if (getActivity() != null) {
            activity = (MainActivity) getActivity();
            db = new Db_Main(getActivity());}


        pathKeysSet = new ArrayList<>();
        pref =  getActivity().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        initUI();

        item = getArguments().getParcelable("edit");

            if (item != null && item.getTheme() != null) {
                editTheme.setText(item.getTheme());
                editDiscription.setText(item.getDescription());

                calendar.setTimeInMillis(item.getDate());
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
                newItem = false;

          getAudioRecord();

            }
         else {
            item = addNewItem();
                newItem = true;
        }
        setHasOptionsMenu(true);

       Log.d("log", "onCreateView in EditDataFragment");
        return view;

    }

    View.OnClickListener onEditItem =  new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isEmpty(editTheme)) {
                item.setTheme(editTheme.getText().toString());
                item.setDescription(editDiscription.getText().toString());
                item.setDate(calendar.getTimeInMillis());
                if (newItem) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            db.addItem(item);
                        }
                    }).run();

                    Log.d("log", item.getDescription());
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            db.updateItem(item);
                        }
                    }).run();

                }

                activity.onItemCreated(MainListFragment.getInstance());
            }
        }
    };

   private void initUI(){
        btnRec = (Button) view.findViewById(R.id.btnRecord);
        btnRec.setOnClickListener(rec);

        btnPlay = (Button) view.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(play);

        editTheme = (EditText) view.findViewById(R.id.editTheme);
        editDiscription = (EditText) view.findViewById(R.id.editDescription);
       editDiscription.requestFocus();

       fab = (ActionButton) view.findViewById(R.id.action_edit);
       fab.setOnClickListener(onEditItem);

   }

    @Override
    public void onResume() {
        super.onResume();
      setToolbarDate();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.edit_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu_item) {
        switch (menu_item.getItemId()) {

            case R.id.deleteItem: {
                if (item != null) {
                    ItemDeleteDialogFragment choise = new ItemDeleteDialogFragment();
                    Bundle args = new Bundle();
                    args.putParcelable("delete", item);
                    choise.setArguments(args);
                    choise.show(getChildFragmentManager(), "Dialog");
                }
                break;
            }

            case R.id.share: {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, item.getDescription());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));

                break;
            }
            case R.id.setDate:{
                showDatePicker();
            }
            default:
                break;
        }


        return super.onOptionsItemSelected(menu_item);

    }


    private View.OnClickListener rec = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AudioRecordFragment recordFragment = new AudioRecordFragment();
            recordFragment.setTargetFragment(EditDataFragment.this, REQUEST_RECORD);
           recordFragment.show(getActivity().getSupportFragmentManager(), "AudioRec");
        }
    };

    private View.OnClickListener play  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AudioPlayFragment playFragment = new AudioPlayFragment();
            playFragment.setTargetFragment(EditDataFragment.this, REQUEST_PLAY);
            Bundle args = new Bundle();
            args.putParcelableArrayList("path", pathKeysSet);
            playFragment.setArguments(args);
            playFragment.show(getActivity().getSupportFragmentManager(), "PlayAudio");
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case REQUEST_RECORD:{
                    if(data.getParcelableExtra("setPath") != null){
                        pathKeysSet.add(data.getParcelableExtra("setPath"));

                            String key = String.valueOf(calendar.getTimeInMillis());
                            item.setAudioPathKey(key);
                            editPrefs(key);
                            btnPlay.setVisibility(View.VISIBLE);
                    }
                }break;
                case REQUEST_PLAY: {
                    if (data.getParcelableArrayListExtra("audiolist") != null) {
                        pathKeysSet = data.getParcelableArrayListExtra("audiolist");

                        editPrefs(item.getAudioPathKey());
                    }
                }    break;
            }
        }
    }

    private void showDatePicker() {

        DatePickerFragment date = new DatePickerFragment();

        Bundle args = new Bundle();

        args.putInt("year", year);
        args.putInt("month", month);
        args.putInt("day", day);
        date.setArguments(args);

        /**
         * Set Call back to capture selected date
         */
        date.setCallBack(ondate);
        date.show(activity.getSupportFragmentManager(), "Date Picker");
    }

    private OnDateSetListener ondate = new OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int newyear, int monthOfYear,
                              int dayOfMonth) {
            calendar.set(newyear, monthOfYear, dayOfMonth, 13, 15);
            Log.d("log", "ondate");
            year = newyear;
            month = monthOfYear;
            day = dayOfMonth;
         item.setDate(calendar.getTimeInMillis());
         setToolbarDate();
        }
    };

    private  void setToolbarDate(){
        if(activity != null) {
            activity.getmToolBar().setTitle(FORMAT_TITLE.format(item.getDate()));
            activity.getmToolBar().setSubtitle(FORMAT_SUBTITLE.format(item.getDate()));
        }
    }

    private DataItem addNewItem() {
        DataItem add_item = new DataItem();
        add_item.setDate(calendar.getTimeInMillis());
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        return add_item;
    }

    private boolean isEmpty(EditText etText) {
        if (etText != null && etText.getText().toString().trim().length() == 0) {
            etText.requestFocus();
            Toast.makeText(getActivity(), R.string.isEmpty, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }

    }
   private  void getAudioRecord(){
       if(item.getAudioPathKey() != null && pref.contains(item.getAudioPathKey()) ){

           Gson gson = new Gson();
           String json = pref.getString(item.getAudioPathKey(), "");
           pathKeysSet = gson.fromJson(json, ArrayList.class);
           LinkedTreeMap map;
           File outFile;
           ArrayList<Record> rec = new ArrayList<>();      // pathFile
           for (int i = 0; i < pathKeysSet.size() ; i++) { // date

               map = (LinkedTreeMap) pathKeysSet.get(i);

               Record record = new Record();
               record.setPathFile(map.get("pathFile").toString());
               record.setDate(map.get("date").toString());
               outFile = new File(record.getPathFile());
               if(outFile.exists()) rec.add(record);
           }

           if(rec.isEmpty())  { btnPlay.setVisibility(View.INVISIBLE);}

           else {
               pathKeysSet = rec;
               btnPlay.setVisibility(View.VISIBLE);
           }
       }
   }

    private void editPrefs(String key){
        if(key != null) {

            editor = pref.edit();
            Gson gson = new Gson();
            String json = gson.toJson(pathKeysSet);

            if(!pathKeysSet.isEmpty()) {
                editor.putString(key, json);
            }
            else{
                editor.remove(item.getAudioPathKey());
                item.setAudioPathKey(null);
                editor.putString(key, json);
                btnPlay.setVisibility(View.INVISIBLE);
            }
            editor.apply();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activity.getmToolBar().setSubtitle("");
    }

}
