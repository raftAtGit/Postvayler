package raft.postvayler;

/**
 * A @Persist method is called inside a @Sync method. This is not allowed and will cause a deadlock.
 * 
 * @author r a f t
 */
public class PersistInSynchException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PersistInSynchException() {
		super();
	}

}
