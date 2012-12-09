package my.eclipse.repl.views;

import my.eclipse.repl.eval.DebuggerMagic;
import my.eclipse.repl.eval.MagicFactory;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class REPLEditorInput implements IEditorInput, MagicFactory {

	private Object element;

	public REPLEditorInput(Object element) {
		this.element = element;
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "REPL Editor Input";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "REPL Editor Input";
	}

	@Override
	public DebuggerMagic makeMagic() {
		if (element instanceof IJavaElement) {
			IJavaProject project = ((IJavaElement) element).getJavaProject();
			return new DebuggerMagic(project);
		}
		return null;
	}

}
