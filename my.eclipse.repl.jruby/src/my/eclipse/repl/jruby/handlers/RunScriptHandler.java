package my.eclipse.repl.jruby.handlers;

import java.io.InputStream;
import java.io.PrintStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.jruby.embed.ScriptingContainer;

public class RunScriptHandler extends AbstractHandler {

	private MessageConsole findConsole(String name) {
		IConsoleManager man = ConsolePlugin.getDefault().getConsoleManager();
		for (IConsole each: man.getConsoles()) {
			if (name.equals(each.getName())) return (MessageConsole) each;
		}
		MessageConsole console = new MessageConsole(name, null);
		man.addConsoles(new IConsole[] { console });
		return console;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("example");
			InputStream in = project.getFile("/scripts/example.rb").getContents();

			MessageConsole m = findConsole("Ruby Script Output");

			ScriptingContainer con = new ScriptingContainer();
			con.setOutput(new PrintStream(m.newOutputStream()));
			con.setError(new PrintStream(m.newOutputStream()));
			con.runScriptlet(in, "example.rb");

		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		return null;
	}
}
