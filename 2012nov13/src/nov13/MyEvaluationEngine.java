package nov13;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventFilter;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.debug.core.IEvaluationRunnable;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.ICompiledExpression;
import org.eclipse.jdt.debug.eval.IEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.eclipse.jdt.internal.debug.core.model.JDIValue;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTEvaluationEngine;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTInstructionCompiler;
import org.eclipse.jdt.internal.debug.eval.ast.engine.EvaluationEngineMessages;
import org.eclipse.jdt.internal.debug.eval.ast.engine.EvaluationSourceGenerator;
import org.eclipse.jdt.internal.debug.eval.ast.engine.IRuntimeContext;
import org.eclipse.jdt.internal.debug.eval.ast.engine.Interpreter;
import org.eclipse.jdt.internal.debug.eval.ast.engine.RuntimeContext;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstructionSequence;

import com.sun.jdi.InvocationException;
import com.sun.jdi.ObjectReference;

/**
 * Copy of {@link ASTEvaluationEngine}, in order to access local variables of
 * the AST interpreter.
 * 
 */
public class MyEvaluationEngine implements IEvaluationEngine {

	private IAstEvaluationEngine eval;
	private Map<String, IVariable> internalVariables;

	public MyEvaluationEngine(IAstEvaluationEngine eval) {
		this.eval = eval;
	}

	public void evaluateExpression(String snippet, IJavaStackFrame frame, IEvaluationListener listener)
			throws DebugException {
		boolean hitBreakpoints = false;
		int evaluationDetail = DebugEvent.EVALUATION;
		ICompiledExpression expression = getCompiledExpression(snippet, frame);
		IRuntimeContext context = new RuntimeContext(eval.getJavaProject(), frame);
		doEvaluation(expression, context, (IJavaThread) frame.getThread(), listener, evaluationDetail, hitBreakpoints);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.debug.eval.IAstEvaluationEngine#getCompiledExpression
	 * (java.lang.String, org.eclipse.jdt.debug.core.IJavaStackFrame)
	 */
	public ICompiledExpression getCompiledExpression(String snippet, IJavaStackFrame frame) {
		IJavaProject javaProject = getJavaProject();
		RuntimeContext context = new RuntimeContext(javaProject, frame);

		EvaluationSourceGenerator mapper = null;
		CompilationUnit unit = null;
		try {
			mapper = ohBoyDontYouMissHigherOrderProgrammingInJava(snippet, context);
			// Compile in context of declaring type to get proper visibility of
			// locals and members.
			// Compiling in context of receiving type potentially provides
			// access to more members,
			// but does not allow access to privates members in declaring type
			IJavaReferenceType receivingType = frame.getReferenceType();

			// currently disabled - see bugs 99416 and 106492
			// if (frame.isStatic()) {
			// receivingType= frame.getReferenceType();
			// } else {
			// receivingType= (IJavaReferenceType)
			// frame.getThis().getJavaType();
			// }

			unit = parseCompilationUnit(mapper.getSource(receivingType, javaProject, frame.isStatic()).toCharArray(),
					mapper.getCompilationUnitName(), javaProject);
		} catch (CoreException e) {
			InstructionSequence expression = new InstructionSequence(snippet);
			expression.addError(e.getStatus().getMessage());
			return expression;
		}

		return createExpressionFromAST(snippet, mapper, unit);
	}

	private EvaluationSourceGenerator ohBoyDontYouMissHigherOrderProgrammingInJava(String snippet,
			RuntimeContext context) throws CoreException, DebugException {
		EvaluationSourceGenerator mapper;
		IJavaVariable[] localsVar = context.getLocals();
		int numLocalsVar = localsVar.length;
		Set<String> names = new HashSet<String>();
		// ******
		// to hide problems with local variable declare as instance of Local
		// Types
		// and to remove locals with duplicate names
		Map<String, IVariable> vars = new HashMap();
		IJavaVariable[] locals = new IJavaVariable[numLocalsVar];
		int numLocals = 0;
		for (int i = 0; i < numLocalsVar; i++) {
			if (!isLocalType(localsVar[i].getSignature()) && !names.contains(localsVar[i].getName())) {
				locals[numLocals++] = localsVar[i];
				names.add(localsVar[i].getName());
				vars.put(localsVar[i].getName(), localsVar[i]);
			}
		}
		if (internalVariables != null) vars.putAll(internalVariables);
		// to solve and remove
		// ******

		String[] localTypesNames = new String[vars.size()];
		String[] localVariables = new String[vars.size()];
		int i = 0;
		for (String each: vars.keySet()) {
			localVariables[i] = each;
			localTypesNames[i] = Signature.toString(((IJavaVariable) vars.get(each)).getGenericSignature())
					.replace('/', '.');
			i++;
		}

		mapper = new EvaluationSourceGenerator(localTypesNames, localVariables, snippet);
		return mapper;
	}

	/**
	 * Creates a compiled expression for the given snippet using the given
	 * mapper and compilation unit (AST).
	 * 
	 * @param snippet
	 *            the code snippet to be compiled
	 * @param mapper
	 *            the object which will be used to create the expression
	 * @param unit
	 *            the compilation unit (AST) generated for the snippet
	 */
	private ICompiledExpression createExpressionFromAST(String snippet, EvaluationSourceGenerator mapper,
			CompilationUnit unit) {
		IProblem[] problems = unit.getProblems();
		if (problems.length != 0) {
			boolean snippetError = false;
			boolean runMethodError = false;
			InstructionSequence errorSequence = new InstructionSequence(snippet);
			int codeSnippetStart = mapper.getSnippetStart();
			int codeSnippetEnd = codeSnippetStart + mapper.getSnippet().length();
			int runMethodStart = mapper.getRunMethodStart();
			int runMethodEnd = runMethodStart + mapper.getRunMethodLength();
			for (IProblem problem: problems) {
				int errorOffset = problem.getSourceStart();
				int problemId = problem.getID();
				if (problemId == IProblem.IsClassPathCorrect) {
					errorSequence.addError(problem.getMessage());
					snippetError = true;
				}
				if (problemId == IProblem.VoidMethodReturnsValue || problemId == IProblem.NotVisibleMethod
						|| problemId == IProblem.NotVisibleConstructor || problemId == IProblem.NotVisibleField
						|| problemId == IProblem.NotVisibleType) {
					continue;
				}
				if (problem.isError()) {
					if (codeSnippetStart <= errorOffset && errorOffset <= codeSnippetEnd) {
						errorSequence.addError(problem.getMessage());
						snippetError = true;
					} else if (runMethodStart <= errorOffset && errorOffset <= runMethodEnd) {
						runMethodError = true;
					}
				}
			}
			if (snippetError || runMethodError) {
				if (runMethodError) {
					errorSequence
							.addError(EvaluationEngineMessages.ASTEvaluationEngine_Evaluations_must_contain_either_an_expression_or_a_block_of_well_formed_statements_1);
				}
				return errorSequence;
			}
		}

		ASTInstructionCompiler visitor = new ASTInstructionCompiler(mapper.getSnippetStart(), snippet);
		unit.accept(visitor);

		return visitor.getInstructions();
	}

	private CompilationUnit parseCompilationUnit(char[] source, String unitName, IJavaProject project) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(source);
		parser.setUnitName(unitName);
		parser.setProject(project);
		parser.setResolveBindings(true);
		Map<String, String> options = EvaluationSourceGenerator.getCompilerOptions(project);
		parser.setCompilerOptions(options);
		return (CompilationUnit) parser.createAST(null);
	}

