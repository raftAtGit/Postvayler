package raft.postvayler.samples.bank;

import java.io.Serializable;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;

/**
 * A company.
 * 
 * @author r a f t
 */
@Persistent 
public class Company implements Serializable {

	private static final long serialVersionUID = 1L;

	private RichPerson owner = new RichPerson("<no name>");
	
	public Company() {
	}

	public RichPerson getOwner() {
		return owner;
	}

	@Persist
	public void setOwner(RichPerson newOwner) {
		if (this.owner != null) {
			this.owner.removeCompany(this);
		}
		this.owner = newOwner;
		
		if (newOwner != null) {
			newOwner.addCompany(this);
		}
	}
	
}
