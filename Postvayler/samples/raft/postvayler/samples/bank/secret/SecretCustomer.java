package raft.postvayler.samples.bank.secret;

import raft.postvayler.Persistent;
import raft.postvayler.samples.bank.Customer;

@Persistent
public class SecretCustomer extends Customer {

	private static final long serialVersionUID = 1L;

	public SecretCustomer() {
		super("anonymous");
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}
}

