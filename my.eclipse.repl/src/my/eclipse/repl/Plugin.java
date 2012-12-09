package my.eclipse.repl;

import my.eclipse.repl.util.BullshitFree;
import my.eclipse.repl.views.JavaConsoleEditor;
import my.eclipse.repl.views.StringEditorInput;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Plugin extends AbstractUIPlugin {

	private static Plugin plugin;
	private static BundleContext context;

	public Plugin() {
		plugin = this;
	}

	public static Plugin getDefault() {
		return plugin;
	}

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Plugin.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Plugin.context = null;
	}

	public void openStringEditor() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbenchPage page = getWorkbench().getActiveWorkbenchWindow().getActivePage();
					page.openEditor(new StringEditorInput("Hello, worlds!"), JavaConsoleEditor.ID);
				} catch (PartInitException exception) {
					throw new BullshitFree(exception);
				}
			}
		});
	}

}
