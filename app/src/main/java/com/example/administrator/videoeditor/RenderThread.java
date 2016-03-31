package com.example.administrator.videoeditor;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.view.Surface;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.opengl.GLES20;


/**
 * Created by Administrator on 2016/3/31.
 */
public class RenderThread extends Thread {
    private volatile RenderHandler mRenderHandler;
    private UIHandler mUIHandler;
    private SurfaceHolder mScreenSurfaceHolder;
    private Surface mOffScreenSurface;
    private Surface mTextureSurface;
    private SurfaceTexture mSurfaceTexture;
    private MediaCodec mDecoder;
    private EglCore mEglCore;
    public volatile boolean constructed = false;

    public RenderThread(UIHandler uiHandler,Surface decoderOutputSurface,MediaCodec decoder,SurfaceHolder holder) {
        mUIHandler = uiHandler;
        mDecoder = decoder;
        mTextureSurface = decoderOutputSurface;
        mScreenSurfaceHolder = holder;
    }

    @Override
    public void run() {
        Looper.prepare();
        mRenderHandler = new RenderHandler(this);
        mEglCore = new EglCore();
        constructed = true;
        Looper.loop();
        mEglCore.release();
    }

    public void surfaceCreated() {
        Surface screenSurface = mScreenSurfaceHolder.getSurface();
        EGLSurface eglSurface = mEglCore.createWindowSurface(screenSurface);
        mEglCore.makeCurrent(eglSurface);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(1, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mEglCore.swapBuffers(eglSurface);
    }

    public RenderHandler getHandler() {
        return mRenderHandler;
    }
}
