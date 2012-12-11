package my.eclipse.repl.jruby.handlers;

import my.eclipse.repl.jruby.BullshitFree;
import my.eclipse.repl.jruby.views.RubyConsoleView;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class OpenEclipseJRubyREPLHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().showView(RubyConsoleView.ID);
		} catch (PartInitException exception) {
			throw new BullshitFree(exception);
		}
		return null;
	}

}
