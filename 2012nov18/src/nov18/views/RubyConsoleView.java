package nov18.views;


import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.eclipse.ui.part.ViewPart;

public class RubyConsoleView extends ViewPart {

	public static final String ID = "nov18.views.SampleView";

	private IOConsoleViewer viewer;

	public void createPartControl(Composite parent) {
		IOConsole console = new RubyConsole();
		viewer = new IOConsoleViewer(parent, console);
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

}