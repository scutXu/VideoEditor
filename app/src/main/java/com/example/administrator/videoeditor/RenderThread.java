package com.example.administrator.videoeditor;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Surface;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.opengl.GLES20;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by Administrator on 2016/3/31.
 */
public class RenderThread extends Thread {
    private volatile RenderHandler mRenderHandler;
    private UIHandler mUIHandler;
    private SurfaceHolder mScreenSurfaceHolder;
    private Surface mOffScreenSurface;
    private EGLSurface mEglScreenSurface;
    private volatile SurfaceTexture mSurfaceTexture = null;
    private MediaCodec mDecoder;
    private EglCore mEglCore;
    public volatile boolean constructed = false;
    private int mPositionAttribLocation;
    private FloatBuffer mVertices;
    private int mTexture;
    private int mShaderProgram;
    private int mCachedBufferIndex;
    private boolean isReadFinished = false;
    private long lastPTS = -1;
    private long lastRenderStartTime = -1;
    private Runnable mDrawRunnable;

    private long timeRecord;
    private boolean firstRender = true;

    public RenderThread(UIHandler uiHandler,MediaCodec decoder,SurfaceHolder holder) {
        mUIHandler = uiHandler;
        mDecoder = decoder;
        mScreenSurfaceHolder = holder;
        mDrawRunnable = new Runnable() {
            @Override
            public void run() {
                lastRenderStartTime = System.nanoTime();
                mDecoder.releaseOutputBuffer(mCachedBufferIndex, true);
            }
        };
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
        mEglScreenSurface = mEglCore.createWindowSurface(screenSurface);
        mEglCore.makeCurrent(mEglScreenSurface);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0, 0, 0, 1);

        createProgram();

        float [] vertices = new float[] {
                -1,-1,
                1,-1,
                1,1,
                -1,1
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertices = bb.asFloatBuffer();
        mVertices.put(vertices).position(0);

        /*GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnableVertexAttribArray(mPositionAttribLocation);
        GLES20.glVertexAttribPointer(mPositionAttribLocation, 2, GLES20.GL_FLOAT, false, 0, mVertices);
         GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        mEglCore.swapBuffers(eglSurface);*/

        createTexture();
        int textureUniformLocation = GLES20.glGetUniformLocation(mShaderProgram,"frameTexture");
        GLES20.glUniform1i(textureUniformLocation, 0);
    }

    public void surfaceChanged(int width,int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public static int createShader(int type,String text) {
        int shader = GLES20.glCreateShader(type);
        int [] compileStatus = new int[1];
        GLES20.glShaderSource(shader, text);
        GLES20.glCompileShader(shader);
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if(compileStatus[0] != GLES20.GL_TRUE) {
            Log.e("mytag", "compile shader fail");
            String info = GLES20.glGetShaderInfoLog(shader);
            Log.e("mytag",text);
            Log.e("mytag",info);
            GLES20.glDeleteShader(shader);
        }
        return  shader;
    }

    private void createTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);   //unnecessary to invoke if there is only one texture unit
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        mTexture = textures[0];
        mSurfaceTexture = new SurfaceTexture(mTexture);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                surfaceTexture.updateTexImage();
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glEnableVertexAttribArray(mPositionAttribLocation);
                GLES20.glVertexAttribPointer(mPositionAttribLocation, 2, GLES20.GL_FLOAT, false, 0, mVertices);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
                //Log.i("mytag","draw frame");
                mEglCore.swapBuffers(mEglScreenSurface);

                if(firstRender) {
                    firstRender = false;
                    timeRecord = System.nanoTime();
                }

                if(isReadFinished) {
                    //do some shut down job
                    //Log.i("mytag","render finished");
                    Log.i("mytag","total time" + Long.toString((System.nanoTime() - timeRecord)/1000000L));
                }
                else {
                    prepareNextFrame();
                }
            }
        });
    }

    private void createProgram() {
        String text = "attribute vec2 aPosition;\n" +
                      "varying vec2 uv;\n" +
                      "void main() {\n" +
                            "uv = vec2(aPosition.x,-aPosition.y);\n" +
                            "uv = (uv + vec2(1,1)) / 2.0;\n" +
                            "gl_Position = vec4(aPosition,0,1);\n" +
                       "}\n";
        int vertexShader = createShader(GLES20.GL_VERTEX_SHADER,text);
        text = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES frameTexture;\n" +
                "varying vec2 uv;\n" +
                "void main() {\n" +
                    "gl_FragColor = vec4(texture2D(frameTexture,uv).xyz,1);\n" +
                "}\n";
        int fragmentShader = createShader(GLES20.GL_FRAGMENT_SHADER, text);
        mShaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mShaderProgram, vertexShader);
        GLES20.glAttachShader(mShaderProgram, fragmentShader);
        GLES20.glLinkProgram(mShaderProgram);
        GLES20.glUseProgram(mShaderProgram);
        mPositionAttribLocation = GLES20.glGetAttribLocation(mShaderProgram, "aPosition");

    }

    public void prepareNextFrame() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while(true) {
            int bufferIndex = mDecoder.dequeueOutputBuffer(info, -1L);
            if(bufferIndex >= 0) {
                //boolean doRender = ((info.size != 0) && ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0));
                boolean doRender = (info.size != 0);
                boolean eos = ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);
                if(doRender) {
                    mCachedBufferIndex = bufferIndex;
                    if(eos) {
                        //Log.i("mytag","eos with frame");
                        isReadFinished = true;
                    }
                    else {
                        //add time event
                        if(lastPTS < 0) {
                            //Log.i("mytag","first frame");
                            mRenderHandler.post(mDrawRunnable);
                        }
                        else {
                            long delay = ((info.presentationTimeUs - lastPTS) - ((System.nanoTime() - lastRenderStartTime)/1000L))/1000L;
                            //Log.i("mytag",Long.toString(delay));
                            if(delay <= 0) {
                                mRenderHandler.post(mDrawRunnable);
                            }
                            else {
                                mRenderHandler.postDelayed(mDrawRunnable, delay);
                                //mRenderHandler.post(mDrawRunnable);
                            }
                        }
                        lastPTS = info.presentationTimeUs;
                    }
                    break;
                }
                else {
                    mDecoder.releaseOutputBuffer(bufferIndex,false);
                    if(eos) {
                        //end thread
                        Log.i("mytag","total time:" + Long.toString(System.nanoTime() - timeRecord));
                        Log.i("mytag","eos without frame");
                        break;
                    }
                }
            }
            else {
                if(bufferIndex != MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //Log.i("mytag","error buffer index:" + Integer.toString(bufferIndex));
                }
            }
        }
    }
    public RenderHandler getHandler() {
        return mRenderHandler;
    }
    public SurfaceTexture getmSurfaceTexture() {
        return mSurfaceTexture;
    }
}
