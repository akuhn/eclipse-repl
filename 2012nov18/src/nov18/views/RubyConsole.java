package nov18.views;

import java.io.InputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.IOConsole;
import org.jruby.Ruby;

public class RubyConsole extends IOConsole {

	public RubyConsole() {
		super("Ruby Console", null);

		Job job = new Job("IRB") {
			protected IStatus run(IProgressMonitor monitor) {

				InputStream in = new BullshitFreeIOConsoleInputStream(getInputStream());
				PrintStream out = new PrintStream(newOutputStream());
				PrintStream err = new PrintStream(newOutputStream());
				Ruby ruby = Ruby.newInstance(in, out, err);

				StringBuilder buf = new StringBuilder();
				buf.append("require 'irb'\n");
				buf.append("ARGV << '--prompt' << 'default' << '--noverbose'\n");
				buf.append("module IRB; class Context; def prompting?; true; end; end; end\n");
				buf.append("IRB.start\n");
				buf.append("\n");

				ruby.evalScriptlet(buf.toString());
				return Status.OK_STATUS;
			}

		};
		job.setSystem(true);
		job.schedule();

	}

}
