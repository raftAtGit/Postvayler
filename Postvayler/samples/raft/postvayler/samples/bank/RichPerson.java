package raft.postvayler.samples.bank;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import raft.postvayler.Persistent;
import raft.postvayler.Synch;

/**
 * A rich person who owns companies and banks.
 * 
 * @author r a f t
 */
@Persistent
public class RichPerson extends Person {

	private static final long serialVersionUID = 1L;
	
	/** we cannot use a regular HashSet since the iteration order is not deterministic */
	private final Set<Company> companies = new LinkedHashSet<Company>();
	
	/** we cannot use a regular HashSet since the iteration order is not deterministic */
	private final Set<Bank> banks = new LinkedHashSet<Bank>();
	
	public RichPerson(String name) {
		super(name);
	}

	@Synch
	public List<Bank> getBanks() {
		return new ArrayList<Bank>(banks);
	}

	boolean addCompany(Company company) {
		boolean result = companies.add(company);
		if (company instanceof Bank) 
			banks.add((Bank)company);
		return result;
	}

	boolean removeCompany(Company company) {
		banks.remove(company);
		return companies.remove(company);
	}
}
