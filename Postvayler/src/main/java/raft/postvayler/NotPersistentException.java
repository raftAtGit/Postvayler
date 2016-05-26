package raft.postvayler;

/**
 * <p>Thrown if a Postvayler related operation performed on a @Persistent object and object has no id. This typically
 * happens if that object is created before Postvayler is created (i.e. {@link Postvayler#create()} is called)
 * and later a @Persist method is called on that object or it is passed as an argument to 
 * another @Persistent object's method.</p>
 * 
 * <p>As a rule of thumb, {@link Postvayler#create()} should be called before any other @Persistent 
 * objects are created.</p>
 * 
 * @see Persistent
 * @see Postvayler
 * 
 * @author r a f t
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
