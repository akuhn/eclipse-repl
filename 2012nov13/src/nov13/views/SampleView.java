package nov13.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import nov13.DebuggerMagic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;

public class SampleView extends ViewPart {

	public static final String ID = "nov13.views.SampleView";

	private IOConsoleViewer viewer;

	public SampleView() {
	}

	public void createPartControl(Composite parent) {
		final IOConsole console = new IOConsole("REPL", null);
		viewer = new IOConsoleViewer(parent, console);

		Job job = new Job("REPL") {

			IOConsoleOutputStream out = console.newOutputStream();
			IOConsoleInputStream in = console.getInputStream();

			DebuggerMagic magic = new DebuggerMagic();

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String line = new BufferedReader(new InputStreamReader(in)).readLine();
					magic.evaluate(line, out);
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
				viewer.setSelectedRange(doc.getLength(), 0);
				doc.removeDocumentListener(this);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

}