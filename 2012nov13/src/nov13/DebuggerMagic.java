package nov13;

import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

public class DebuggerMagic {

	private IJavaProject myJavaProject;
	private Launch launch;
	private IDebugModelPresentation fPresentation;

	public DebuggerMagic() {
		try {
			initializeMagic();
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

	private void initializeMagic() throws Exception {

		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject("example");
		myJavaProject = JavaCore.create(p);

		IVMInstall vmInstall = JavaRuntime.getVMInstall(myJavaProject);
		if (vmInstall == null) vmInstall = JavaRuntime.getDefaultVMInstall();
		if (vmInstall != null) {
			IVMRunner vmRunner = vmInstall.getVMRunner(ILaunchManager.DEBUG_MODE);
			if (vmRunner != null) {
				String[] classPath = null;
				try {
					classPath = JavaRuntime.computeDefaultRuntimeClassPath(myJavaProject);
				} catch (CoreException e) {
				}
				if (classPath != null) {
					VMRunnerConfiguration vmConfig = new VMRunnerConfiguration("com.example.Example", classPath);
					launch = new Launch(null, ILaunchManager.RUN_MODE, null);

					IJavaMethodBreakpoint bp = createMagicBreakpoint();
					vmRunner.run(vmConfig, launch, null);

					// XXX Apparently we have to rely on the fact that things
					// are slow and set the breakpoint after the configuration
					// has been launched, the ScrapebookLauncher does seem to do
					// it the same way.

					launch.getDebugTarget().breakpointAdded(bp);
				}
			}
		}
	}

	private IJavaMethodBreakpoint createMagicBreakpoint() throws CoreException {
		IJavaMethodBreakpoint bp = JDIDebugModel
				.createMethodBreakpoint(ResourcesPlugin.getWorkspace().getRoot(), "com.example.Example", "main",
						"([Ljava/lang/String;)V", true, false, false, -1, -1, -1, 1, false, null);
		bp.setPersisted(false);
		return bp;
	}

	private static void moreStuff() throws Exception {
		// Get sample Java project
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject("example");
		IJavaProject project = JavaCore.create(p);
		// Get sample launch (= debug session)
		DebugPlugin debug = DebugPlugin.getDefault();
		ILaunchManager launchManager = debug.getLaunchManager();
		ILaunch launch = launchManager.getLaunches()[0];
	}

	public void evaluate(String expression, OutputStream out) {
		try {
			evaluateStuff(expression, out);
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

	private void evaluateStuff(String expression, final OutputStream out) throws Exception {
		IJavaDebugTarget target = (IJavaDebugTarget) launch.getDebugTarget();
		// Create interpreter
		IAstEvaluationEngine eval = EvaluationManager.newAstEvaluationEngine(myJavaProject, target);
		// Get sample stack frame
		IThread[] threads = target.getThreads();
		IJavaStackFrame top = (IJavaStackFrame) threads[threads.length - 1].getTopStackFrame();
		// Evaluate an expression
		eval.evaluate(expression, top, new IEvaluationListener() {
			@Override
			public void evaluationComplete(final IEvaluationResult result) {
				try {
					new PrintStream(out).println(result.getValue().getValueString());
				} catch (DebugException exception) {
					throw new BullshitFree(exception);
				}

				// evaluationResultToString(result, out);
			}
		}, DebugEvent.EVALUATION, false);
	}

}
