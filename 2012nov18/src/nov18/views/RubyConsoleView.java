package nov18.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.jruby.Ruby;

public class RubyConsoleView extends ViewPart {

	public static final String ID = "nov18.views.SampleView";

	private IOConsoleViewer viewer;

	private SourceViewer viewer2;

	boolean isUpdating = false;

	public void createPartControl(Composite parent) {
		viewer2 = new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
		final IDocument doc = new Document();

		final MyInputStream in = new MyInputStream();
		final PrintStream out = new PrintStream(new OutputStream() {

			@Override
			public void write(final int b) throws IOException {
				Job job = new UIJob("XXX") {

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						try {
							isUpdating = true;
							doc.replace(doc.getLength(), 0, new String(new char[] { (char) b }));
							viewer2.getTextWidget().setCaretOffset(Integer.MAX_VALUE);
						} catch (BadLocationException e) {
							e.printStackTrace();
						} finally {
							isUpdating = false;
						}
						return Status.OK_STATUS;
					}

				};
				job.schedule();

			}

		});

		viewer2.setDocument(doc);
		viewer2.addTextListener(new ITextListener() {

			@Override
			public void textChanged(TextEvent event) {
				if (!isUpdating) in.appendData(event.getText().getBytes());
			}
		});

		Job job = new Job("IRB") {
			protected IStatus run(IProgressMonitor monitor) {

				Ruby ruby = Ruby.newInstance(in, out, out);

				StringBuilder buf = new StringBuilder();
				buf.append("require 'irb'\n");
				buf.append("require 'irb/completion'\n");
				buf.append("ARGV << '--prompt' << 'default' << '--noverbose' << '--readline'\n");
				buf.append("def STDIN.isatty; true; end\n");
				buf.append("module IRB; class Context; def prompting?; true; end; end; end\n");
				buf.append("IRB.start\n");
				buf.append("\n");

				ruby.evalScriptlet(buf.toString());

				return Status.OK_STATUS;
			}

		};
		job.setSystem(true);
		job.schedule();

	}

	public void setFocus() {
		viewer2.getControl().setFocus();
	}

	public class MyInputStream extends InputStream {

		private byte[] buf = new byte[100];
		private int pos = 0;
		private int limit = 0;

		@Override
		public synchronized int read() throws IOException {
			while (pos == limit) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			return buf[pos++];
		}

		public synchronized void appendData(byte[] data) {
			if (limit + data.length > buf.length) { throw new Error(); }
			for (byte each: data) {
				buf[limit++] = each;
			}
			notifyAll();
		}

		@Override
		public int available() throws IOException {
			return limit - pos;
		}

	}

}