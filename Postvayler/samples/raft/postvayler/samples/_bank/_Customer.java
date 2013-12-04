package raft.postvayler.samples._bank;

import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Persistent;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class _Customer extends _Person {
	private static final long serialVersionUID = 1L;

	private int id;

	private final Map<Integer, _Account> accounts = new TreeMap<Integer, _Account>();

	public _Customer(String name) {
		super(name);
	}
	
	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Customer:" + id + ":" + getName();
	}

}
