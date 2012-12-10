package my.eclipse.repl.views;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;

public class ExampleGenerator {

	private ExampleContext context;

	public ExampleGenerator(ExampleContext context) {
		this.context = context;
	}

	public void generateExampleWith(List data) {
		IJavaProject project = context.getJavaProject();
		ICompilationUnit unit = context.getCompilationUnit();
		IMethod method = context.getExampleMethod();
	}

}
