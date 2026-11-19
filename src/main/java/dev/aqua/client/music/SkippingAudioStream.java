package dev.aqua.client.music;

import net.minecraft.client.sound.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Wraps a decoded stream and discards PCM data up to a start offset (used to resume playback). */
final class SkippingAudioStream implements AudioStream {
    private final AudioStream delegate;
    private long bytesToSkip;

    SkippingAudioStream(AudioStream delegate, long offsetMs) {
        this.delegate = delegate;
        AudioFormat f = delegate.getFormat();
        long frameBytes = (long) f.getChannels() * Math.max(1, f.getSampleSizeInBits() / 8);
        long frames = (long) (f.getSampleRate() * offsetMs / 1000.0);
        this.bytesToSkip = frames * frameBytes;
    }

    @Override
    public AudioFormat getFormat() {
        return delegate.getFormat();
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        while (bytesToSkip > 0) {
            ByteBuffer b = delegate.read((int) Math.min(65536, Math.max(4096, bytesToSkip)));
            if (b == null || b.remaining() == 0) {
                bytesToSkip = 0;
                return b;
            }
            int n = b.remaining();
            if (n <= bytesToSkip) {
                bytesToSkip -= n;
                continue;
            }
            b.position(b.position() + (int) bytesToSkip);
            bytesToSkip = 0;
            ByteBuffer copy = ByteBuffer.allocateDirect(b.remaining());
            copy.put(b);
            copy.flip();
            return copy;
        }
        return delegate.read(size);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
