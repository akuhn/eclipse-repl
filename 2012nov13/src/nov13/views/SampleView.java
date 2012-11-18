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

public class SampleView extends ViewPart {

	public static final String ID = "nov13.views.SampleView";

	private IOConsoleViewer viewer;

	private History history = new History();

	public void createPartControl(Composite parent) {
		final IOConsole console = new IOConsole("REPL", null);
		viewer = new IOConsoleViewer(parent, console);
		viewer.getTextWidget().addKeyListener(history);

		Job job = new Job("REPL") {

			IOConsoleOutputStream out = console.newOutputStream();
			IOConsoleInputStream in = console.getInputStream();
			DebuggerMagic magic = new DebuggerMagic();

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
		};
		job.setSystem(true);
		job.schedule();
	}

	private void updateCaretPositionAfterPrint() {
		viewer.getDocument().addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				IDocument doc = event.getDocument();
				putCaretAtBottom(doc);
				doc.removeDocumentListener(this);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}

	private void putCaretAtBottom(IDocument doc) {
		viewer.setSelectedRange(doc.getLength(), 0);
	}

	public void setFocus() {
		viewer.getControl().setFocus();
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
					putCaretAtBottom(doc);
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