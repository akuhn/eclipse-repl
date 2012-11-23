package my.eclipse.repl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.debug.core.breakpoints.JavaMethodBreakpoint;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.eclipse.jdt.internal.debug.ui.JavaDetailFormattersManager;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.sun.jdi.event.Event;

/**
 * Runs example main method in example project, stops at method entry and
 * evaluates expressions within that context.
 * 
 */
public class DebuggerMagic {

	private static final String MAIN_CLASS_NAME = "my.eclipse.repl.Main";

	private IJavaProject myJavaProject;
	private Launch launch;
	private MyEvaluationEngine eval;

	public DebuggerMagic() {
		try {
			initializeMagic();
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

	private void initializeMagic() throws Exception {

		myJavaProject = anyJavaProject();

		IVMInstall vmInstall = JavaRuntime.getVMInstall(myJavaProject);
		if (vmInstall == null) vmInstall = JavaRuntime.getDefaultVMInstall();
		if (vmInstall == null) return;
		IVMRunner vmRunner = vmInstall.getVMRunner(ILaunchManager.DEBUG_MODE);
		if (vmRunner == null) return;
		String[] classPath = computeCustomClassPath();
		VMRunnerConfiguration config = new VMRunnerConfiguration(MAIN_CLASS_NAME, classPath);
		launch = new Launch(null, ILaunchManager.DEBUG_MODE, null);

		IJavaMethodBreakpoint bp = createMagicBreakpoint();
		vmRunner.run(config, launch, null);

		// XXX Apparently we have to rely on the fact that things
		// are slow and set the breakpoint after the configuration
		// has been launched, the ScrapebookLauncher actually makes
		// the same bold bet.

		launch.getDebugTarget().breakpointAdded(bp);
		IJavaDebugTarget target = (IJavaDebugTarget) launch.getDebugTarget();
		eval = new MyEvaluationEngine(myJavaProject, target);
	}

	private String[] computeCustomClassPath() {
		StringList path = new StringList();
		try {
			path.add(JavaRuntime.computeDefaultRuntimeClassPath(myJavaProject));
		} catch (CoreException exception) {
			throw new BullshitFree(exception);
		}
		try {
			URL entry = Activator.getContext().getBundle().getEntry("bin");
			String string = FileLocator.toFileURL(entry).getFile();
			path.add(string);
		} catch (IOException exception) {
			throw new BullshitFree(exception);
		}
		return path.asArray();
	}

	private IJavaProject anyJavaProject() {
		// XXX works in UI thread only!
		// ISelectionService window =
		// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		// IStructuredSelection sel = (IStructuredSelection)
		// window.getSelection("org.eclipse.jdt.ui.PackageExplorer");
		// if (sel != null && !sel.isEmpty()) return ((IJavaElement)
		// sel.iterator().next()).getJavaProject();
		for (IProject each: ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IJavaProject p = JavaCore.create(each);
			if (p != null) return p;
		}
		throw new Error("No java project found!");
	}

	CountDownLatch isSuspended = new CountDownLatch(1);

	private IJavaMethodBreakpoint createMagicBreakpoint() throws CoreException {
		IJavaMethodBreakpoint bp = new JavaMethodBreakpoint( //
				ResourcesPlugin.getWorkspace().getRoot(), //
				MAIN_CLASS_NAME, "main", "([Ljava/lang/String;)V", //
				true, false, false, -1, -1, -1, 1, false, //
				new HashMap()) {
			@Override
			public boolean handleBreakpointEvent(Event event, JDIThread thread, boolean suspendVote) {
				try {
					return super.handleBreakpointEvent(event, thread, suspendVote);
				} finally {
					isSuspended.countDown();
				}
			}
		};
		bp.setPersisted(false);
		return bp;
	}

	public void evaluate(String expression, OutputStream out) {
		try {
			evaluateStuff(expression, out);
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

	private static Pattern IMPORT = Pattern.compile("import\\s+(static\\s+)?\\w+(\\.\\w+)*(\\.\\*)?;");

	private void evaluateStuff(String expression, final OutputStream os) throws Exception {
		if (expression == null) return;
		if (IMPORT.matcher(expression).matches()) {
			eval.imports.add(expression);
			PrintStream out = new PrintStream(os);
			for (String each: eval.imports) {
				out.println(each);
			}
			return;
		}
		IThread[] threads = launch.getDebugTarget().getThreads();
		isSuspended.await();
		IJavaStackFrame frame = (IJavaStackFrame) threads[threads.length - 1].getTopStackFrame();
		IEvaluationListener callback = new IEvaluationListener() {
			@Override
			public void evaluationComplete(final IEvaluationResult result) {
				try {
					printEvaluationResult((MyEvaluationResult) result, os);
				} catch (DebugException exception) {
					throw new BullshitFree(exception);
				}
			}

		};
		eval.evaluateExpression(expression, frame, callback);
	}

	public void printEvaluationResult(MyEvaluationResult result, OutputStream os) throws DebugException {
		final PrintStream out = new PrintStream(os);
		if (result.hasErrors()) {
			for (String each: result.getErrorMessages()) {
				out.println(each);
			}
			DebugException exception = result.getException();
			if (exception != null) {
				out.println(exception.getMessage());
			}
		} else {
			for (String each: result.internalVariables.keySet()) {
				out.print(each);
				out.print("=");
				out.println(result.internalVariables.get(each).getValue());
			}
			IJavaValue value = result.getValue();
			JavaDetailFormattersManager man = JavaDetailFormattersManager.getDefault();
			IThread[] threads = launch.getDebugTarget().getThreads();
			man.computeValueDetail(value, (IJavaThread) threads[threads.length - 1], new IValueDetailListener() {
				@Override
				public void detailComputed(IValue value, String result) {
					out.println(result);
				}
			});
		}
	}

}
