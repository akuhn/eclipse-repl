package nov13.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.events.KeyAdapter;
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
	
	private List<String> inputs = new ArrayList<String>();

	private IOConsoleViewer viewer;

	public SampleView() {
	}

	public void createPartControl(Composite parent) {
		final IOConsole console = new IOConsole("REPL", null);
		viewer = new IOConsoleViewer(parent, console);
		
		final IOConsoleOutputStream out = console.newOutputStream();
		final IOConsoleInputStream in = console.getInputStream();
		
		// hate
		final int suggestionIdx[] = {0};
		final boolean shouldRecordHistory[] = {true};
		
		viewer.getTextWidget().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				String character = ""+e.character;
				if (e.keyCode == SWT.ARROW_UP && !inputs.isEmpty()) {
					try {
						IDocument document = viewer.getDocument();
						int lineIndex = document.getNumberOfLines() - 1;
						int index = suggestionIdx[0] > 0 ? suggestionIdx[0]-- : 0;
						document.replace(document.getLineOffset(lineIndex), document.getLineLength(lineIndex), inputs.get(index));
						putCaretAtBottom(document);
						
						shouldRecordHistory[0] = false;
					} catch (BadLocationException e1) {
						e1.printStackTrace();
					}
				}
				// could be improved, eg deletions should also enter here
				else if (character.matches("[\\w\\s]") && e.keyCode != SWT.CR && e.keyCode != SWT.KEYPAD_CR) {
					shouldRecordHistory[0] = true;
					suggestionIdx[0] = inputs.size() - 1;
				}
				super.keyPressed(e);
			}
		});
		
		Job job = new Job("REPL") {

			DebuggerMagic magic = new DebuggerMagic();
			

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String line = new BufferedReader(new InputStreamReader(in)).readLine();
					magic.evaluate(line, out);
					
					if (shouldRecordHistory[0]) {
						inputs.add(line);
					}
					suggestionIdx[0] = inputs.size() - 1;
					
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

}