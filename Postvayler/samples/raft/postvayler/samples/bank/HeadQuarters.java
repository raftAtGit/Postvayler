package raft.postvayler.samples.bank;

import java.io.Serializable;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;

@Persistent
public class HeadQuarters implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String address;
	private String city;
	
	Bank bank;
	
	public String getAddress() {
		return address;
	}
	@Persist
	public void setAddress(String address) {
		this.address = address;
	}
	public String getCity() {
		return city;
	}
	@Persist
	public void setCity(String city) {
		this.city = city;
	}
	

}
