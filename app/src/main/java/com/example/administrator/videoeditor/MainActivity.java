package com.example.administrator.videoeditor;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private RenderThread mRenderThread;
    private IThread mIThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("mytag","click");
                mRenderThread.getHandler().sendDoFrame();
            }
        });
        sv.getHolder().addCallback(this);

        String path  = Environment.getExternalStorageDirectory().getAbsolutePath();
        mIThread = new IThread(path + File.separator + "1.mp4");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        mRenderThread = new RenderThread(null,mIThread.getDecoder(),sv.getHolder());
        mRenderThread.start();
        while(!mRenderThread.constructed) {

        }
        RenderHandler renderHandler = mRenderThread.getHandler();
        renderHandler.sendSurfaceCreated();
        while(mRenderThread.getmSurfaceTexture() == null) {
            //wait until surfaceTexture constructed
        }
        Surface  surface = new Surface(mRenderThread.getmSurfaceTexture());
        mIThread.startDecoder(surface);
        mIThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mRenderThread.getHandler().sendSurfaceChanged(width,height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
