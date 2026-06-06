import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayAudio {

    public static void main(String[] args) {
        try {
            int streamType = AudioManager.STREAM_MUSIC;
            String path;

            if (args.length == 1) {
                path = args[0];
            } else if (args.length == 3 && "-s".equals(args[0])) {
                String streamName = args[1];

                if ("alarm".equals(streamName)) {
                    streamType = AudioManager.STREAM_ALARM;
                } else if ("media".equals(streamName)) {
                    streamType = AudioManager.STREAM_MUSIC;
                } else if ("notification".equals(streamName)) {
                    streamType = AudioManager.STREAM_NOTIFICATION;
                } else if ("ring".equals(streamName)) {
                    streamType = AudioManager.STREAM_RING;
                } else if ("system".equals(streamName)) {
                    streamType = AudioManager.STREAM_SYSTEM;
                } else if ("voice".equals(streamName)) {
                    streamType = AudioManager.STREAM_VOICE_CALL;
                } else {
                    usage();
                    System.exit(1);
                    return;
                }

                path = args[2];
            } else {
                usage();
                System.exit(1);
                return;
            }

            play(path, streamType);

            System.exit(0);
        } catch (Throwable t) {
            String message = t.getMessage();

            if (message != null && !message.isEmpty()) {
                System.err.println(message);
            } else {
                System.err.println(t.getClass().getName());
            }

            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println("Usage: PlayAudio [-s streamtype] <audio-file>");
        System.err.println();
        System.err.println("The audio stream type (which affects the volume)");
        System.err.println("may be specified as:");
        System.err.println("    alarm");
        System.err.println("    media (default)");
        System.err.println("    notification");
        System.err.println("    ring");
        System.err.println("    system");
        System.err.println("    voice");
    }

    private static void play(String path, int streamType) throws Exception {
        MediaExtractor extractor = new MediaExtractor();

        try {
            extractor.setDataSource(path);
        } catch (Throwable e) {
            throw new IOException("Cannot open audio file: " + path);
        }

        int trackIndex = -1;
        MediaFormat format = null;

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat f = extractor.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);

            if (mime != null && mime.startsWith("audio/")) {
                trackIndex = i;
                format = f;
                break;
            }
        }

        if (trackIndex < 0) {
            throw new IOException("No audio track found");
        }

        extractor.selectTrack(trackIndex);

        String mime = format.getString(MediaFormat.KEY_MIME);

        MediaCodec codec = MediaCodec.createDecoderByType(mime);

        codec.configure(format, null, null, 0);
        codec.start();

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        int channelConfig = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

        int minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack track = new AudioTrack(streamType, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBuffer, AudioTrack.MODE_STREAM);

        track.play();

        boolean inputEOS = false;
        boolean outputEOS = false;

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (!outputEOS) {
            if (!inputEOS) {
                int inputIndex = codec.dequeueInputBuffer(10000);

                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);

                    int sampleSize = extractor.readSampleData(inputBuffer, 0);

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputEOS = true;
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            int outputIndex = codec.dequeueOutputBuffer(info, 10000);

            if (outputIndex >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);

                if (outputBuffer != null && info.size > 0) {
                    outputBuffer.position(info.offset);
                    outputBuffer.limit(info.offset + info.size);

                    byte[] pcm = new byte[info.size];

                    outputBuffer.get(pcm);

                    track.write(pcm, 0, pcm.length);
                }

                boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                codec.releaseOutputBuffer(outputIndex, false);

                if (eos) {
                    outputEOS = true;
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

            }
        }

        try {
            track.pause();
        } catch (Throwable ignored) {
        }

        try {
            track.flush();
        } catch (Throwable ignored) {
        }

        try {
            track.stop();
        } catch (Throwable ignored) {
        }

        track.release();

        codec.stop();
        codec.release();

        extractor.release();
    }
}
