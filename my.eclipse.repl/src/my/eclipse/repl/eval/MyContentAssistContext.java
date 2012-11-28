package my.eclipse.repl.eval;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.debug.ui.contentassist.TypeContext;

public class MyContentAssistContext extends TypeContext {

	private IJavaProject project;

	public MyContentAssistContext(IJavaProject project) {
		super(null, -1);
		this.project = project;
	}

	public IType getType() throws CoreException {
		return project.findType("java.lang.Cloneable");
	}

	@Override
	public String[][] getLocalVariables() throws CoreException {
		return new String[2][0];
	}

	@Override
	public boolean isStatic() throws CoreException {
		return true;
	}

}
