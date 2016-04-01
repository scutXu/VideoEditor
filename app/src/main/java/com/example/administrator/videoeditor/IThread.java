package com.example.administrator.videoeditor;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2016/3/31.
 */
public class IThread extends Thread {
    private MediaExtractor mMediaExtractor;
    private MediaCodec mDecoder;
    private MediaFormat mVideoFormat;
    private int TIMEOUT_USEC = 10000;
    public IThread(String dataSource) {
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(dataSource);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        int numTrack = mMediaExtractor.getTrackCount();
        int videoTrackIndex = -1;
        for(int i=0;i<numTrack;++i) {
            mVideoFormat = mMediaExtractor.getTrackFormat(i);
            String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("video/")) {
                videoTrackIndex = i;
                break;
            }
        }
        if(videoTrackIndex == -1) {
            Log.i("mytag","video track not found");
        }
        mMediaExtractor.selectTrack(videoTrackIndex);
        String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
        try {
            mDecoder = MediaCodec.createDecoderByType(mime);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //mDecoder.configure(format,outputSurface,null,0);
        //mDecoder.start();
    }
    public void startDecoder(Surface outputSurface) {
        mDecoder.configure(mVideoFormat,outputSurface,null,0);
        mDecoder.start();

    }
    @Override
    public void run() {
        ByteBuffer [] inputBuffers = mDecoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean readFinished = false;
        while(!readFinished) {
            int bufferIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
            if(bufferIndex >= 0) {
                ByteBuffer buffer = inputBuffers[bufferIndex];
                int chunkSize = mMediaExtractor.readSampleData(buffer,0);
                if(chunkSize < 0) {
                    mDecoder.queueInputBuffer(bufferIndex,0,0,0L,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    readFinished = true;
                }
                else {
                    long presentationTimeUs = mMediaExtractor.getSampleTime();
                    mDecoder.queueInputBuffer(bufferIndex,0,chunkSize,presentationTimeUs,0);
                    mMediaExtractor.advance();
                }
            }
        }
    }

    public MediaCodec getDecoder() {
        return mDecoder;
    }

}
