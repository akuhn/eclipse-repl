package nov13;

public class BullshitFree extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BullshitFree(Exception exception) {
		super(exception);
	}

}
