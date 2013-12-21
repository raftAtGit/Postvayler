package raft.postvayler.samples.bank;

import java.io.Serializable;

import raft.postvayler.Persistent;
import raft.postvayler.Persist;

/**
 * A person.
 * 
 * @author r a f t
 */
@Persistent
public class Person implements Serializable {
	private static final long serialVersionUID = 1L;

	private String name;
	private String phone;

	public Person() {
	}	
	public Person(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	@Persist
	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getName() {
		return name;
	}
	
	
	
}
