package my.eclipse.repl;

import java.util.Arrays;
import java.util.Iterator;

public class StringList implements Iterable<String> {

	private String[] list;
	private int size;

	public StringList() {
		this.list = new String[10];
	}

	public StringList(String[] array) {
		this.list = array;
	}

	public StringList add(String element) {
		if (size == list.length) needMore(1);
		list[size++] = element;
		return this;
	}

	private void needMore(int more) {
		int capacity = list.length + Math.max(more, list.length);
		String[] array = new String[capacity];
		System.arraycopy(list, 0, array, 0, size);
		this.list = array;
	}

	public StringList add(String... elements) {
		if (size + elements.length > list.length) needMore(elements.length);
		System.arraycopy(elements, 0, list, size, elements.length);
		size += elements.length;
		return this;
	}

	public String[] asArray() {
		String[] array = new String[size];
		System.arraycopy(list, 0, array, 0, size);
		return array;
	}

	@Override
	public Iterator<String> iterator() {
		return Arrays.asList(list).iterator();
	}

}
