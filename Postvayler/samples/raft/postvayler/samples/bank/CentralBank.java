package raft.postvayler.samples.bank;

import raft.postvayler.Persistent;

/**
 * Central bank.
 * 
 * @author r a f t
 */
@Persistent
public class CentralBank extends Bank {

	private static final long serialVersionUID = 1L;

	public CentralBank() {
		super();
	}

}
