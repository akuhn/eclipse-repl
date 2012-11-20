package my.eclipse.repl.jruby.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ParseFailedException;
import org.jruby.embed.ScriptingContainer;

class ReadEvaluatePrintLoop {

	private InputStream in;
	private OutputStream out;
	private OutputStream err;
	private ScriptingContainer ruby;
	private History history;

	ReadEvaluatePrintLoop(InputStream in, OutputStream out, OutputStream err) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.history = new History();
	}

	private void initialize() {
		ruby = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
		ruby.put("$stin", in);
		ruby.put("$stdout", out);
		ruby.put("$stderr", err);
		ruby.runScriptlet("require 'java'");
	}

	public void readEvaluatePrint() {
		try {
			String line = new BufferedReader(new InputStreamReader(in)).readLine();
			history.add(line);
			if (ruby == null) initialize();
			Object object = ruby.runScriptlet(line);
			ruby.put("_", object);
			String result = "=> " + ruby.runScriptlet("_.inspect") + "\n";
			out.write(result.getBytes());
		} catch (EvalFailedException | ParseFailedException ex) {
			// ASSUME script already printed the error
		} catch (IOException ex) {
			// ASSUME input stream just closed
		}
	}

	public void dispose() {
		try {
			in.close();
		} catch (IOException e) {
			// ASSUME redundant
		}
	}

	public Job asJob() {
		Job job = new Job("JRuby REPL") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				while (!monitor.isCanceled()) {
					readEvaluatePrint();
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		return job;
	}

	public KeyListener asKeyListener(final RubyConsoleView view) {
		return new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
					String line = e.keyCode == SWT.ARROW_UP ? history.previous() : history.next();
					view.replaceLastLine(line);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		};
	}
}