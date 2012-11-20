package nov18.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

public class RubyConsoleView extends ViewPart {

	public static final String ID = "nov18.views.SampleView";

	private IOConsoleViewer viewer;

	public void createPartControl(Composite parent) {
		IOConsole console = new IOConsole("JRuby Console", null);
		viewer = new IOConsoleViewer(parent, console);

		final ScriptingContainer con = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
		final InputStream in = console.getInputStream();

		con.setOutput(new PrintStream(console.newOutputStream()));
		con.setError(new PrintStream(console.newOutputStream()));

		Job job = new Job("JRuby Console REPL") {
			protected IStatus run(IProgressMonitor monitor) {

				while (!monitor.isCanceled()) {
					String line;
					try {
						line = new BufferedReader(new InputStreamReader(in)).readLine();
						Object object = con.runScriptlet(line);
						con.put("_", object);
						con.runScriptlet("print '=> '; p _;");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				return Status.OK_STATUS;
			}

		};
		job.setSystem(true);
		job.schedule();

	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

}