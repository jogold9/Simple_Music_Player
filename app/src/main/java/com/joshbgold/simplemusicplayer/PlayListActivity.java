package com.joshbgold.simplemusicplayer;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class PlayListActivity extends ListActivity {

    private EditText editSearch;  //search text input by user
    private ImageView searchIcon;
    public SimpleAdapter simpleAdapter;
    private ListView listView;
    private ArrayList<HashMap<String, String>> songsListData;
    public ArrayList<HashMap<String, String>> songsList = new ArrayList<>();  //stores all the songs
    public ArrayList<HashMap<String, String>> filteredSongsList = new ArrayList<>();  //stores songs that match search
    private int songsAddedCounter = 0;  //counter for debugging -> are songs being added to list?
    public boolean listIsFiltered = false;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3F51B5")));  //sets action bar to color primary dark
        }

        context = getApplicationContext();

        // Set up the layout elements for this activity
        editSearch = (EditText) findViewById(R.id.search);
        searchIcon = (ImageView) findViewById(R.id.search_icon);

        songsListData = new ArrayList<>();  //Stores all the songs to put into ListView

        final SongsManager songsManager = new SongsManager(context);
        // get all songs from SD card
        this.songsList = songsManager.getPlayList();  //gets all the songs from the phone and puts them in the HashMap

        createListViewUsingSongs();  //draws the ListView on the screen using the songsList HashMap

        // selecting single ListView item
        listView = getListView();

        // listening to single playlist_item item click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                String songTitle;
                                                String songPath;
                                                String songUniqueID;
                                                int songIndex;

                                                HashMap<String, String> song = (HashMap<String, String>) parent.getItemAtPosition(position);
                                                songTitle = song.get("songTitle");
                                                songPath = song.get("songPath");
                                                songUniqueID = song.get("songUniqueID");

                                                songIndex = Integer.parseInt(songUniqueID);

                                                // Starting new intent
                                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                                                // Sending song path and song title to MainActivity
                                                intent.putExtra("songIndex", songIndex);
                                                intent.putExtra("songTitle", songTitle);
                                                intent.putExtra("songPath", songPath);

                                                setResult(100, intent);

                                                // Closing PlayListView
                                                finish();

                                            }
                                        }

        );

        /**
         * When user clicks search icon, execute a search, then update the ListView simpleAdapter
         */
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String text = editSearch.getText().toString().toLowerCase(Locale.getDefault());
                filteredSongsList = songsManager.filter(text);
                updateListViewUsingSongs();
                listIsFiltered = true;
            }
        });
    }

    private void createListViewUsingSongs() {
        // looping through playlist
        for (int i = 0; i < songsList.size(); i++) {
            // creating new HashMap
            HashMap<String, String> song = songsList.get(i);
            // adding HashList to ArrayList
            songsListData.add(song);

        }

        // Adding menuItems to ListView
        simpleAdapter = new SimpleAdapter(this, songsListData,
                R.layout.playlist_item, new String[]{"songTitle"}, new int[]{
                R.id.songTitle});

        setListAdapter(simpleAdapter);
    }

    private void updateListViewUsingSongs() {

        songsAddedCounter = 0;
        songsListData.clear();  //super important that we start from zero, and add only the filtered songs!
        songsList.clear(); //TODO: Is this line needed??

        // looping through playlist
        for (int i = 0; i < filteredSongsList.size(); i++) {
            // creating new HashMap
            HashMap<String, String> song = filteredSongsList.get(i);
            // adding HashList to ArrayList
            songsListData.add(song);
            songsAddedCounter++;
        }
        Toast.makeText(getApplicationContext(), "Search results: " + songsAddedCounter + " songs", Toast.LENGTH_SHORT).show();

        simpleAdapter = null;

        simpleAdapter = new SimpleAdapter(this, songsListData,
                R.layout.playlist_item, new String[]{"songTitle"}, new int[]{
                R.id.songTitle});


        setListAdapter(simpleAdapter);
        simpleAdapter.notifyDataSetChanged();
    }

}