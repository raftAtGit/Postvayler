package raft.postvayler.samples.bank;

import java.io.Serializable;

import raft.postvayler.inject.Key;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class Account implements Serializable {

	private static final long serialVersionUID = 1L;


	@Key({Bank.class, Customer.class})
	private int id;

}