	// ******
	// to hide problems with local variable declare as instance of Local Types
	private boolean isLocalType(String typeName) {
		StringTokenizer strTok = new StringTokenizer(typeName, "$"); //$NON-NLS-1$
		strTok.nextToken();
		while (strTok.hasMoreTokens()) {
			char char0 = strTok.nextToken().charAt(0);
			if ('0' <= char0 && char0 <= '9') { return true; }
		}
		return false;
	}

	private void doEvaluation(ICompiledExpression expression, IRuntimeContext context, IJavaThread thread,
			IEvaluationListener listener, int evaluationDetail, boolean hitBreakpoints) throws DebugException {
		if (expression instanceof InstructionSequence) {
			// don't queue explicit evaluation if the thread is all ready
			// performing an evaluation.
			if (thread.isSuspended() && ((JDIThread) thread).isInvokingMethod() || thread.isPerformingEvaluation()
					&& evaluationDetail == DebugEvent.EVALUATION) {
				org.eclipse.jdt.internal.debug.eval.EvaluationResult result = new org.eclipse.jdt.internal.debug.eval.EvaluationResult(
						this, expression.getSnippet(), thread);
				result.addError(EvaluationEngineMessages.ASTEvaluationEngine_Cannot_perform_nested_evaluations);
				listener.evaluationComplete(result);
				return;
			}
			thread.queueRunnable(new EvalRunnable((InstructionSequence) expression, thread, context, listener,
					evaluationDetail, hitBreakpoints));
		} else {
			throw new DebugException(
					new Status(
							IStatus.ERROR,
							JDIDebugPlugin.getUniqueIdentifier(),
							IStatus.OK,
							EvaluationEngineMessages.ASTEvaluationEngine_AST_evaluation_engine_cannot_evaluate_expression,
							null));
		}
	}

	@Override
	public void evaluate(String snippet, IJavaStackFrame frame, IEvaluationListener listener, int evaluationDetail,
			boolean hitBreakpoints) throws DebugException {
		throw new Error();
	}

	@Override
	public void evaluate(String snippet, IJavaObject thisContext, IJavaThread thread, IEvaluationListener listener,
			int evaluationDetail, boolean hitBreakpoints) throws DebugException {
		throw new Error();
	}

	@Override
	public IJavaProject getJavaProject() {
		return eval.getJavaProject();
	}

