package com.example.musicplayer2;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    Handler handler=new Handler();
    ArrayList<String> songList=new ArrayList<String>();
    ArrayList<Integer> idList=new ArrayList<Integer>();
    String selectedSongName;
    boolean listTransfer=true;
    boolean isBoundMusicService=false;
    boolean pause=true;
    boolean timerRunning=false;
    PlayMusicService musicService;
    private ServiceConnection connection=new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            PlayMusicService.LocalBinder localBinder=(PlayMusicService.LocalBinder) service;
            musicService=localBinder.getService();
            isBoundMusicService=true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name){ musicService=null; isBoundMusicService=false;}
    };
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#313233"));
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        bindToService();
        checkPermissions();
        refreshMediaAudio();
        populateList();
        populateListView();
        SeekBar sb=(SeekBar)findViewById(R.id.seek_bar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Seek bar progress is :" + progressChangedValue, Toast.LENGTH_SHORT).show();
                int pos=(progressChangedValue*musicService.getMusicDuration())/100;
                Log.d("dbg","Hello"+String.valueOf(pos));
                musicService.seekToPosition(pos);
            }
        });
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.select_dialog_item,songList);
        AutoCompleteTextView actv =  (AutoCompleteTextView)findViewById(R.id.actv);
        actv.setThreshold(1);
        actv.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
        actv.setTextColor(Color.WHITE);
        actv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                selectedSongName = (String) parent.getItemAtPosition(position);
                musicService.setSong(songList.indexOf(selectedSongName));
                if(listTransfer){
                    listTransfer=false;
                    musicService.songs=idList;
                }
                if(timerRunning==true) {
                    handler.removeCallbacks(runnableCode);
                }
                timerRunning=false;
                musicService.play();
                actv.clearListSelection();
                actv.setText("");
            }
        });
    }
    @Override
    public void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("CUSTOM_ACTION");
        LocalBroadcastManager.getInstance(this).registerReceiver(listener,filter);
    }
    public void checkPermissions(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissionCheck!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                checkPermissions();
            }
        }
    }
    public void bindToService(){
        Intent i=new Intent(MainActivity.this,PlayMusicService.class);
        bindService(i,connection, Context.BIND_AUTO_CREATE);
    }
    public void populateList(){
        String[] projections={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME};
        Cursor audioCursor=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projections,null,null,null);
        if(audioCursor!=null){
            if(audioCursor.moveToFirst()){
                do{
                    int audioIndex=audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    songList.add(audioCursor.getString(audioIndex));
                    audioIndex=audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    idList.add(audioCursor.getInt(audioIndex));
                }while(audioCursor.moveToNext());
            }
        }
        audioCursor.close();
    }
    public void populateListView(){
        ListView audioView;
        audioView = (ListView)findViewById(R.id.list_songs);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.list_view_textview,songList);
        audioView.setAdapter(adapter);
        audioView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedSongName = (String) parent.getItemAtPosition(position);
                Toast.makeText(getBaseContext(),selectedSongName,Toast.LENGTH_SHORT).show();
                //musicService.currentSongPosition=position;
                musicService.setSong(position);
                if(listTransfer){
                    listTransfer=false;
                    musicService.songs=idList;
                }
                if(timerRunning==true) {
                    handler.removeCallbacks(runnableCode);
                }
                timerRunning=false;
                musicService.play();
            }
        });
    }
    public void refreshMediaAudio(){
        MediaScannerConnection.scanFile(
                this,
                new String[]{"/storage/emulated/0"},
                new String[]{ "audio/mp3", "*/*" },
                new MediaScannerConnection.MediaScannerConnectionClient() {
                    public void onMediaScannerConnected() { }
                    public void onScanCompleted(String path, Uri uri) { }
                });
    }
    public void setPlayingNowAttributes(Intent intent){
        TextView songNameTextView=(TextView)findViewById(R.id.song_name);
        songNameTextView.setText(intent.getStringExtra("songName"));
        TextView songDuration=(TextView)findViewById(R.id.duration);
        songDuration.setText(intent.getStringExtra("songDuration"));
        if(timerRunning==false) {
            handler.post(runnableCode);
            timerRunning=true;
        }
        ((ImageView)findViewById(R.id.play_button)).setImageResource(android.R.drawable.ic_media_pause);
    }
    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            setPlayingNowAttributes(intent);
        }
    };
    public void handleClickPlayPause(View view){
        if(pause) {
            musicService.pauseMusic();
            pause=false;
            ((ImageView)findViewById(R.id.play_button)).setImageResource(android.R.drawable.ic_media_play);
        }
        else {
            musicService.go();
            ((ImageView)findViewById(R.id.play_button)).setImageResource(android.R.drawable.ic_media_pause);
            pause=true;
        }
    }
    public void handleClickPrev(View v){
        if(timerRunning==true) {
            handler.removeCallbacks(runnableCode);
        }
        timerRunning=false;
        musicService.playPrevious();
    }
    public void handleClickNext(View v){
        if(timerRunning==true) {
            handler.removeCallbacks(runnableCode);
        }
        timerRunning=false;
        musicService.playNext();
    }
    Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            int position=musicService.getMusicCurrentPosition();
            String currenttime = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(position), TimeUnit.MILLISECONDS.toSeconds(position) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(position)));
            ((TextView) findViewById(R.id.current_time)).setText(currenttime);
            int completion=(musicService.getMusicCurrentPosition()*100)/musicService.getMusicDuration();
            Log.d("dbg",String.valueOf(completion));
            ((SeekBar) findViewById(R.id.seek_bar)).setProgress(completion);
            handler.postDelayed(this, 1000);
            //Bitmap bmImg = BitmapFactory.decodeFile(musicService.getArtPath());
            //((ImageView)findViewById(R.id.song_art)).setImageBitmap(bmImg);
        }
    };
    @Override
    public void onDestroy(){
        super.onDestroy();
        handler.removeCallbacks(runnableCode);
    }
}