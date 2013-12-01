package raft.postvayler;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class NotCompiledException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotCompiledException() {
		super();
	}

	public NotCompiledException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotCompiledException(String message) {
		super(message);
	}

	public NotCompiledException(Throwable cause) {
		super(cause);
	}
	
	

}
