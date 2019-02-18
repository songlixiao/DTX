package org.dracarys.dtx.transaction.context;

/**
 * 不在RPC环境中时的 事务上下文
 * @author slx
 *
 */
public class NoRpcTxContext implements TxContextInterface {

	@Override
	public boolean isInRpc() {
		return false;
	}

	@Override
	public String getTxIDFromContext() {
		return null;
	}

}
