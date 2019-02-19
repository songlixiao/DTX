package org.dracarys.dtx.transaction.messager;

import javax.sql.DataSource;

import org.dracarys.dtx.transaction.manager.DistributedTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * 事务监听管理器接口。
 * 负责事务提交或回滚消息的发送或监听处理
 * @author slx
 *
 */
public interface TxMessagerInterface {

	public static final String KEY_PREFIX_MSG_DTX_COMMITED = "distributed_tx_commited_";

	/**
	 * 监听指定的事务ID，并在收到通知后进行相关的处理。
	 * @param txm
	 * @param txId
	 * @param status
	 * @param dataSource
	 */
	public void addListener(DistributedTransactionManager txm, String txId, DefaultTransactionStatus status, DataSource dataSource);

	/**
	 * 删除对指定事务Id的监听
	 * @param txId
	 */
	public void removeListener(String txId);

	/**
	 * 发送事务提交或回滚的消息
	 * @param txId
	 * @param doCommit
	 */
	public void sendMessage(String txId, boolean doCommit);

	/**
	 * 返回当前txId的事务是否已经处理过，防止重复处理
	 * @param txId
	 * @return
	 */
	public boolean isProcessed(String txId);
}
