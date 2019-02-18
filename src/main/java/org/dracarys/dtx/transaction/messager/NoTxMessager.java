/**
 * 
 */
package org.dracarys.dtx.transaction.messager;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dracarys.dtx.transaction.manager.DistributedTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * 没有消息管理者（负责抛出异常）
 * 
 * @author slx
 *
 */
public class NoTxMessager implements TxMessagerInterface {

	protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public void addListener(DistributedTransactionManager txm, String txId, DefaultTransactionStatus status, DataSource dataSource) {
		throw new CannotCreateTransactionException("Current transaction is in RpcContext, but not set a TxMessager !");
	}

	@Override
	public void removeListener(String txId) {
		throw new CannotCreateTransactionException("Current transaction is in RpcContext, but not set a TxMessager !");
	}

	@Override
	public void sendMessage(String txId, boolean doCommit) {
		throw new CannotCreateTransactionException("Current transaction is in RpcContext, but not set a TxMessager !");
	}

	@Override
	public boolean isProcessed(String txId) {
		throw new CannotCreateTransactionException("Current transaction is in RpcContext, but not set a TxMessager !");
	}
}
