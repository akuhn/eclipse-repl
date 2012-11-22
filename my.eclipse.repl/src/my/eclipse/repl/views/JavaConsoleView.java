package my.eclipse.repl.views;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.contentassist.CurrentFrameContext;
import org.eclipse.jdt.internal.debug.ui.contentassist.JavaDebugContentAssistProcessor;
import org.eclipse.jdt.internal.debug.ui.display.DisplayViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class JavaConsoleView extends ViewPart {

	public static final String ID = "my.eclipse.repl.views.JavaREPL";

	private ConsoleViewer viewer;
	private ReadEvaluatePrintLoop repl;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new ConsoleViewer(parent);
		configureSourceViewer(viewer);
		InputStream in = viewer.getInputStream();
		OutputStream out = viewer.getOutputStream();
		repl = new ReadEvaluatePrintLoop(in, out, out);
		repl.connect(viewer);
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

	private void configureSourceViewer(ISourceViewer viewer) {
		Document doc = new Document();
		viewer.setDocument(doc);

		JavaTextTools tools = JDIDebugUIPlugin.getDefault().getJavaTextTools();
		tools.setupJavaDocumentPartitioner(doc);
		viewer.configure(new DisplayViewerConfiguration() {
			@Override
			public IContentAssistProcessor getContentAssistantProcessor() {
				return new JavaDebugContentAssistProcessor(new CurrentFrameContext());
			}
		});
	}

}