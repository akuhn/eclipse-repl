package my.eclipse.repl.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import my.eclipse.repl.DebuggerMagic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

final class ReadEvaluatePrintLoop {

	private DebuggerMagic magic;

	private InputStream in;
	private PrintStream out;
	private History history;

	public ReadEvaluatePrintLoop(InputStream in, OutputStream out, OutputStream err) {
		this.in = in;
		this.out = new PrintStream(out);
		this.history = new History();
	}

	private void initialize() {
		magic = new DebuggerMagic();
	}

	public void readEvaluatePrint() {
		try {
			out.print(">> ");
			String line = new BufferedReader(new InputStreamReader(in)).readLine();
			history.add(line);
			if (magic == null) initialize();
			String result = magic.evaluate(line);
			out.print("=> ");
			out.println(result);
		} catch (IOException ex) {
			// ASSUME input stream just closed
		}
	}

	public Job asJob() {
		Job job = new Job("JRuby REPL") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				while (!monitor.isCanceled()) {
					readEvaluatePrint();
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		return job;
	}

	public void dispose() {
		try {
			in.close();
		} catch (IOException e) {
			// ASSUME redundant
		}
	}

	class History {

		private List<String> list = new ArrayList();
		private int index = 0;

		public void add(String line) {
			list.add(line);
			index = list.size();
		}

		public String previous() {
			if (index > 0) index--;
			return list.get(index);
		}

		public String next() {
			if (index < list.size()) index++;
			if (index == list.size()) return "";
			return list.get(index);
		}

	}

	public KeyListener asKeyListener(final ConsoleViewer viewer) {
		return new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
					String line = e.keyCode == SWT.ARROW_UP ? history.previous() : history.next();
					viewer.replaceLastLine(line);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		};
	}

	public void connect(ConsoleViewer viewer) {
		viewer.getTextWidget().addKeyListener(asKeyListener(viewer));

	}

}