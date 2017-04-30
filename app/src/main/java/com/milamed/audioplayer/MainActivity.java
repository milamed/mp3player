package com.milamed.audioplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mPlay;
    private TextView mStop;
    private TextView mFavorite;
    static TextView mMusicDuration;
    static TextView mCurrentPosition;
    private boolean isFavorite=false;

    static DiscreteSeekBar mSeekbar;


    public static final String MEDIA_PLAYER_APP_MESSENGER_KEY = "app_messenger";
    private AppHandler mHandler;
    private Messenger mAppMessenger;
    private Messenger messengerToService;
    private MediaPlayerServiceConnection mConnection = new MediaPlayerServiceConnection();
    private boolean isServiceConnected = false;
    private static final String MUSIC_DURATION ="3:45";
    static int SEEK_MAX= 0;
    static boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initService();
    }

    private void initService() {
        Intent serviceIntent = new Intent(this,
                AudioPlayerService.class);
        serviceIntent.putExtra(MEDIA_PLAYER_APP_MESSENGER_KEY, mAppMessenger);
        startService(serviceIntent);
    }

    private void initViews() {
        mFavorite = (TextView) findViewById(R.id.favorite);
        mMusicDuration = (TextView) findViewById(R.id.songduration);
        mCurrentPosition = (TextView) findViewById(R.id.time);
        mPlay = (TextView) findViewById(R.id.play);
        mStop = (TextView) findViewById(R.id.stop);
        mPlay.setOnClickListener(this);
        mFavorite.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mHandler = new AppHandler(this);
        mAppMessenger = new Messenger(mHandler);
        mSeekbar= (DiscreteSeekBar) findViewById(R.id.seekbar);


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.play:
            {
                if (!isPlaying) {
                    playMusic();
                } else {
                    pauseMusic();
                }
            }break;
            case R.id.stop:
            {
                stopMusic();
            }break;
            case R.id.favorite:
            {
                if(!isFavorite)
                {
                    mFavorite.setBackground(getDrawable(R.drawable.favorite));
                    isFavorite=true;
                }
                else if (isFavorite)
                {
                    mFavorite.setBackground(getDrawable(R.drawable.favorite_black));
                    isFavorite=false;
                }
            }break;
        }
    }



    private void stopMusic() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_STOP;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void playMusic() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_START;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    }

    private void pauseMusic() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_PAUSE;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    private void doBind() {
        Log.v("log_iit", "request service bind in activity");
        bindService(
                new Intent(this, AudioPlayerService.class),
                mConnection, Context.BIND_AUTO_CREATE);

    }

    private void doUnbindService() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_SERVICE_CLIENT_UNBOUND;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updatePlayButton() {
        isPlaying = true;
        mPlay.setBackground(getDrawable(R.drawable.pause));

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updatePauseButton() {
        isPlaying = false;
        mPlay.setBackground(getDrawable(R.drawable.play));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopPerformed() {
        isPlaying = false;
        mPlay.setBackground(getDrawable(R.drawable.play));
    }



    /***********************************************************/
    /***************** private classes *************************/
    /**
     * *******************************************************
     */

    private class MediaPlayerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {

            isServiceConnected = true;
            messengerToService = new Messenger(binder);

            Log.v("log_iit", "service connected");

            //try {
            //Message message = Message.obtain();
            //message.what = MediaPlayerService.MEDIA_PLAYER_GET_PODCASTS;
            //messengerToService.send(message);
            //} catch (RemoteException e1) {
            //  e1.printStackTrace();
            //}
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            messengerToService = null;
        }
    }

    private static class AppHandler extends Handler {

        private final WeakReference<MainActivity> mTarget;

        private AppHandler(MainActivity target) {
            mTarget = new WeakReference<MainActivity>(target);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message message) {

            MainActivity target = mTarget.get();
            Bundle bundle;
            switch (message.what) {
                case AudioPlayerService.MEDIA_PLAYER_SERVICE_STARTED:
                    target.doBind();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_START:
                    target.updatePlayButton();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_PAUSE:
                    target.updatePauseButton();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_STOP:
                    target.stopPerformed();
                    break;

            }
        }
    }
}
