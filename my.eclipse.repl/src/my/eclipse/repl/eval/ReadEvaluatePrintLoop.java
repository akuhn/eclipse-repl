package my.eclipse.repl.eval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ui.progress.UIJob;

public final class ReadEvaluatePrintLoop {

	public final History history;

	private DebuggerMagic magic;
	private InputStream in;
	private PrintStream out;

	public ReadEvaluatePrintLoop(InputStream in, OutputStream out, OutputStream err) {
		this(null, in, out, err);
	}

	public ReadEvaluatePrintLoop(MagicFactory factory, InputStream in, OutputStream out, OutputStream err) {
		this.in = in;
		this.out = new PrintStream(out);
		this.history = new History();
		this.magic = factory == null ? new DebuggerMagic() : factory.makeMagic();
	}

	public void readEvaluatePrint() {
		try {
			out.print(">> ");
			String line = new BufferedReader(new InputStreamReader(in)).readLine();
			history.add(line);
			Result result = magic.evaluate(line);
			out.print("=> ");
			out.println(result.toPrintString());
			notifyListeners(result);
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

	public class History {

		private List<String> history = new ArrayList();
		private int index = 0;

		private void add(String line) {
			history.add(line);
			index = history.size();
		}

		public String previous() {
			if (index > 0) index--;
			return history.get(index);
		}

		public String next() {
			if (index < history.size()) index++;
			if (index == history.size()) return "";
			return history.get(index);
		}

	}

	public IContentAssistProcessor getContentAssitentProvide() {
		return magic.getContentAssistProcessor();
	}

	private ListenerList listeners;

	public void addEvaluationListener(EvaluationListener listener) {
		if (listeners == null) listeners = new ListenerList();
		listeners.add(listener);
	}

	public void removeEvaluationListener(EvaluationListener listener) {
		if (listeners == null) return;
		listeners.remove(listener);
	}

	private void notifyListeners(final Result event) {
		if (listeners == null) return;
		new UIJob("") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				for (Object each: listeners.getListeners()) {
					((EvaluationListener) each).notify(event);
				}
				return Status.OK_STATUS;
			}
		};
	}

}