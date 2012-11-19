package nov18.views;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.ui.console.IOConsoleInputStream;

public class BullshitFreeIOConsoleInputStream extends InputStream {

	private IOConsoleInputStream in;

	public BullshitFreeIOConsoleInputStream(IOConsoleInputStream in) {
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return in.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		try {
			return in.available();
		} catch (IOException exception) {
			return 0;
		}
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

}
