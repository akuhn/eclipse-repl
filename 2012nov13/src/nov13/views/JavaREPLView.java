package nov13.views;

import nov13.BullshitFree;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;

public class JavaREPLView extends ViewPart {

	public static final String ID = "nov13.views.SampleView";

	private IOConsoleViewer viewer;

	public void createPartControl(Composite parent) {
		IOConsole console = new IOConsole("REPL", null);
		ReadEvaluatePrintLoop repl = new ReadEvaluatePrintLoop(this, console);
		viewer = new IOConsoleViewer(parent, console);
		viewer.getTextWidget().addKeyListener(repl.history);
		repl.schedule();
	}

	void updateCaretPositionAfterPrint() {
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

	public void setCaretToEndOfDocument() {
		viewer.getTextWidget().setCaretOffset(Integer.MAX_VALUE);
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void dispose() {
		// TODO Find a way to stop job, job#cancel is not doing it.
		super.dispose();
	}

	public void replaceLastLine(String line) {
		try {
			IDocument doc = viewer.getDocument();
			int last = doc.getNumberOfLines() - 1;
			doc.replace(doc.getLineOffset(last), doc.getLineLength(last), line);
			setCaretToEndOfDocument();
		} catch (BadLocationException exception) {
			throw new BullshitFree(exception);
		}
	}

}