package my.eclipse.repl.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class Promise {

	private Object[] args;
	private Object callback;
	private CountDownLatch hasResult = new CountDownLatch(1);

	public Promise(Class iface) {
		callback = Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getDeclaringClass() == Object.class) { return method.invoke(this, args); }
				Promise.this.args = args;
				hasResult.countDown();
				// if (method.getReturnType() == Void.TYPE) return null;
				if (method.getReturnType() == Boolean.class) return false;
				if (method.getReturnType().isPrimitive()) return 0;
				return null;
			}
		});
	}

	public Object callback() {
		return callback;
	}

	public Object[] await() {
		awaitResult();
		return args;
	}

	private void awaitResult() {
		while (args == null) {
			try {
				hasResult.await();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public static class Examples {

		@Test
		public void shouldWaitForCallback() {

			AsynchronousAPI api = new AsynchronousAPI();
			Promise result = new Promise(Listener.class);

			api.method((Listener) result.callback());
			String string = (String) result.await()[0];

			assertEquals(string, "Hello, worlds!");

		}

		class AsynchronousAPI {

			public void method(final Listener listener) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
						listener.notify("Hello, worlds!");
					}
				}).run();
			}

		}

		interface Listener {

			void notify(String result);

		}

	}

}
