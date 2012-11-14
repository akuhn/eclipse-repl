package nov13;

import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventFilter;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IEvaluationRunnable;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
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
import org.eclipse.jdt.internal.debug.eval.EvaluationResult;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTEvaluationEngine;
import org.eclipse.jdt.internal.debug.eval.ast.engine.EvaluationEngineMessages;
import org.eclipse.jdt.internal.debug.eval.ast.engine.IRuntimeContext;
import org.eclipse.jdt.internal.debug.eval.ast.engine.Interpreter;
import org.eclipse.jdt.internal.debug.eval.ast.engine.RuntimeContext;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstructionSequence;
import org.eclipse.jdt.internal.debug.ui.JavaDetailFormattersManager;

import com.sun.jdi.InvocationException;
import com.sun.jdi.ObjectReference;

/**
 * Copy of {@link ASTEvaluationEngine}, in order to access local variables of
 * the AST interpreter.
 * 
 */
public class MyEvaluationEngine implements IEvaluationEngine {

	private IAstEvaluationEngine eval;

	public MyEvaluationEngine(IAstEvaluationEngine eval) {
		this.eval = eval;
	}

	public void evaluateExpression(String snippet, final OutputStream out, final IJavaDebugTarget target,
			IJavaStackFrame frame) throws DebugException {
		IEvaluationListener listener = new IEvaluationListener() {
			@Override
			public void evaluationComplete(final IEvaluationResult result) {
				try {
					printEvaluationResult(result, target, out);
				} catch (DebugException exception) {
					throw new BullshitFree(exception);
				}
			}

		};
		boolean hitBreakpoints = false;
		int evaluationDetail = DebugEvent.EVALUATION;
		ICompiledExpression expression = eval.getCompiledExpression(snippet, frame);
		IRuntimeContext context = new RuntimeContext(eval.getJavaProject(), frame);
		doEvaluation(expression, context, (IJavaThread) frame.getThread(), listener, evaluationDetail, hitBreakpoints);

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

	public void printEvaluationResult(final IEvaluationResult result, IJavaDebugTarget target, final OutputStream out)
			throws DebugException {
		IJavaValue value = result.getValue();
		JavaDetailFormattersManager man = JavaDetailFormattersManager.getDefault();
		IThread[] threads = target.getThreads();
		man.computeValueDetail(value, (IJavaThread) threads[threads.length - 1], new IValueDetailListener() {
			@Override
			public void detailComputed(IValue value, String result) {
				new PrintStream(out).println(result);
			}
		});
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
			EvaluationResult result = new EvaluationResult(MyEvaluationEngine.this, fExpression.getSnippet(), fThread);
			if (fExpression.hasErrors()) {
				String[] errors = fExpression.getErrorMessages();
				for (String error: errors) {
					result.addError(error);
				}
				evaluationFinished(result);
				return;
			}
			final Interpreter interpreter = new Interpreter(fExpression, fContext);

			class EvaluationRunnable implements IEvaluationRunnable, ITerminate {

				CoreException fException;
				boolean fTerminated = false;

				public void run(IJavaThread jt, IProgressMonitor pm) {
					EventFilter filter = new EventFilter();
					try {
						DebugPlugin.getDefault().addDebugEventFilter(filter);
						interpreter.execute();
					} catch (CoreException exception) {
						fException = exception;
						if (fEvaluationDetail == DebugEvent.EVALUATION
								&& exception.getStatus().getException() instanceof InvocationException) {
							// print the stack trace for the exception if an
							// *explicit* evaluation
							InvocationException invocationException = (InvocationException) exception.getStatus()
									.getException();
							ObjectReference exObject = invocationException.exception();
							IJavaObject modelObject = (IJavaObject) JDIValue.createValue(
									(JDIDebugTarget) getDebugTarget(), exObject);
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

			EvaluationRunnable er = new EvaluationRunnable();
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
			evaluationFinished(result);
		}

		private void evaluationFinished(IEvaluationResult result) {
			// only notify if plug-in not yet shutdown - bug# 8693
			if (JDIDebugPlugin.getDefault() != null) {
				fListener.evaluationComplete(result);
			}
		}

	}

	/**
	 * Filters variable change events during an evaluation to avoid refreshing
	 * the variables view until done.
	 */
	class EventFilter implements IDebugEventFilter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.debug.core.IDebugEventFilter#filterDebugEvents(org.eclipse
		 * .debug.core.DebugEvent[])
		 */
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
