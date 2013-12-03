package raft.postvayler;

import java.io.File;


/**
 * Handle to persistent storage. After compilation @Root class can be casted to this interface. 
 * 
 * @author hakan eryargi (r a f t)
 */
public interface Storage {
	
	public File takeSnapshot() throws Exception;
}
