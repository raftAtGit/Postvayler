package raft.postvayler.inject;

import java.io.Serializable;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public interface Locator<P, C> extends Serializable {

	public C findObject(P root) throws Exception;
}
