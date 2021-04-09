package com.example.musicplayer2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayMusicService extends Service {

    MediaPlayer mediaPlayer;
    ArrayList<Integer> songs=new ArrayList<Integer>();
    int currentSongPosition=0;
    public String songTitle="";
    public String songPath="";
    //public String songDuration="";
    public String songArt="";

    private final IBinder mBinder=new LocalBinder();
    public class LocalBinder extends Binder {
        PlayMusicService getService(){
            return PlayMusicService.this;
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mediaPlayer=new MediaPlayer();
        initialiseMusicPLayer();
    }
    public void initialiseMusicPLayer(){
        //mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getBaseContext());
                Intent localIntent = new Intent("CUSTOM_ACTION");
                //Duration
                int duration=getMusicDuration();
                String timeduration = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
                Log.d("dbg",timeduration);
                //Song Name
                if(songTitle.indexOf('.')>20)
                    songTitle=songTitle.substring(0,19)+"...";
                else
                    songTitle=songTitle.substring(0,songTitle.indexOf('.'));
                localIntent.putExtra("songName",songTitle);
                localIntent.putExtra("songDuration",timeduration);
                localBroadcastManager.sendBroadcast(localIntent);
                Log.d("dbg","Successfully started playing music after onPrepared"+songTitle);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPLayer) {
                if(mediaPlayer.getCurrentPosition()>0){
                    mediaPLayer.reset();
                    playNext();
                    Log.d("dbg","Song Successfully completed playing , now shifted to next song");
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                mediaPlayer.reset();
                Log.d("dbg","Error occured in Music Player Service");
                return false;
            }
        });
    }
    public void setSongs(ArrayList<Integer> list){
        songs=list;
    }
    @Override
    public IBinder onBind(Intent intent){
        return(mBinder);
    }
    @Override
    public boolean onUnbind(Intent intent){
        mediaPlayer.stop();
        mediaPlayer.release();
        Log.d("dbg","Service in unbounded with activity");
        return false;
    }
    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        return super.onStartCommand(intent,flags,startId);
    }

    /*Client Functions*/

    public void play(){
        mediaPlayer.reset();
        int songId=songs.get(currentSongPosition);
        getDetails(songId);
        try {
            mediaPlayer.setDataSource(songPath);
        }catch(Exception e){e.printStackTrace();}
        mediaPlayer.prepareAsync();
        Log.d("dbg","Now started playing");
        setnotification();
    }
    public void setSong(int index){
        currentSongPosition=index;
    }
    public int getMusicDuration(){
        int duration=mediaPlayer.getDuration();
        Log.d("dbg","Current music position "+duration);
        return(duration);
    }
    public int getMusicCurrentPosition(){
        int position=mediaPlayer.getCurrentPosition();
        Log.d("dbg","Current music position "+position);
        return(position);
    }
    public boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }
    public void go(){
        mediaPlayer.start();
    }
    public void pauseMusic(){
        mediaPlayer.pause();
        Log.d("dbg","Music successfully paused at Service");
    }
    public void stopMusic(){
        mediaPlayer.stop();
        Log.d("dbg","Music Successfully Stopped at Service");
    }
    public void seekToPosition(int position){
        mediaPlayer.seekTo(position);
        Log.d("dbg","Music Successfully seeked to position "+position);
    }
    public void playPrevious(){
        currentSongPosition--;
        if(currentSongPosition<0) currentSongPosition=songs.size()-1;
        play();
        Log.d("dbg","Successfully playing Previous Track");
    }
    public void playNext(){

        currentSongPosition++;
        if(currentSongPosition>=songs.size()) currentSongPosition=0;
        play();
        Log.d("dbg","Successfully playing Next Track");
    }
    public void getDetails(int id){
        String[] projections={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.DATA};
        Cursor cursor=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projections,MediaStore.Audio.Media._ID+" = "+id,null,null);
        if(cursor!=null){
            if(cursor.moveToFirst()){
                do{
                    int index=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    songTitle=cursor.getString(index);
                    index=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    songPath=cursor.getString(index);
                }while(cursor.moveToNext());
            }
        }
        cursor.close();
    }
    public String getSongName(){
        return songTitle;
    }
    public String getArtPath(){
        return songArt;
    }
    public void setnotification(){
        Notification notification=new Notification.Builder(getApplicationContext())
                .setContentTitle("Music Player : Now playing")
                .setContentText(songTitle)
                .setSmallIcon(android.R.drawable.ic_media_play).build();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1,notification);
    }
}
