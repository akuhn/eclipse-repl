package my.eclipse.repl.handlers;

import my.eclipse.repl.util.BullshitFree;
import my.eclipse.repl.views.JavaConsoleView;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenJavaREPLHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().showView(JavaConsoleView.ID);
		} catch (PartInitException exception) {
			throw new BullshitFree(exception);
		}
		return null;
	}

}
