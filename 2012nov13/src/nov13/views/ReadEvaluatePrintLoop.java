package nov13.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nov13.DebuggerMagic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;

final class ReadEvaluatePrintLoop extends Job {

	private IOConsoleOutputStream out;
	private IOConsoleInputStream in;
	private DebuggerMagic magic;
	private JavaREPLView view;

	public History history;

	public ReadEvaluatePrintLoop(JavaREPLView view, IOConsole console) {
		super("REPL");
		this.setSystem(true);
		this.view = view;
		out = console.newOutputStream();
		in = console.getInputStream();
		magic = new DebuggerMagic();
		history = new History();
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			String line = new BufferedReader(new InputStreamReader(in)).readLine();
			magic.evaluate(line, out);
			history.add(line);
			view.updateCaretPositionAfterPrint();
			this.schedule();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Status.OK_STATUS;
	}

	class History implements KeyListener {

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

	}

}