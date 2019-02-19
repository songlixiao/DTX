/**
 * 
 */
package org.dracarys.dtx.transaction.messager;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dracarys.dtx.transaction.manager.DistributedTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * 基于Redis的消息管理者
 * 
 * @author slx
 *
 */
public class RedisTxMessager implements TxMessagerInterface {

	protected transient Log						logger		= LogFactory.getLog(getClass());

	private Map<String, MessageListener>	listenerMap	= new HashMap<>();

	@Autowired
	protected RedisTemplate<?, ?>				redisTemplate;

	@Autowired
	private RedisMessageListenerContainer		redisMsgListenerContainer;

	@Override
	public void addListener(DistributedTransactionManager txm, String txId, DefaultTransactionStatus status,
			DataSource dataSource) {
		final Topic tp = new ChannelTopic(KEY_PREFIX_MSG_DTX_COMMITED + txId);
		final MessageListener ls = new MessageListener() {
			@Override
			public void onMessage(Message message, byte[] pattern) {
				boolean iscommit = (Boolean) redisTemplate.getValueSerializer().deserialize(message.getBody());
				logger.debug("RPCTX 子事务，收到事务提交通知：" + txId + "commit:" + iscommit);
				txm.processSubTx(iscommit, status, dataSource, txId);
			}
		};
		redisMsgListenerContainer.addMessageListener(ls, tp);
		logger.debug("RPCTX 子事务，开启事务监听：" + txId);
		listenerMap.put(txId, ls);
	}

	@Override
	public void removeListener(String txId) {
		MessageListener ls = listenerMap.get(txId);
		if (ls != null) {
			redisMsgListenerContainer.removeMessageListener(ls);
			listenerMap.remove(txId);
		}
	}

	@Override
	public void sendMessage(String txId, boolean doCommit) {
		redisTemplate.convertAndSend(KEY_PREFIX_MSG_DTX_COMMITED + txId, true);
	}
	
	@Override
	public boolean isProcessed(String txId) {
		// 找不到认为已经运行过
		return !listenerMap.containsKey(txId);
	}
}
