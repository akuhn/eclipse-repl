package my.eclipse.repl.views;

import my.eclipse.repl.eval.MagicFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class JavaConsoleEditor extends EditorPart {

	public static final String ID = "my.eclipse.repl.editors.JavaConsoleEditor";

	private JavaConsolePart part;

	@Override
	public void createPartControl(Composite parent) {
		MagicFactory factory = null;
		IEditorInput input = getEditorInput();
		if (input instanceof MagicFactory) {
			factory = (MagicFactory) input;
		}
		part = new JavaConsolePart(factory);
		part.createPartControl(parent);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
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
