package raft.postvayler.samples._bank;

import java.io.Serializable;

import raft.postvayler.inject.Key;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class _Account implements Serializable {

	private static final long serialVersionUID = 1L;


	@Key({_Bank.class, _Customer.class})
	private int id;

}
