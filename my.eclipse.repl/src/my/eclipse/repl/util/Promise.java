package my.eclipse.repl.util;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;

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
				return null; // ASSUME return type is not primitive
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

	public static void main(String[] args) throws IOException {

		AsynchronousAPI api = new AsynchronousAPI();
		Promise result = new Promise(Callback.class);

		api.method((Callback) result.callback());
		String string = (String) result.await()[0];

		System.out.println(string);

	}

}

interface Callback {

	void callback(String result);

}

class AsynchronousAPI {

	public void method(final Callback callback) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				callback.callback("Hello, worlds!");
			}

		}).run();
	}

}
