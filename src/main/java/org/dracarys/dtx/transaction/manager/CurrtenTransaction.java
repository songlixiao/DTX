package org.dracarys.dtx.transaction.manager;

/**
 * 当前线程事务属性
 * 
 * @author slx
 *
 */
public class CurrtenTransaction {
	/**
	 * 当前线程事务是否回滚
	 */
	private static ThreadLocal<Boolean>	isRollback				= new ThreadLocal<>();

	/**
	 * 当前线程是否是RPC事务的根事务
	 */
	private static ThreadLocal<Boolean>	isRpcRootTransaction	= new ThreadLocal<>();

	private static ThreadLocal<String>	TX_ID					= new ThreadLocal<>();

	public static Boolean isRollback() {
		return isRollback.get() == null ? false : isRollback.get();
	}

	public static void setRollback(Boolean rollback) {
		isRollback.set(rollback);
	}

	public static Boolean isRpcRootTransaction() {
		return isRpcRootTransaction.get() == null ? false : isRpcRootTransaction.get();
	}

	public static void setRpcRootTransaction(Boolean isRoot) {
		isRpcRootTransaction.set(isRoot);
	}

	public static String getTX_ID() {
		return TX_ID.get();
	}

	public static void setTX_ID(String txID) {
		TX_ID.set(txID);
	}

	public static void removeTxID() {
		TX_ID.remove();
	}
}
