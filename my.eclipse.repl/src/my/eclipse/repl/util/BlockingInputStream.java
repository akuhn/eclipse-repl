package my.eclipse.repl.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Input stream backed by a byte buffer. When buffer empty, blocks and waits for
 * more data. Use {@link #append(String)} to append more data.
 * 
 * @author Adrian Kuhn
 * 
 */
public class BlockingInputStream extends InputStream {

	Lock lock = new ReentrantLock();
	Condition isAvailable = lock.newCondition();

	byte[] data = new byte[100];
	int pos = 0;
	int limit = 0;

	public void append(String text) {
		append(text.getBytes());
	}

	public void append(byte[] more) {
		lock.lock();
		try {
			int length = limit + more.length;
			if (length > data.length) needMore(Math.max(data.length * 2, length));
			System.arraycopy(more, 0, data, limit, more.length);
			limit = length;
			isAvailable.signalAll();
		} finally {
			lock.unlock();
		}
	}

	private void needMore(int length) {
		assert length > data.length;
		byte[] copy = new byte[length];
		System.arraycopy(data, 0, copy, 0, limit);
		data = copy;
	}

	@Override
	public int available() throws IOException {
		return limit - pos;
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int len = read(b, 0, 1);
		assert len > 0;
		return b[0];
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		lock.lock();
		try {
			waitingForData();
			int read = Math.min(len, limit - pos);
			System.arraycopy(data, pos, b, off, read);
			pos += read;
			rewindEmptyBuffer();
			return read;
		} finally {
			lock.unlock();
		}
	}

	private void waitingForData() {
		while (pos == limit) {
			try {
				isAvailable.await();
			} catch (InterruptedException e) {
				// These are not the droids you're looking for!
			}
		}
	}

	private void rewindEmptyBuffer() {
		if (pos == limit) pos = limit = 0;
	}

}