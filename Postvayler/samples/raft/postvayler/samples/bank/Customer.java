package raft.postvayler.samples.bank;

import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Persistent;
import raft.postvayler.inject.Key;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class Customer extends Person {
	private static final long serialVersionUID = 1L;

	@Key(Bank.class)
	private int id;
	

	private final Map<Integer, Account> accounts = new TreeMap<Integer, Account>();

	public Customer(String name) {
		super(name);
		
		// pseudo injected code
		
		// if (context.isBound) {
		//    if (context.inTransaction) {
		//       id = context.root.put(this);
		//    }  else {
		// 
		//        context.inTransaction = true
		//        try {
		//           id = prevayler.execute(new Tx(context.root.put(this)));
		//        } finally {
		//           context.inTransaction = false
		//        }
		//    }
		// } else if (context.inRecovery) {
		//   id = context.preRoot.put(this);
		// } else {
		//    // -> object is not persistent 
		// }
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
