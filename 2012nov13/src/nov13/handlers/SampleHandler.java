package nov13.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public SampleHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			myStuff();
		} catch (Exception e) {
			new RuntimeException(e);
		}
		return null;
	}

	private void myStuff() throws Exception {

		// moreStuff();

		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject("example");
		IJavaProject myJavaProject = JavaCore.create(p);

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
					ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null);

					IJavaMethodBreakpoint bp = JDIDebugModel.createMethodBreakpoint(ResourcesPlugin.getWorkspace()
							.getRoot(), "com.example.Example", "main", "([Ljava/lang/String;)V", true, false, false,
							-1, -1, -1, 1, false, null);
					bp.setPersisted(false);
					vmRunner.run(vmConfig, launch, null);

					// XXX Apparently we have to rely on the fact that things
					// are slow and set the breakpoint after the configuration
					// has been launched, the ScrapebookLauncher does seem to do
					// it the same way.

					launch.getDebugTarget().breakpointAdded(bp);
					evaluateThreePlusFour(myJavaProject, launch);
				}
			}
		}
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

	private void evaluateThreePlusFour(IJavaProject project, ILaunch launch) throws DebugException {
		IJavaDebugTarget target = (IJavaDebugTarget) launch.getDebugTarget();
		// Create interpreter
		IAstEvaluationEngine eval = EvaluationManager.newAstEvaluationEngine(project, target);
		// Get sample stack frame
		IThread[] threads = target.getThreads();
		IJavaStackFrame top = (IJavaStackFrame) threads[threads.length - 1].getTopStackFrame();
		// Evaluate an expression
		eval.evaluate("3+4", top, new IEvaluationListener() {
			@Override
			public void evaluationComplete(IEvaluationResult result) {
				try {
					System.out.println(result.getValue().getValueString());
				} catch (DebugException e) {
					throw new RuntimeException(e);
				}
			}
		}, DebugEvent.EVALUATION, false);
	}

}
