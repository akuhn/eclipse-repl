package my.eclipse.repl.views;

import java.io.IOException;
import java.io.OutputStream;

import my.eclipse.repl.BullshitFree;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.debug.ui.JDISourceViewer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;

class ConsoleViewer extends JDISourceViewer {

	private BlockingInputStream in = new BlockingInputStream();

	private OutputStream out = new OutputStream() {

		@Override
		public void write(byte[] b) throws IOException {
			buf.append(new String(b));
			job.schedule(50);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			buf.append(new String(b, off, len));
			job.schedule(50);
		}

		@Override
		public void write(int b) throws IOException {
			buf.append(new String(new byte[] { (byte) b }));
			job.schedule(50);
		}

	};

	UIJob job = new UIJob("Console Output Stream") {
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			try {
				String output = buf.toString();
				IDocument doc = getDocument();
				doc.replace(doc.getLength(), 0, output);
				buf.setLength(0); // TODO shrink if grown too large
				mark = doc.getLength();
				getTextWidget().invokeAction(ST.TEXT_END);
			} catch (BadLocationException exception) {
				throw new BullshitFree(exception);
			}
			return Status.OK_STATUS;
		}
	};

	private int mark = 0;

	StringBuilder buf = new StringBuilder();

	public ConsoleViewer(Composite parent) {
		super(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
		initializeVerifyListener();
	}

	private void initializeVerifyListener() {
		getTextWidget().addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				try {
					if (event.text.equals("\n")) {
						appendAtTheEnd(event.text);
						String input = advanceMark();
						in.append(input);
						event.doit = false;
					} else if (event.start < mark) {
						appendAtTheEnd(event.text);
						event.doit = false;
					}
				} catch (BadLocationException exception) {
					throw new BullshitFree(exception);

				}
			}
		});
	}

	private String advanceMark() throws BadLocationException {
		IDocument doc = getDocument();
		int length = doc.getLength();
		String input = doc.get(mark, length - mark);
		mark = length;
		return input;
	}

	private void appendAtTheEnd(String text) throws BadLocationException {
		IDocument doc = getDocument();
		doc.replace(doc.getLength(), 0, text);
		getTextWidget().invokeAction(ST.TEXT_END);
	}

	public BlockingInputStream getInputStream() {
		return in;
	}

	public OutputStream getOutputStream() {
		return out;
	}

}