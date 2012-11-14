package nov13;

import java.util.Map;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.eval.IEvaluationEngine;
import org.eclipse.jdt.internal.debug.eval.EvaluationResult;

public class MyEvaluationResult extends EvaluationResult {

	public Map<String, IVariable> internalVariables;

	public MyEvaluationResult(IEvaluationEngine engine, String snippet, IJavaThread thread) {
		super(engine, snippet, thread);
	}

}
