/**
 * 
 */
package org.dracarys.dtx.transaction.context;

import com.alibaba.dubbo.rpc.RpcContext;

/**
 * Dubbo 事务上下文
 * @author slx
 *
 */
public class DubboTxContext implements TxContextInterface {

	@Override
	public boolean isInRpc() {
		if (RpcContext.getContext().getUrl() == null)
			return false;
		return RpcContext.getContext().isConsumerSide() || RpcContext.getContext().isProviderSide();
	}

	@Override
	public String getTxIDFromContext() {
		return RpcContext.getContext().getAttachment(KEY_RPC_TX_ID);
	}

}
