package raft.postvayler;

/**
 * <p>Thrown if a @Persistent class is not correctly instrumented. Class maybe either 
 * not instrumented at all or instrumented for a different Root class other than current 
 * {@link Postvayler}'s root.</p>
 * 
 * <p>Note, if there is no Postvayler context around (i.e. {@link Postvayler#create()} not called),
 * this exception is never thrown and @Persistent classes behave like they are not instrumented at all.</p>
 * 
 * @see Persistent
 * @see Postvayler
 * 
 * @author r a f t
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
