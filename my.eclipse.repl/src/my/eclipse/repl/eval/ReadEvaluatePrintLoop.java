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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;

public final class ReadEvaluatePrintLoop {

	public final History history;

	private DebuggerMagic magic;
	private InputStream in;
	private PrintStream out;

	public ReadEvaluatePrintLoop(InputStream in, OutputStream out, OutputStream err) {
		this(null, in, out, err);
	}

	public ReadEvaluatePrintLoop(DebuggerMagic magic, InputStream in, OutputStream out, OutputStream err) {
		this.in = in;
		this.out = new PrintStream(out);
		this.history = new History();
		this.magic = magic == null ? new DebuggerMagic() : magic;
	}

	public void readEvaluatePrint() {
		try {
			out.print(">> ");
			String line = new BufferedReader(new InputStreamReader(in)).readLine();
			history.add(line);
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

}