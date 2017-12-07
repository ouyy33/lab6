package com.ouyangyi.lab6;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.Nullable;

public class MusicService extends Service {
    public int Totaltime;
    public final IBinder binder = new MyBinder();

    public class MyBinder extends Binder{
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)throws RemoteException{
            switch (code){
                case 101:
                    playAndpause();
                    break;
                case 102:
                    stop();
                    break;
                case 103:
                    quit();
                    break;
                case 104:
                    //界面刷新
                    int CurrentTime = mp.getCurrentPosition();
                    int TotalTime = mp.getDuration();
                    int isPlay=0;
                    if(mp.isPlaying()){
                        isPlay=1;

                    }
                    reply.writeInt(CurrentTime);
                    reply.writeInt(TotalTime);
                    reply.writeInt(isPlay);
                    return true;
                case 105:
                    //拖动进度条
                    TrackingTouch(data.readInt());
                    break;
                case 106:
                    try{
                        mp.setDataSource(Environment.getExternalStorageDirectory()+"/melt.mp3");
                        mp.prepare();
                        mp.setLooping(true);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;

            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    public static MediaPlayer mp = new MediaPlayer();
    public MusicService() {
        try{
            mp.setDataSource(Environment.getExternalStorageDirectory() + "/melt.mp3");
            mp.prepare();
            Totaltime= mp.getDuration();
            mp.setLooping(true);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    //播放按钮
    public void playAndpause(){
        if(mp.isPlaying()){
            mp.pause();
        }else {
            mp.start();
        }
    }
    //停止按钮
    public void stop(){
        if(mp!=null){
            mp.stop();
            try{
                mp.prepare();
                mp.seekTo(0);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //退出按钮
    public  void quit(){
        mp.stop();
        mp.release();
    }
    //注销函数
    public void onDestory(){
        super.onDestroy();
        mp.stop();
        mp.release();
    }
    //拖动进度条
    public void TrackingTouch(int position){
        mp.seekTo(position);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return binder;
    }

}
