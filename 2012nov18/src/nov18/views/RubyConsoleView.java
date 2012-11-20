package nov18.views;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;

public class RubyConsoleView extends ViewPart {

	public static final String ID = "nov18.views.SampleView";

	private IOConsoleViewer viewer;
	private ReadEvaluatePrintLoop repl;

	public void createPartControl(Composite parent) {
		IOConsole console = new IOConsole("JRuby Console", null);
		viewer = new IOConsoleViewer(parent, console);
		updateCaretWhenDocumentUpdates(viewer);
		repl = new ReadEvaluatePrintLoop(console.getInputStream(), console.newOutputStream(), console.newOutputStream());
		repl.asJob().schedule();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		repl.dispose();
		super.dispose();
	}

	private void updateCaretWhenDocumentUpdates(final IOConsoleViewer viewer) {
		viewer.getDocument().addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				viewer.getTextWidget().setCaretOffset(Integer.MAX_VALUE);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}

}