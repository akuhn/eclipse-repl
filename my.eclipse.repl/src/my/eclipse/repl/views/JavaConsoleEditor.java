package my.eclipse.repl.views;

import java.util.ArrayList;
import java.util.List;

import my.eclipse.repl.eval.EvaluationListener;
import my.eclipse.repl.eval.Result;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class JavaConsoleEditor extends EditorPart {

	public static final String ID = "my.eclipse.repl.editors.JavaConsoleEditor";

	private JavaConsolePart part;
	private ExampleContext context;

	private List<Result> data = new ArrayList();
	private boolean dirty;

	@Override
	public void createPartControl(Composite parent) {
		IEditorInput input = getEditorInput();
		context = input instanceof ExampleContext ? (ExampleContext) input : null;
		part = new JavaConsolePart(context);
		part.createPartControl(parent);
		part.getREPL().addEvaluationListener(new EvaluationListener() {

			@Override
			public void notify(Result result) {
				if (dirty == false) {
					dirty = true;
					firePropertyChange(PROP_DIRTY);
				}
				data.add(result);
			}
		});
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ExampleGenerator gen = new ExampleGenerator(context);
		gen.generateExampleWith(data);
		data.clear();
		dirty = false;
		firePropertyChange(PROP_DIRTY);
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
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
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
