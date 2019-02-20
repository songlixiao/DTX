/**    
 * 文件名：DistributedTransactionManager.java
 *
 * 版本信息： V1.0
 * 日期：2018年8月16日
 * @author songlixiao_qd@163.com
 *     
 */
package org.dracarys.dtx.transaction.manager;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.sql.DataSource;

import org.dracarys.dtx.transaction.context.NoRpcTxContext;
import org.dracarys.dtx.transaction.context.TxContextInterface;
import org.dracarys.dtx.transaction.messager.NoTxMessager;
import org.dracarys.dtx.transaction.messager.TxMessagerInterface;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * RPC环境下的事务管理器。用于在RPC微服务环境中，能够将整个微服务调用过程中包含在同一个事务当中。 <br/>
 * <b>注意：仅生效与RPC协议下且原来使用 org.springframework.jdbc.datasource.DataSourceTransactionManager进行事务管理的情况。</b>
 * <P>
 * 使用说明： <br/>
 * 1.修改spring配置文件，将事务管理器改为本类。 <br/>
 * 2.在需要向下（方法内部调用的RPC服务）传递事务时，需要将本类中的事务ID（DistributedTransactionManager.TX_ID.get()）向下传递。如果不向下传递，则服务方的事务将视为独立事务。
 * 	当前已经提供了Dubbo版本的事务传递过滤器,详见：${@link org.dracarys.dtx.dubbo.DubboTransactionFilter}。<br/>
 * 3.服务方本身也需要启用事务管理。否则即使使用了DistributedTransactionManager向下传递了事务，服务方仍无法参与到外层的事务当中。<br/>
 * 配置示例如下： <xmp> 
 * <!-- DubboTransactionManager需要通过Redis传递和接收“提交”、“回滚”通知 。（详细redis配置略）-->
 * <bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate">
 * 	<property name="connectionFactory" ref="jedisConnFactory"/>
 * 	<property name="keySerializer" ref="stringRedisSerializer"/>
 * 	<property name="valueSerializer" ref="stringRedisSerializer"> 
 * </bean>
 * <bean id="redisMessageListenerContainer" class="org.springframework.data.redis.listener.RedisMessageListenerContainer" >
 * 	<property name="connectionFactory" ref="jedisConnFactory"></property>
 * </bean>
 * <bean id="transactionManager" class="org.dracarys.dtx.transaction.DistributedTransactionManager" p:dataSource-ref="dataSource" /> 
 * </xmp>
 * </P>
 * <P>
 * 实现原理： <br/>
 * 覆盖了原spring单数据源事务管理器的主要方法，包括：事务开始，事务提交，事务回滚，事务销毁。 <br/>
 * 在事务开始时，从当前线程中获取RPC事务id（TxId），如果当前线程没有，则从RPC上下文中获取，如果仍获取不到，则认为当前事务为顶级事务（根事务）并创建TxId。<br/>
 * 如果当前事务在RPC环境中并且不是根事务，那么当前事务的提交和回滚动作，实际并不执行。而是开启一个针对当前TxId的监听，等待根事务通知来进行实际的动作。<b>（监听暂时是用Redis发布订阅实现的。）</b>
 * <br/>
 * 当出现下面情况时，当前事务的处理直接使用原来spring的处理逻辑： <br/>
 * 1.当前事务不在 RPC环境中。 2.当前事务是根事务。 3.当前事务抛出异常。
 * </P>
 * 
 * @ClassName DistributedTransactionManager
 * @author songlixiao_qd@163.com
 * @date 2018年8月16日 上午11:25:54
 * @ModifyRemarks
 * @version:V1.0
 * 
 */
public class DistributedTransactionManager extends DataSourceTransactionManager {

	private static final long				serialVersionUID		= 1L;

	/**
	 * 当前线程事务是否回滚
	 */
	protected static ThreadLocal<Boolean>	isRollback				= new ThreadLocal<>();

	/**
	 * 当前线程是否是RPC事务的根事务
	 */
	protected static ThreadLocal<Boolean>	isRpcRootTransaction	= new ThreadLocal<>();

	public static ThreadLocal<String>		TX_ID					= new ThreadLocal<>();

	/**
	 * 事务消息管理器
	 */
	private TxMessagerInterface				txMessager				= new NoTxMessager();

	/**
	 * 事务环境
	 */
	private TxContextInterface				txContext				= new NoRpcTxContext();

