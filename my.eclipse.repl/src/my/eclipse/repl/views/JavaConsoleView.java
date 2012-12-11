package my.eclipse.repl.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class JavaConsoleView extends ViewPart {

	public static final String ID = "my.eclipse.repl.views.JavaREPL";

	private JavaConsolePart part;

	@Override
	public void createPartControl(Composite parent) {
		part = new JavaConsolePart();
		part.createPartControl(parent);
	}

	@Override
	public void setFocus() {
		part.setFocus();
	}

	@Override
	public void dispose() {
		part.dispose();
		super.dispose();
	}

}