package my.eclipse.repl.eval;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import my.eclipse.repl.Plugin;
import my.eclipse.repl.util.BullshitFree;
import my.eclipse.repl.util.Promise;
import my.eclipse.repl.util.StringList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
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

	private static final String MAIN_CLASS_NAME = "my.eclipse.repl.eval.MacGuffin";

	private IJavaProject project;
	private Launch launch;
	private MyEvaluationEngine eval;

	public DebuggerMagic(IJavaProject project) {
		this.project = project;
	}

	public DebuggerMagic() {
		this(anyJavaProject());
	}

	private void initializeMagic() throws Exception {

		IVMInstall vmInstall = JavaRuntime.getVMInstall(project);
		if (vmInstall == null) vmInstall = JavaRuntime.getDefaultVMInstall();
		if (vmInstall == null) return;
		IVMRunner vmRunner = vmInstall.getVMRunner(ILaunchManager.DEBUG_MODE);
		if (vmRunner == null) return;
		String[] classPath = computeCustomClassPath();
		VMRunnerConfiguration config = new VMRunnerConfiguration(MAIN_CLASS_NAME, classPath);
		launch = new Launch(null, ILaunchManager.DEBUG_MODE, null);

		// TODO figure out which attributes and stuff in launch we're missing
		// for correct display in the debugger's launch view!

		DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);

		IJavaMethodBreakpoint bp = createMagicBreakpoint();
		vmRunner.run(config, launch, null);

		// TODO capture output from launch's process here

		// XXX Apparently we have to rely on the fact that things
		// are slow and set the breakpoint after the configuration
		// has been launched, the ScrapebookLauncher actually makes
		// the same bold bet.

		launch.getDebugTarget().breakpointAdded(bp);
		IJavaDebugTarget target = (IJavaDebugTarget) launch.getDebugTarget();
		eval = new MyEvaluationEngine(project, target);
	}

	private String[] computeCustomClassPath() {
		StringList path = new StringList();
		try {
			path.add(JavaRuntime.computeDefaultRuntimeClassPath(project));
		} catch (CoreException exception) {
			throw new BullshitFree(exception);
		}
		try {
			URL entry = Plugin.getContext().getBundle().getEntry("bin");
			String string = FileLocator.toFileURL(entry).getFile();
			path.add(string);
		} catch (IOException exception) {
			throw new BullshitFree(exception);
		}
		return path.asArray();
	}

	private static IJavaProject anyJavaProject() {
		for (IProject each: ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (!each.isOpen()) continue;
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

	public String evaluate(String expression) {
		if (eval == null) try {
			initializeMagic();
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
		assert expression != null;
		if (IMPORT.matcher(expression).matches()) {
			eval.imports.add(expression);
			StringBuilder buf = new StringBuilder();
			for (String each: eval.imports) {
				buf.append(each);
				buf.append('\n');
			}
			return buf.toString();
		}
		try {
			return evaluateStuff(expression);
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

	private static Pattern IMPORT = Pattern.compile("import\\s+(static\\s+)?\\w+(\\.\\w+)*(\\.\\*)?;");

	private String evaluateStuff(String expression) throws DebugException, InterruptedException {
		IJavaStackFrame frame = (IJavaStackFrame) getSuspendedThread().getTopStackFrame();
		Promise result = new Promise(IEvaluationListener.class);
		eval.evaluateExpression(expression, frame, (IEvaluationListener) result.callback());
		return printEvaluationResult((MyEvaluationResult) result.await()[0]);
	}

	private String printEvaluationResult(MyEvaluationResult result) throws DebugException, InterruptedException {
		if (result.hasErrors()) return printEvaluationErrors(result);

		IJavaValue value = result.getValue();
		JavaDetailFormattersManager man = JavaDetailFormattersManager.getDefault();

		Promise detail = new Promise(IValueDetailListener.class);
		man.computeValueDetail(value, getSuspendedThread(), (IValueDetailListener) detail.callback());
		return (String) detail.await()[1];
	}

	private String printEvaluationErrors(MyEvaluationResult result) {
		StringBuilder buf = new StringBuilder();
		for (String each: result.getErrorMessages()) {
			buf.append(each);
			buf.append('\n');
		}
		DebugException exception = result.getException();
		if (exception != null) {
			buf.append(exception.getMessage());
			buf.append('\n');
		}
		return buf.toString();
	}

	private IJavaThread getSuspendedThread() throws DebugException, InterruptedException {
		isSuspended.await();
		IThread[] threads = launch.getDebugTarget().getThreads();
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].isSuspended()) return (IJavaThread) threads[i];
		}
		throw new IllegalStateException();
	}

	public MyContentAssistProcessor getContentAssistProcessor() {
		return new MyContentAssistProcessor(project);
	}

}
