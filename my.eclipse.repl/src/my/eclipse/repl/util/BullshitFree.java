package my.eclipse.repl.util;

import java.lang.reflect.Field;

/**
 * Calling bullshit on this language and its shitty type checker.
 * 
 */
public class BullshitFree extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BullshitFree(Exception exception) {
		super(exception);
	}

	public static <T> T getField(Object object, String name) {
		try {
			Field field = object.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(object);
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

	public static void setField(Object object, String name, Object value) {
		try {
			Field field = object.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(object, value);
		} catch (Exception exception) {
			throw new BullshitFree(exception);
		}
	}

}
