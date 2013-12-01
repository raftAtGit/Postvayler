package raft.postvayler;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class NotPersistentException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotPersistentException() {
		super();
	}

	public NotPersistentException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotPersistentException(String message) {
		super(message);
	}

	public NotPersistentException(Throwable cause) {
		super(cause);
	}
	
	

}
