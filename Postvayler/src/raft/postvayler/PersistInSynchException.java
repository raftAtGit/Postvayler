package raft.postvayler;

/**
 * <p>Thrown if a @Persist method is called inside a @Sync method. The call maybe direct or indirect. 
 * This is not allowed and will cause a deadlock.</p>
 * 
 * @author r a f t
 */
public class PersistInSynchException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PersistInSynchException() {
		super();
	}

}
