package my.eclipse.repl.views;

import java.io.InputStream;
import java.io.OutputStream;

import my.eclipse.repl.eval.ReadEvaluatePrintLoop;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.display.DisplayViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class JavaConsolePart {

	private ConsoleViewer viewer;
	private ReadEvaluatePrintLoop repl;

	private IHandlerService service;
	private IHandlerActivation activation;

	public void createPartControl(Composite parent) {
		viewer = new ConsoleViewer(parent);
		InputStream in = viewer.getInputStream();
		OutputStream out = viewer.getOutputStream();
		repl = new ReadEvaluatePrintLoop(in, out, out);
		configureViewer(viewer, repl);
		repl.asJob().schedule();
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void dispose() {
		deactiveHandler();
		repl.dispose();
	}

	private void configureViewer(ISourceViewer viewer, ReadEvaluatePrintLoop repl2) {
		Document doc = new Document();
		viewer.setDocument(doc);
		JavaTextTools tools = JDIDebugUIPlugin.getDefault().getJavaTextTools();
		tools.setupJavaDocumentPartitioner(doc);
		configureHistoryKeyListener();
		configureContentAssistant();
		activateHandler();
	}

	private void configureHistoryKeyListener() {
		viewer.appendVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey(VerifyEvent e) {
				if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
					boolean down = e.keyCode == SWT.ARROW_DOWN;
					String line = down ? repl.history.next() : repl.history.previous();
					viewer.replaceLastLine(line);
					e.doit = false;
				}
			}

		});
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

	private void configureContentAssistant() {
		viewer.configure(new DisplayViewerConfiguration() {
			@Override
			public IContentAssistProcessor getContentAssistantProcessor() {
				return repl.getContentAssitentProvide();
			}
		});
	}

}