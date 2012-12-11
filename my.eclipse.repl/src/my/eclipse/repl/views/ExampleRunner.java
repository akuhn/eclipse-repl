package my.eclipse.repl.views;

import my.eclipse.repl.eval.DebuggerMagic;
import my.eclipse.repl.util.BullshitFree;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

public class ExampleRunner extends DebuggerMagic {

	private IMethod example;

	public ExampleRunner(IMethod example) {
		super(example.getJavaProject());
		this.example = example;
	}

	@Override
	protected String getMainClassName() {
		return "org.junit.runner.JUnitCore";
	}

	@Override
	protected String[] getArguments() {
		return new String[] { example.getDeclaringType().getFullyQualifiedName() };
	}

	@Override
	protected String[] getBreakpointLocation() {
		try {
			return new String[] { example.getDeclaringType().getFullyQualifiedName(), example.getElementName(),
					example.getSignature() };
		} catch (JavaModelException exception) {
			throw new BullshitFree(exception);
		}
	}

	@Override
	protected boolean isBreakinOnMethodExit() {
		return true;
	}

}
