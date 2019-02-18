/**
 * 
 */
package org.dracarys.dtx.transaction.context;

/**
 * @author slx
 *
 */
public interface TxContextInterface {

	/**
	 * 上下文中保存TxId的key
	 */
	public static final String KEY_RPC_TX_ID = "KEY_RPC_TX_ID";

	/**
	 * 是否在RPC环境中
	 * 
	 * @return
	 */
	public boolean isInRpc();

	/**
	 * 从上下文中获取TxID
	 * 
	 * @return
	 */
	public String getTxIDFromContext();
}
