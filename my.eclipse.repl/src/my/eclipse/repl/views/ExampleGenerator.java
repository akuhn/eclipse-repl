package my.eclipse.repl.views;

import java.util.List;

import my.eclipse.repl.eval.Result;
import my.eclipse.repl.util.BullshitFree;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.actions.OpenNewClassWizardAction;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.text.edits.TextEdit;

public class ExampleGenerator {

	private ExampleContext context;

	public ExampleGenerator(ExampleContext context) {
		this.context = context;
	}

	public void generateExampleWith(List<Result> data) {
		try {
			IJavaProject project = context.getJavaProject();
			IMethod method = context.getExampleMethod();
			IType type = declaringType(project, method);

			StringBuilder buf = new StringBuilder();
			buf.append("\t@Test\n");
			buf.append("\tpublic void shouldDoStuff() {\n");
			buf.append("\t\tObject _;\n");
			for (Result each: data) {
				if (each.hasErrors()) continue;
				buf.append("\t\t_ = ");
				buf.append(each.getExpression());
				buf.append(";\n");
				buf.append("\t\tassertEquals(\"");
				buf.append(each.toPrintString());
				buf.append("\",_.toString());\n");
			}
			buf.append("\t}\n");
			String source = buf.toString();

			ImportRewrite rewrite = CodeStyleConfiguration.createImportRewrite(type.getCompilationUnit(), true);
			rewrite.addImport("junit.org.Test");
			rewrite.addStaticImport("org.junit.Assert", "assertEquals", false);
			TextEdit edit = rewrite.rewriteImports(null);
			type.getCompilationUnit().applyTextEdit(edit, null);

			type.createMethod(source, null, true, null);

		} catch (CoreException exception) {
			throw new BullshitFree(exception);
		}
	}

	private IType declaringType(IJavaProject project, IMethod method) throws JavaModelException {
		if (method != null) return method.getDeclaringType();
		OpenNewClassWizardAction wizard = new OpenNewClassWizardAction();
		wizard.setOpenEditorOnFinish(false);
		NewClassWizardPage page = new NewClassWizardPage();
		page.setPackageFragmentRoot(project.getPackageFragmentRoots()[0], true);
		wizard.setConfiguredWizardPage(page);
		wizard.run();
		return (IType) wizard.getCreatedElement();
	}

}
