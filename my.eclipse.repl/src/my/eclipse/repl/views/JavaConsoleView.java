package my.eclipse.repl.views;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class JavaConsoleView extends ViewPart {

	public static final String ID = "my.eclipse.repl.views.JavaREPL";

	private ConsoleViewer viewer;
	private ReadEvaluatePrintLoop repl;

	private IHandlerService service;

	private IHandlerActivation activation;

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
		deactiveHandler();
		repl.dispose();
		super.dispose();
	}

	private void configureSourceViewer(ISourceViewer viewer) {
		Document doc = new Document();
		viewer.setDocument(doc);
		JavaTextTools tools = JDIDebugUIPlugin.getDefault().getJavaTextTools();
		tools.setupJavaDocumentPartitioner(doc);
		activateHandler();
	}

	private void activateHandler() {
		IHandler handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws org.eclipse.core.commands.ExecutionException {
				viewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
				return null;
			}
		};
		IWorkbench workbench = PlatformUI.getWorkbench();
		service = (IHandlerService) workbench.getAdapter(IHandlerService.class);
		activation = service.activateHandler(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, handler);
	}

	private void deactiveHandler() {
		service.deactivateHandler(activation);
	}

}