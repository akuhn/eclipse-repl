package my.eclipse.repl.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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

public class SampleView extends ViewPart {

	public static final String ID = "my.eclipse.repl.views.SampleView";
	ConsoleViewer viewer;

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new ConsoleViewer(parent);
		configureSourceViewer(viewer);
		final InputStream in = viewer.getInputStream();
		final OutputStream out = viewer.getOutputStream();

		viewer.getDocument().set("new Hello().world(); \"hello\"");

		Job job = new Job("Sample") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String line;
					line = new BufferedReader(new InputStreamReader(in)).readLine();
					new PrintStream(out).println(line);
					line = new BufferedReader(new InputStreamReader(in)).readLine();
					new PrintStream(out).println(line);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}

		};
		job.setSystem(true);
		job.schedule();

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
