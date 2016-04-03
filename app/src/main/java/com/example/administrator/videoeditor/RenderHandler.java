package com.example.administrator.videoeditor;


import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by Administrator on 2016/3/31.
 */
public class RenderHandler extends Handler {
    private static final int MSG_SURFACE_CREATED = 0;
    private static final int MSG_SURFACE_CHANGED = 1;
    private static final int MSG_START_RENDER = 2;

    private WeakReference<RenderThread> mWeakRenderThread;


    public RenderHandler(RenderThread renderThread) {
        mWeakRenderThread = new WeakReference<RenderThread>(renderThread);
    }

    public void sendSurfaceCreated() {
        sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CREATED));
    }
    public void sendSurfaceChanged(int width,int height) {
        sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CHANGED,width,height));
    }
    public void sendStartRender() {
        sendMessage(obtainMessage(RenderHandler.MSG_START_RENDER));
    }

    @Override
    public void handleMessage(Message msg) {
        int what = msg.what;
        RenderThread renderThread = mWeakRenderThread.get();
        if (renderThread == null) {
            return;
        }
        switch (what) {
            case MSG_SURFACE_CREATED:
                renderThread.surfaceCreated();
                break;
            case MSG_SURFACE_CHANGED:
                renderThread.surfaceChanged(msg.arg1,msg.arg2);
                break;
            case MSG_START_RENDER:
                renderThread.prepareNextFrame();
                break;
        }
    }
}
