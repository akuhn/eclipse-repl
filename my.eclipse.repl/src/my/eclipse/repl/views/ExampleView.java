package my.eclipse.repl.views;

import java.util.ArrayList;
import java.util.List;

import my.eclipse.repl.Plugin;
import my.eclipse.repl.util.BullshitFree;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ExampleView extends ViewPart {

	public static final String ID = "my.eclipse.repl.views.ExampleView";

	private TreeViewer viewer;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	class TreeObject implements IAdaptable {
		private String name;
		private TreeParent parent;

		public TreeObject(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setParent(TreeParent parent) {
			this.parent = parent;
		}

		public TreeParent getParent() {
			return parent;
		}

		public String toString() {
			return getName();
		}

		public Object getAdapter(Class key) {
			return null;
		}
	}

	class TreeParent extends TreeObject {
		private ArrayList children;

		public TreeParent(String name) {
			super(name);
			children = new ArrayList();
		}

		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}

		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}

		public TreeObject[] getChildren() {
			return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
		}

		public boolean hasChildren() {
			return children.size() > 0;
		}
	}

	class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}

		public Object getParent(Object child) {
			if (child instanceof TreeObject) { return ((TreeObject) child).getParent(); }
			return null;
		}

		public Object[] getChildren(Object parent) {
			try {
				if (parent instanceof IJavaModel) return getJavaProjects(parent);
				if (parent instanceof IJavaProject) return getExampleClasses(parent);

				return new Object[0];

			} catch (JavaModelException exception) {
				throw new BullshitFree(exception);
			}
		}

		private Object[] getExampleClasses(Object parent) {
			try {
				List list = new ArrayList();
				IJavaProject project = (IJavaProject) parent;
				for (IPackageFragment pack: project.getPackageFragments()) {
					for (ICompilationUnit unit: pack.getCompilationUnits()) {
						for (IType type: unit.getTypes()) {
							for (IMethod each: type.getMethods()) {
								if (each.getAnnotation("Test").exists()) {
									list.add(each);
								}
							}
						}
					}
				}
				return list.toArray();
			} catch (JavaModelException exception) {
				throw new BullshitFree(exception);
			}
		}

		private IJavaProject[] getJavaProjects(Object parent) throws JavaModelException {
			return ((IJavaModel) parent).getJavaProjects();
		}

		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}

	}

	class ViewLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			try {
				if (obj instanceof IMethod) return getMethodName(obj);

				return obj.toString();

			} catch (JavaModelException exception) {
				throw new BullshitFree(exception);
			}
		}

		private String getMethodName(Object obj) throws JavaModelException {
			IMethod method = (IMethod) obj;
			String name = method.getElementName();
			return name.replaceAll("([^A-Z])([A-Z])", "$1 $2").toLowerCase();
		}

		public Image getImage(Object obj) {
			return null;
		}
	}

	class NameSorter extends ViewerSorter {
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getInput());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
	}

	private Object getInput() {
		return JavaCore.create(JavaPlugin.getWorkspace().getRoot());
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ExampleView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void makeActions() {
		makeResumeAction();

		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				showMessage("Double-click detected on " + obj.toString());
			}
		};
	}

	private void makeResumeAction() {
		action1 = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object element = ((IStructuredSelection) selection).getFirstElement();
				Plugin.getDefault().openEditorREPL(element);
			}
		};
		action1.setText("Resume REPL Session");
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Example View", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}