	@Override
	public IJavaDebugTarget getDebugTarget() {
		return eval.getDebugTarget();
	}

	@Override
	public void dispose() {
		eval.dispose();
	}

	class EvalRunnable implements Runnable {

		private InstructionSequence fExpression;

		private IJavaThread fThread;

		private int fEvaluationDetail;

		private boolean fHitBreakpoints;

		private IRuntimeContext fContext;

		private IEvaluationListener fListener;

		public EvalRunnable(InstructionSequence expression, IJavaThread thread, IRuntimeContext context,
				IEvaluationListener listener, int evaluationDetail, boolean hitBreakpoints) {
			fExpression = expression;
			fThread = thread;
			fContext = context;
			fListener = listener;
			fEvaluationDetail = evaluationDetail;
			fHitBreakpoints = hitBreakpoints;
		}

		public void run() {
			MyEvaluationResult result = new MyEvaluationResult(MyEvaluationEngine.this, fExpression.getSnippet(),
					fThread);
			if (fExpression.hasErrors()) {
				String[] errors = fExpression.getErrorMessages();
				for (String error: errors) {
					result.addError(error);
				}
				evaluationFinished(result);
				return;
			}
			final Interpreter interpreter = new Interpreter(fExpression, fContext);
			myPassInternalVariablesToInterpreter(interpreter);

			EvaluationRunnable er = new EvaluationRunnable(interpreter);
			CoreException exception = null;
			try {
				fThread.runEvaluation(er, null, fEvaluationDetail, fHitBreakpoints);
			} catch (DebugException e) {
				exception = e;
			}

			IJavaValue value = interpreter.getResult();

			if (exception == null) {
				exception = er.getException();
			}

			result.setTerminated(er.fTerminated);
			if (exception != null) {
				if (exception instanceof DebugException) {
					result.setException((DebugException) exception);
				} else {
					result.setException(new DebugException(exception.getStatus()));
				}
			} else {
				if (value != null) {
					result.setValue(value);
				} else {
					result.addError(EvaluationEngineMessages.ASTEvaluationEngine_An_unknown_error_occurred_during_evaluation);
				}
			}

			myStoreInternalVariablesInResult(result, interpreter);

			evaluationFinished(result);
		}

		private void evaluationFinished(IEvaluationResult result) {
			// only notify if plug-in not yet shutdown - bug# 8693
			if (JDIDebugPlugin.getDefault() != null) {
				fListener.evaluationComplete(result);
			}
		}

	}

	private void myPassInternalVariablesToInterpreter(Interpreter interpreter) {
		if (internalVariables != null) {
			BullshitFree.setField(interpreter, "fInternalVariables", internalVariables);
		}
	}

	private void myStoreInternalVariablesInResult(MyEvaluationResult result, Interpreter interpreter) {
		internalVariables = BullshitFree.getField(interpreter, "fInternalVariables");
		result.internalVariables = internalVariables;
	}

	class EvaluationRunnable implements IEvaluationRunnable, ITerminate {

		CoreException fException;
		boolean fTerminated = false;
		private Interpreter interpreter;

		public EvaluationRunnable(Interpreter interpreter) {
			this.interpreter = interpreter;
		}

		public void run(IJavaThread jt, IProgressMonitor pm) {
			EventFilter filter = new EventFilter();
			try {
				DebugPlugin.getDefault().addDebugEventFilter(filter);
				interpreter.execute();
			} catch (CoreException exception) {
				fException = exception;
				if (exception.getStatus().getException() instanceof InvocationException) {
					// print the stack trace for the exception if an
					// *explicit* evaluation
					InvocationException invocationException = (InvocationException) exception.getStatus()
							.getException();
					ObjectReference exObject = invocationException.exception();
					IJavaObject modelObject = (IJavaObject) JDIValue.createValue((JDIDebugTarget) getDebugTarget(),
							exObject);
					try {
						modelObject.sendMessage("printStackTrace", "()V", null, jt, false); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (DebugException e) {
						// unable to print stack trace
					}
				}
			} finally {
				DebugPlugin.getDefault().removeDebugEventFilter(filter);
			}
		}

		public void terminate() {
			fTerminated = true;
			interpreter.stop();
		}

		public boolean canTerminate() {
			return true;
		}

		public boolean isTerminated() {
			return false;
		}

		public CoreException getException() {
			return fException;
		}
	}

	/**
	 * Filters variable change events during an evaluation to avoid refreshing
	 * the variables view until done.
	 */
	class EventFilter implements IDebugEventFilter {

		public DebugEvent[] filterDebugEvents(DebugEvent[] events) {
			if (events.length == 1) {
				DebugEvent event = events[0];
				if (event.getSource() instanceof IJavaVariable && event.getKind() == DebugEvent.CHANGE) {
					if (((IJavaVariable) event.getSource()).getDebugTarget().equals(getDebugTarget())) { return null; }
				}
			}
			return events;
		}

	}

}
