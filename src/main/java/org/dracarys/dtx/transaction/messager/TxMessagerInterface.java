package org.dracarys.dtx.transaction.messager;

import javax.sql.DataSource;

import org.dracarys.dtx.transaction.manager.DistributedTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * 事物监听管理器
 * 
 * @author slx
 *
 */
public interface TxMessagerInterface {

	public static final String KEY_PREFIX_MSG_DTX_COMMITED = "distributed_tx_commited_";

	public void addListener(DistributedTransactionManager txm, String txId, DefaultTransactionStatus status, DataSource dataSource);

	public void removeListener(String txId);

	public void sendMessage(String txId, boolean doCommit);

	public boolean isProcessed(String txId);
}
