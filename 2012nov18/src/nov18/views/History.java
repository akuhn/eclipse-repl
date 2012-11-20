package nov18.views;

import java.util.ArrayList;
import java.util.List;

public class History {

	private List<String> list = new ArrayList();
	private int index = 0;

	public void add(String line) {
		list.add(line);
		index = list.size();
	}

	public String previous() {
		if (index > 0) index--;
		return list.get(index);
	}

	public String next() {
		if (index < list.size()) index++;
		if (index == list.size()) return "";
		return list.get(index);
	}

}
