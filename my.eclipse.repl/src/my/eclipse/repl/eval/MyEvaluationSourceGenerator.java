package my.eclipse.repl.eval;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.internal.debug.eval.ast.engine.EvaluationSourceGenerator;

/**
 * Extension of {@link EvaluationSourceGenerator}, in order to inject import
 * statements.
 * 
 */
public class MyEvaluationSourceGenerator extends EvaluationSourceGenerator {

	public Collection<String> imports;
	private int length = 0;

	public MyEvaluationSourceGenerator(String[] types, String[] names, String snipped) {
		super(types, names, snipped);
	}

	@Override
	public int getSnippetStart() {
		return super.getSnippetStart() + length;
	}

	@Override
	public int getRunMethodStart() {
		return super.getRunMethodStart() + length;
	}

	@Override
	public String getSource(IJavaReferenceType type, IJavaProject javaProject, boolean isStatic) throws CoreException {
		String source = super.getSource(type, javaProject, isStatic);
		StringBuilder buf = new StringBuilder();
		for (String each: imports) {
			buf.append('\n');
			buf.append(each);
		}
		length = buf.length();
		buf.append("\nclass");
		return source.replace("\nclass", buf.toString());

	}

}
