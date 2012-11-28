package my.eclipse.repl.eval;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.debug.ui.contentassist.JavaDebugContentAssistProcessor;
import org.eclipse.jdt.internal.debug.ui.contentassist.TypeContext;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class MyContentAssistProcessor extends JavaDebugContentAssistProcessor {

	protected static final String MAC_GUFFIN = "Cloneable";

	public MyContentAssistProcessor(final IJavaProject project) {
		super(new TypeContext(null, -1) {

			public IType getType() throws CoreException {
				return project.findType("java.lang." + MAC_GUFFIN);
			}

			@Override
			public String[][] getLocalVariables() throws CoreException {
				return new String[2][0];
			}

			@Override
			public boolean isStatic() throws CoreException {
				return true;
			}

		});
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		ICompletionProposal[] proposals = super.computeCompletionProposals(viewer, documentOffset);
		if (proposals.length == 1 && proposals[0].toString().contains(MAC_GUFFIN)) return new ICompletionProposal[0];
		return proposals;
	}

}
