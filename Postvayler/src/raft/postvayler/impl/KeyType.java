package raft.postvayler.impl;

import java.io.Serializable;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
class KeyType implements Serializable{

	private static final long serialVersionUID = 1L;
	
	final Object[] values;

	KeyType(Object[] values) {
		this.values = values;
	}
}
