package my.eclipse.repl.eval;

public class Result {

	protected String expression;
	protected String print;
	protected String kind;

	public Result(String expression) {
		this.expression = expression;
	}

	public String toPrintString() {
		return print;
	}

	public boolean hasErrors() {
		return kind.startsWith("err");
	}

	public String getExpression() {
		return expression;
	}

}
