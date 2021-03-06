package com.joshbgold.simplemusicplayer;

/**
 * Based on code from Ravi Tamada http://www.androidhive.info/2012/03/android-building-audio-player-tutorial/
 * I added the search feature, and a feature to select the media source.
 */

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MainActivity extends Activity implements MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private static final int REQUEST_SAVE = 101;
    private static final int REQUEST_LOAD = 102;
    private static final int RED = 0;
    private static final int BLUE = 1;
    private static final int GREEN = 2;
    private static final int GRAY = 3;

    //variables for layout items
    private ImageButton btnPlay;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private ImageButton btnNext;
    private ImageButton btnPrevious;
    private ImageButton btnPlaylist;
    private ImageButton btnRepeat;
    private ImageButton btnShuffle;
    private ImageButton btnFolder_icon;
    private SeekBar songProgressBar;
    private TextView songTitleLabel;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private TextView albumTextView;
    private TextView artistTextView;
    private MediaMetadataRetriever metaRetriver;

    // Media Player
    private MediaPlayer mediaPlayer;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();

    private SongsManager songManager;
    private Utilities utils;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds
    private int currentSongIndex = 0;
    private String songTitle = "";
    private String songPath = "";
    private String songUniqueID = "";
    /*    private String songArtist = "";
        private String songAlbum = "";*/
    private boolean isShuffle = true;
    private boolean isRepeat = false;
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<>();
    private Context context;
    private int song_position;
    public String folderPath = "";
    public String musicFolderPath = "";
    protected String colorTheme = "blue";

    public MainActivity(Context context) {
        this.context = context;
    }

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Views listed here for setting colors as per user preferences
        android.app.ActionBar actionBar = getActionBar();
        View mainView = findViewById(R.id.main_layout);
        View headerView = findViewById(R.id.player_header);
        View footerView = findViewById(R.id.player_footer);

       colorTheme = loadPrefs("color", colorTheme);

        if (colorTheme.equals("red")){
            SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#770F0F")), getResources().getColor(R.color.redPrimaryDark), getResources().getColor(R.color.redPrimary));
        }
        else if (colorTheme.equals("green")){
            SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#094A1D")), getResources().getColor(R.color.greenPrimaryDark), getResources().getColor(R.color.greenPrimary));
        }
        else if (colorTheme.equals("grey")){
            SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#4B4B4B")), getResources().getColor(R.color.greyPrimaryDark), getResources().getColor(R.color.greyPrimary));
        }
        else {
            //do nothing
        }


        // All player buttons
        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnForward = (ImageButton) findViewById(R.id.btnForward);
        btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
        btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
        btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
        btnFolder_icon = (ImageButton) findViewById(R.id.folder_icon);
        songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
        songTitleLabel = (TextView) findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
        albumTextView = (TextView) findViewById(R.id.album);
        artistTextView = (TextView) findViewById(R.id.artist);

        musicFolderPath = loadPrefs("folder", musicFolderPath);  //if user has chosen a media folder, get their choice

        // Mediaplayer
        mediaPlayer = new MediaPlayer();
        songManager = new SongsManager(context, musicFolderPath);
        utils = new Utilities();

        // Listeners
        songProgressBar.setOnSeekBarChangeListener(this); // Important
        mediaPlayer.setOnCompletionListener(this); // Important

        // Getting all songs playlist_item
        songsList = songManager.getPlayList();

        /**
         * Button Click event for Play playlist_item click event
         * Launches playlist_item activity which displays playlist_item of songs
         * */
        btnPlaylist.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent intent = new Intent(getApplicationContext(), PlayListActivity.class);
                startActivityForResult(intent, 100);

            }
        });

        /**
         * Button Click event for choosing a folder where music is kept
         * More info on Directory Picker can be found here: https://www.bgreco.net/directorypicker/readme.html
         * */
        btnFolder_icon.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Select folder for music (https://code.google.com/archive/p/android-file-dialog/)
                Intent intent = new Intent(getBaseContext(), FileDialog.class);
                intent.putExtra(FileDialog.START_PATH, "/");

                //can user select directories or not
                intent.putExtra(FileDialog.CAN_SELECT_DIR, true);
                //intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);  //Prevents new items folder & file creation.

                //Stop mediaPlayer if present so that I do not have multiple mediaPlayers running later on when returning to this activity
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                    song_position = mediaPlayer.getCurrentPosition();
                    btnPlay.setImageResource(R.drawable.ic_av_play_circle_fill);
                }

                startActivityForResult(intent, REQUEST_SAVE);
            }
        });

        /**
         * Play button click event
         * Plays & stops songs
         * */

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    song_position = mediaPlayer.getCurrentPosition();
                    btnPlay.setImageResource(R.drawable.ic_av_play_circle_fill);
                }
                //play song if songs list is not empty
                else if (songsList != null && songsList.size() > 0) {
                    mediaPlayer.seekTo(song_position);
                    mediaPlayer.start();
                    btnPlay.setImageResource(R.drawable.ic_av_pause_circle_fill);
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Oh noes!")
                            .setMessage("No songs found.  Please click folder icon at top right and select your music folder." + "\n" + "\n" +
                                    "You may want to look in folders called sdcard or storage.")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //do nothing except close the alert dialog box
                                }
                            })
                            .setIcon(R.drawable.ic_hardware_headset)
                            .show();
                }
            }
        });

        /**
         * Forward button click event
         * Forwards song specified seconds
         * */
        btnForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (mediaPlayer.isPlaying()) {
                    // get current song position
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    // check if seekForward time is lesser than song duration
                    if (currentPosition + seekForwardTime <= mediaPlayer.getDuration()) {
                        // forward song
                        mediaPlayer.seekTo(currentPosition + seekForwardTime);
                    } else {
                        // forward to end position
                        mediaPlayer.seekTo(mediaPlayer.getDuration());
                    }
                }
            }
        });

        /**
         * Backward button click event
         * Backward song to specified seconds
         * */
        btnBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mediaPlayer.isPlaying()) {
                    // get current song position
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    // check if seekBackward time is greater than 0 sec
                    if (currentPosition - seekBackwardTime >= 0) {
                        // forward song
                        mediaPlayer.seekTo(currentPosition - seekBackwardTime);
                    } else {
                        // backward to starting position
                        mediaPlayer.seekTo(0);
                    }
                }
            }
        });

        /**
         * Next button click event
         * Plays next song by taking currentSongIndex + 1
         * */
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (mediaPlayer.isPlaying()) {
                    // check if next song is there or not
                    if (currentSongIndex < (songsList.size() - 1)) {
                        playSong(currentSongIndex + 1, songTitle, songPath);
                        currentSongIndex = currentSongIndex + 1;
                    } else {
                        // play first song
                        playSong(0, songTitle, songPath);
                        currentSongIndex = 0;
                    }
                }
            }
        });

        /**
         * Back button click event
         * Plays previous song by currentSongIndex - 1
         * */
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (mediaPlayer.isPlaying()) {
                    if (currentSongIndex > 0) {
                        playSong(currentSongIndex - 1, songTitle, songPath);
                        currentSongIndex = currentSongIndex - 1;
                    } else {
                        // play last song
                        playSong(songsList.size() - 1, songTitle, songPath);
                        currentSongIndex = songsList.size() - 1;
                    }
                }
            }
        });

        /**
         * Button Click event for Repeat button
         * Enables repeat flag to true
         * */
        btnRepeat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (isRepeat) {
                    isRepeat = false;
                    Toast.makeText(getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                    btnRepeat.setImageResource(R.drawable.ic_av_repeat);
                } else {
                    // make repeat to true
                    isRepeat = true;
                    Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isShuffle = false;
                    btnRepeat.setImageResource(R.drawable.ic_av_repeat);
                    btnShuffle.setImageResource(R.drawable.ic_av_shuffle);
                }
            }
        });

        /**
         * Button Click event for Shuffle button
         * Enables shuffle flag to true
         * */
        btnShuffle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (isShuffle) {
                    isShuffle = false;
                    Toast.makeText(getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                    btnShuffle.setImageResource(R.drawable.ic_av_shuffle);
                } else {
                    // make repeat to true
                    isShuffle = true;
                    Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isRepeat = false;
                    btnShuffle.setImageResource(R.drawable.ic_av_shuffle);
                    btnRepeat.setImageResource(R.drawable.ic_av_repeat);
                }
            }
        });
    }

 /*   @Override
    protected void onResume() {
        super.onResume();
        musicFolderPath = loadPrefs("folder", musicFolderPath);  //if user has chosen a media folder, get their choice
        // Getting all songs playlist_item
        songsList = songManager.getPlayList();
    }*/

    /**
     * Receiving song index from playlist view
     * and play the song
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100) {
            currentSongIndex = data.getExtras().getInt("songIndex");
            songUniqueID = data.getExtras().getString("songUniqueID");
            songTitle = data.getExtras().getString("songTitle");
            songPath = data.getExtras().getString("songPath");
        /*    songArtist = data.getExtras().getString("artist");
            songAlbum = data.getExtras().getString("album");*/
            // play selected song
            if (!"".equals(songTitle) && songTitle != null && !"".equals(songPath) && songPath != null) {
                playSong(currentSongIndex, songTitle, songPath);
            }

   /*         artistTextView.setText("Artist: " + songArtist);
            albumTextView.setText("Album: " + songAlbum);*/
        }

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_SAVE) {
                System.out.println("Saving...");
            } else if (requestCode == REQUEST_LOAD) {
                System.out.println("Loading...");
            }

            folderPath = data.getStringExtra(FileDialog.RESULT_PATH);

        }
      /*  else if (resultCode == Activity.RESULT_CANCELED) {
            Logger.getLogger(AccelerationChartRun.class.getName()).log(
                    Level.WARNING, "file not selected");
        }*/

    }

    /**
     * Function to play a song
     */
    public void playSong(int songIndex, String songTitle, String songPath) {
        String path;

        // Play song
        try {
            mediaPlayer.reset();

            if (songsList.size() - 1 >= songIndex) {
                path = songsList.get(songIndex).get("songPath");
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                mediaPlayer.start();

                // Displaying Song title
                songTitleLabel.setText(songsList.get(songIndex).get("songTitle"));

                // Changing Button Image to pause image
                btnPlay.setImageResource(R.drawable.ic_av_pause_circle_fill);

                // set Progress bar values
                songProgressBar.setProgress(0);
                songProgressBar.setMax(100);

                // Updating progress bar
                updateProgressBar();

                //get & display artist & album
                getAdditionalMediaInfo(path);
            }
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    private void getAdditionalMediaInfo(String path) {
        metaRetriver = new MediaMetadataRetriever();
        metaRetriver.setDataSource(path);  //path holds location of the current song
        try {
            albumTextView.setText(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        } catch (Exception exception) {
            //leave these textViews blank if unable to retrieve the album or artist
            albumTextView.setText("");
        }

        try {
            artistTextView.setText(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        } catch (Exception exception) {
            //leave these textViews blank if unable to retrieve the album or artist
            artistTextView.setText("");
        }
    }

    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mediaPlayer.getDuration();
            long currentDuration = mediaPlayer.getCurrentPosition();

            // Displaying Total Duration time
            songTotalDurationLabel.setText("" + utils.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = utils.getProgressPercentage(currentDuration, totalDuration);
            //Log.d("Progress", ""+progress);
            songProgressBar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    /**
     *
     * */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    /**
     * When user stops moving the progress handler
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mediaPlayer.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    /**
     * On Song Playing completed
     * if repeat is ON play same song again
     * if shuffle is ON play random song
     */
    @Override
    public void onCompletion(MediaPlayer arg0) {

        // check for repeat is ON or OFF
        //noinspection StatementWithEmptyBody
        if (songsList.size() == 0) {
            //do nothing, because there are no songs in the songList
        } else {
            if (isRepeat) {
                // repeat is on play same song again
                playSong(currentSongIndex, songTitle, songPath);
            } else if (isShuffle) {
                // shuffle is on - clear album and artist, then play a random song
      /*      albumTextView.setText("");
            artistTextView.setText("");*/
                Random rand = new Random();
                currentSongIndex = rand.nextInt((songsList.size() - 1) + 1);
                playSong(currentSongIndex, songTitle, songPath);
            } else {
                // no repeat or shuffle ON - clear album and artist, then play next song
/*            albumTextView.setText("");
            artistTextView.setText("");*/
                if (currentSongIndex < (songsList.size() - 1)) {
                    playSong(currentSongIndex + 1, songTitle, songPath);
                    currentSongIndex = currentSongIndex + 1;
                } else {
                    // play first song
                    playSong(0, songTitle, songPath);
                    currentSongIndex = 0;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
    }

    //get prefs
    public String loadPrefs(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences("SimpleMusicPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, value);
    }

    //save prefs
    public void savePrefs(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences("SimpleMusicPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mediaPlayer.stop();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        menu.add(0, RED, 0, "Fire Theme");
        menu.add(1, BLUE, 1, "Sky Theme");
        menu.add(2, GREEN, 2, "Forest Theme");
        menu.add(3, GRAY, 3, "Smoke Theme");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        android.app.ActionBar actionBar = getActionBar();
        View mainView = findViewById(R.id.main_layout);
        View headerView = findViewById(R.id.player_header);
        View footerView = findViewById(R.id.player_footer);

        switch (item.getItemId()) {
            case 0:
                SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#770F0F")), getResources().getColor(R.color.redPrimaryDark), getResources().getColor(R.color.redPrimary));
                colorTheme = "red";
                savePrefs("color", colorTheme);
                return super.onOptionsItemSelected(item);

            case 1:
                SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#303F9F")), getResources().getColor(R.color.colorPrimaryDark), getResources().getColor(R.color.colorPrimary));
                colorTheme = "blue";
                savePrefs("color", colorTheme);
                return super.onOptionsItemSelected(item);
            case 2:
                SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#094A1D")), getResources().getColor(R.color.greenPrimaryDark), getResources().getColor(R.color.greenPrimary));
                colorTheme = "green";
                savePrefs("color", colorTheme);
                return super.onOptionsItemSelected(item);
            case 3:
                SetBackgroundColor(actionBar, mainView, headerView, footerView, new ColorDrawable(Color.parseColor("#4B4B4B")), getResources().getColor(R.color.greyPrimaryDark), getResources().getColor(R.color.greyPrimary));
                colorTheme = "grey";
                savePrefs("color", colorTheme);
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void SetBackgroundColor(ActionBar actionBar, View mainView, View headerView, View footerView, ColorDrawable drawable, int color, int color2) {
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(drawable);
        }
        headerView.setBackgroundColor(color);
        footerView.setBackgroundColor(color);
        mainView.setBackgroundColor(color2);
    }
}