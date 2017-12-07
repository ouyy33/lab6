package com.ouyangyi.lab6;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    private IBinder mBinder;
    private static final int REQUEST_EXTERNAL_STORAGE = 0;
    private static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"};
    private static boolean hasPermission =false;
    private static boolean isCreate= false;
    private static boolean isStopped= false;

    private ImageView musicCover;
    private TextView state,currentTime,TotalTime;
    private SeekBar seekBar;
    private MusicService musicService = new MusicService();
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private Button playbutton, stopbutton, quitbutton;
    private ObjectAnimator objectAnimator = new ObjectAnimator();


    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("iBinder","connected");
            mBinder = iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            sc = null;
        }
    };

    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.READ_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
            else {
                hasPermission = true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length >0 &&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //用户同意授权
            startPlaying();
            hasPermission = true;
        }else{
            //用户拒绝授权
            System.exit(0);
        }
        return;
    }

    private void startPlaying() {
        try{
            int code = 106;
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            mBinder.transact(code, data, reply, 0);
            reply.recycle();
            data.recycle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        int TimeNow = 0;
        int TotalTime1 = 0;
        try{
            int code = 104;
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            mBinder.transact(code, data, reply, 0);

            TimeNow = reply.readInt();
            TotalTime1 = reply.readInt();
            reply.recycle();
            data.recycle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        seekBar.setProgress(TimeNow);
        seekBar.setMax(TotalTime1);
        TotalTime.setText(time.format(TotalTime1));
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(MainActivity.this); //动态获取读取内置存储权限

        //绑定Service,保持通信
        Intent intent = new Intent(this,MusicService.class);
        startService(intent);
        bindService(intent,sc,BIND_AUTO_CREATE);

        isCreate= true;
        musicCover = (ImageView)findViewById(R.id.imageView);
        state =(TextView)findViewById(R.id.text_state);
        currentTime=(TextView)findViewById(R.id.time_now);
        TotalTime= (TextView)findViewById(R.id.time_total);
        seekBar =(SeekBar)findViewById(R.id.seekBar);
        seekBar.setProgress(0);
        seekBar.setMax(musicService.Totaltime);
        TotalTime.setText(time.format(musicService.Totaltime));

        //播放和暂停
        playbutton=(Button)findViewById(R.id.button_play);
        playbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCreate = false;
                isStopped =false;
                try{
                    int code = 101;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    mBinder.transact(code,data,reply,0);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        });

        //停止按钮
        stopbutton = (Button)findViewById(R.id.button_stop);
        stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStopped = true;
                isCreate = false;
                try{
                    int code =102;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    mBinder.transact(code,data,reply,0);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        });

        //退出按钮
        quitbutton  =(Button)findViewById(R.id.button_quit);
        quitbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unbindService(sc);
                sc=null;
                try{
                    int code = 103;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    mBinder.transact(code, data, reply, 0);
                    MainActivity.this.finish();
                    System.exit(0);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        });
        //调用新线程
        MyThread();
        //图片的旋转
        objectAnimator = ObjectAnimator.ofFloat(musicCover, "rotation", 0f, 360f);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setDuration(8000);
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);

    }


    @SuppressLint("HandlerLeak")
    final Handler mHandler =new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            int TimeNow= 0;
            int totaltime=0;
            int isPlay = 0;
            try{
                int code = 104;
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                mBinder.transact(code, data, reply, 0);
                TimeNow = reply.readInt();
                totaltime= reply.readInt();
                isPlay= reply.readInt();
            }catch (RemoteException e){
                e.printStackTrace();
            }
            switch (msg.what){
                case 123:
                    if(isCreate){
                        state.setText("");
                    }
                    else if(isStopped){
                        state.setText("Stopped");
                        playbutton.setText("PLAY");
                        objectAnimator.end();
                    }
                    else if(isPlay == 1){
                        state.setText("Playing");
                        playbutton.setText("PAUSED");
                        if(objectAnimator.isRunning()){
                            objectAnimator.resume();
                        }else{
                            objectAnimator.start();
                        }
                    }
                    else{
                        state.setText("Paused");
                        playbutton.setText("PLAY");
                        objectAnimator.pause();
                    }
                    seekBar.setProgress(TimeNow);
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            currentTime.setText(time.format(i));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            try{
                                int code = 105;
                                Parcel data = Parcel.obtain();
                                Parcel reply = Parcel.obtain();
                                data.writeInt(seekBar.getProgress());
                                mBinder.transact(code, data, reply, 0);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            }

        }
    };

    //线程
    private void MyThread() {
        Thread mThread = new Thread(){
            @Override
            public void run(){
                while (true){
                    try{
                        Thread.sleep(100);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    if(sc != null && hasPermission==true){
                        mHandler.obtainMessage(123).sendToTarget();
                    }
                }
            }
        };
        mThread.start();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unbindService(sc);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode,event);

    }

}