	/**
	 * 等待RPC根事务通知超时时间（超时后没有收到消息当前事务回滚）
	 */
	private Long							waitRpcTxMessageTimeout	= 1000L * 10;

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		isRollback.set(false);
		isRpcRootTransaction.set(false);
		super.doBegin(transaction, definition);
		// 不管事务开启时，是否在RPC上下文中，都需要创建RPC事务ID，以防止在事务过程中，调用其他RPC服务。
		// 防止当前线程内包含多个独立事务，造成它们使用相同的TxID，事务开启必须清除之前事务的ID。(如果使用了相同的事务ID，这些独立事务中调用了Dubbo服务后，会造成后继的事务监听事务状态时发生错乱。）
		TX_ID.remove();
		// 当调用Dubbo服务后，过滤器会向后传递事务ID。
		getRpcTxID();
	}

	@Override
	protected void doCommit(final DefaultTransactionStatus status) {
		isRollback.set(false);
		if (!isInRpc()) {
			// 不在RPC环境中才需要真正提交
			logger.debug("RPCTX 不在RPC环境，本地事务提交：" + getRpcTxID());
			super.doCommit(status);
		} else {
			// 在RPC环境中，需要等待RPC环境的事务指令再处理。
			final String txId = getRpcTxID();
			if (isRpcRootTransaction.get()) {
				// 如果自己是RPC链条中的顶级事务，需要发出全局提交指令，并自己提交事务。
				logger.debug("RPCTX 根事务 RCP环境向子事务发送提交通知，并提交本地事务：" + txId);
				txMessager.sendMessage(txId, true);
				super.doCommit(status);
			} else {
				final DataSource dataSource = this.getDataSource();
				// 如果自己是RPC中的子事务，需要订阅事务状态，然后再进一步处理。
				txMessager.addListener(this, txId, status, dataSource);
				logger.debug("RPCTX 子事务，开启事务监听：" + getRpcTxID());
				// 如果长时间没收到根事务的 提交或回滚通知，则默认回滚本地事务
				new Timer(true).schedule(new TimerTask() {
					@Override
					public void run() {
						logger.debug("RPCTX 子事务，等待根事务通知超时，自动回滚：" + txId);
						processSubTx(false, status, dataSource, txId);
					}
				}, waitRpcTxMessageTimeout);
			}
		}
	}

	/**
	 * 处理当前事务，可被当前线程或其他线程（事务消息管理线程，或者超时检测定时器线程）调用
	 * 
	 * @param iscommit
	 * @param status
	 * @param dataSource
	 * @param txId
	 */
	public void processSubTx(boolean iscommit, DefaultTransactionStatus status, DataSource dataSource, String txId) {

		// txId是UUID创建或从RPC上下文获取后放入TreadLocal，并在各方法中传递，不会出现value相同引用不同的情况，因此synchronized不会出现锁不住情况
		// 防止多线程（事务消息管理线程，或者超时检测定时器线程）同时操作事务
		synchronized (txId) {
			if (txMessager.isProcessed(txId)) {
				// 已经处理过，不再进行处理。
				return;
			}
			if (iscommit) {
				super.doCommit(status);
			} else {
				super.doRollback(status);
			}
			Object transaction = status.getTransaction();
			// 销毁事务时，如果newConnectionHolder是true内部将会抛出异常（因为当前线程已经不是事务之前的线程）
			Field newHolderField = ReflectionUtils.findField(transaction.getClass(), "newConnectionHolder");
			ReflectionUtils.makeAccessible(newHolderField);
			ReflectionUtils.setField(newHolderField, transaction, false);
			doDubboTxCleanup(transaction, dataSource);
			txMessager.removeListener(txId);
			logger.debug("RPCTX 子事务，处理完毕，移除监听并销毁本地事务：" + txId);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		// 此方法被调用，一定是当前事务中抛出异常导致回滚。（不管是否在RPC环境中都需要继续执行）
		isRollback.set(true);
		if (isRpcRootTransaction.get()) {
			logger.debug("RPCTX 根事务 在RPC环境 向子事务发出回滚通知：" + getRpcTxID());
			// 如果是RPC 顶级事务回滚，则发出全局回滚指令。
			txMessager.sendMessage(getRpcTxID(), false);
		}
		super.doRollback(status);
		logger.debug("RPCTX 回滚本地事务：" + getRpcTxID());
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {

		if (!isInRpc()) {
			// 不在RPC环境中，与原来逻辑相同
			logger.debug("RPCTX 不再RPC环境中，销毁本地事务管理器：" + getRpcTxID());
			super.doCleanupAfterCompletion(transaction);
		} else {
			// 在RPC环境中，当前线程事务因异常回滚，与原来事务结束处理相同，结束事务。
			if (isRollback.get()) {
				logger.debug("RPCTX 事务在RPC环境中本地发生回滚，销毁本地事务管理器：" + getRpcTxID());
				super.doCleanupAfterCompletion(transaction);
			} else {
				if (isRpcRootTransaction.get()) {
					logger.debug("RPCTX 根事务，提交。销毁本地事务管理器：" + getRpcTxID());
					super.doCleanupAfterCompletion(transaction);
				} else {
					// 不是根事务不要销毁事务，等待RPC上下文中进一步的指令
					logger.debug("RPCTX 子事务，提交，暂不销毁本地事务管理器，等待根事务指令或超时：" + getRpcTxID());
				}
			}
		}
		// 本地事务不需要传递txid，RPC事务已经发送了通知，也不需要txid了
		TX_ID.remove();
	}

	public void doDubboTxCleanup(Object transaction, DataSource dataSource) {
		super.doCleanupAfterCompletion(transaction);
		JdbcTransactionObjectSupport tx = (JdbcTransactionObjectSupport) transaction;
		// 将jdbConnection归还连接池
		DataSourceUtils.releaseConnection(tx.getConnectionHolder().getConnection(), dataSource);
	}

	protected boolean isInRpc() {
		return txContext.isInRpc();
	}

	protected String getRpcTxID() {
		String txId = TX_ID.get();
		// 当前线程中没有ID，则从RPC上下文中取，如果RPC上下文也没有，则生成新的的ID放入当前线程中。（RPC上下文中的ID传递使用RPC过滤器实现）
		if (!StringUtils.hasText(txId)) {
			txId = txContext.getTxIDFromContext();
			if (!StringUtils.hasText(txId)) {
				txId = UUID.randomUUID().toString();
				isRpcRootTransaction.set(true);
				logger.debug("RPCTX 未读取到RPC上下文中的事务ID，生成新的事务ID，当前线程作为根事务。" + txId);
			}
			TX_ID.set(txId);
		}

		return txId;
	}
}
