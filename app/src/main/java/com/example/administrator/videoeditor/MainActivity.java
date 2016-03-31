package com.example.administrator.videoeditor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private RenderThread mRenderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        sv.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        mRenderThread = new RenderThread(null,null,null,sv.getHolder());    //need to modify
        mRenderThread.start();
        while(!mRenderThread.constructed) {

        }
        RenderHandler renderHandler = mRenderThread.getHandler();
        renderHandler.sendSurfaceCreated();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
