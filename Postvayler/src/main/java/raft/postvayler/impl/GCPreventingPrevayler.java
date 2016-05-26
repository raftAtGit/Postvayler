package raft.postvayler.impl;

import java.io.File;
import java.io.IOException;

import org.prevayler.Clock;
import org.prevayler.Prevayler;
import org.prevayler.Query;
import org.prevayler.SureTransactionWithQuery;
import org.prevayler.Transaction;
import org.prevayler.TransactionWithQuery;

/** 
 * Holds a reference to Transaction's before delegating call to another Prevayler. Combined with
 * references to target objects in {@link MethodTransaction} and {@link MethodTransactionWithQuery}, 
 * this safely prevents garbage collector cleaning our target before we are done. 
 * 
 * @author r a f t
 * @see MethodTransaction
 * @see MethodTransactionWithQuery
 * */
public class GCPreventingPrevayler implements Prevayler<IsRoot> {

	final Prevayler<IsRoot> delegate;
	final Prevayler<IsRoot> dummy = new NullPrevayler(); 
	
	public GCPreventingPrevayler(Prevayler<IsRoot> delegate) {
		this.delegate = delegate;
	}

	public IsRoot prevalentSystem() {
		return delegate.prevalentSystem();
	}

	public Clock clock() {
		return delegate.clock();
	}

	@Override
	public void execute(Transaction<? super IsRoot> transaction) {
		delegate.execute(transaction);
		dummy.execute(transaction);
	}
	
	@Override
	public <R> R execute(TransactionWithQuery<? super IsRoot, R> transactionWithQuery) throws Exception {
		R result = delegate.execute(transactionWithQuery);
		dummy.execute(transactionWithQuery);
		return result;
	}


	@Override
	public <R> R execute(Query<? super IsRoot, R> sensitiveQuery) throws Exception {
		R result = delegate.execute(sensitiveQuery);
		dummy.execute(sensitiveQuery);
		return result;
	}

	@Override
	public <R> R execute(SureTransactionWithQuery<? super IsRoot, R> sureTransactionWithQuery) {
		R result = delegate.execute(sureTransactionWithQuery);
		dummy.execute(sureTransactionWithQuery);
		return result;
	}

	@Override
	public File takeSnapshot() throws Exception {
		return delegate.takeSnapshot();
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}
	
	/** A Prevayler which does nothing :) */
	private class NullPrevayler implements Prevayler<IsRoot> {

		@Override
		public IsRoot prevalentSystem() {
			return null;
		}

		@Override
		public Clock clock() {
			return null;
		}

		@Override
		public void execute(Transaction<? super IsRoot> transaction) {
		}

		@Override
		public <R> R execute(Query<? super IsRoot, R> sensitiveQuery) throws Exception {
			return null;
		}

		@Override
		public <R> R execute(TransactionWithQuery<? super IsRoot, R> transactionWithQuery) throws Exception {
			return null;
		}

		@Override
		public <R> R execute(SureTransactionWithQuery<? super IsRoot, R> sureTransactionWithQuery) {
			return null;
		}

		@Override
		public File takeSnapshot() throws Exception {
			return null;
		}

		@Override
		public void close() throws IOException {
		}
		
	}

}
