package nov13.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nov13.BullshitFree;
import nov13.DebuggerMagic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;

public class JavaREPLView extends ViewPart {

	public static final String ID = "nov13.views.SampleView";

	private IOConsoleViewer viewer;

	private History history = new History();

	private Job repl;

	public void createPartControl(Composite parent) {
		final IOConsole console = new IOConsole("REPL", null);
		viewer = new IOConsoleViewer(parent, console);
		viewer.getTextWidget().addKeyListener(history);
		repl = new ReadEvaluatePrintLoop("REPL", console);
		repl.setSystem(true);
		repl.schedule();
	}

	private void updateCaretPositionAfterPrint() {
		viewer.getDocument().addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				setCaretToEndOfDocument();
				event.getDocument().removeDocumentListener(this);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}

	private void setCaretToEndOfDocument() {
		viewer.getTextWidget().setCaretOffset(Integer.MAX_VALUE);
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void dispose() {
		// TODO does not seem to stop the job as the job is apparently blocked
		// with waiting for input. Find way to stop job when tearing down.
		repl.cancel();
		super.dispose();
	}

	private final class ReadEvaluatePrintLoop extends Job {
		IOConsoleOutputStream out;
		IOConsoleInputStream in;
		DebuggerMagic magic;

		private ReadEvaluatePrintLoop(String name, IOConsole console) {
			super(name);
			out = console.newOutputStream();
			in = console.getInputStream();
			magic = new DebuggerMagic();
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				String line = new BufferedReader(new InputStreamReader(in)).readLine();
				magic.evaluate(line, out);
				history.add(line);
				updateCaretPositionAfterPrint();
				this.schedule();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return Status.OK_STATUS;
		}
	}

	private class History implements KeyListener {

		private List<String> list = new ArrayList();
		private int index = 0;

		public void add(String line) {
			list.add(line);
			index = list.size();
		}

		public String previous() {
			if (index > 0) index--;
			return list.get(index);
		}

		public String next() {
			if (index < list.size()) index++;
			if (index == list.size()) return "";
			return list.get(index);
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
				try {
					IDocument doc = viewer.getDocument();
					int last = doc.getNumberOfLines() - 1;
					String line = e.keyCode == SWT.ARROW_UP ? history.previous() : history.next();
					doc.replace(doc.getLineOffset(last), doc.getLineLength(last), line);
					setCaretToEndOfDocument();
				} catch (BadLocationException exception) {
					throw new BullshitFree(exception);
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

	}

}