/**    
 * 文件名：DubboTransactionFilter.java
 *
 * 版本信息： V1.0
 * 日期：2018年8月17日
 * @author songlixiao_qd@163.com
 * Copyright BAHEAL 2015 版权所有    
 *     
 */
package org.dracarys.dtx.dubbo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dracarys.dtx.transaction.context.TxContextInterface;
import org.dracarys.dtx.transaction.manager.CurrtenTransaction;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * dubbo全局事务过滤器，负责传递全局事务ID <br/>
 * 过滤器的使用方法： <br/>
 * 1.在项目的 resources下创建 META-INFO文件夹。内添加文本文件com.alibaba.dubbo.rpc.Filter <br/>
 * 2.文件内容如下：DubboTx=org.dracarys.dtx.filter <br/>
 * 3.客户端需要传递事务时，声明dubbo服务引用需要添加过滤器 例如：
 * <xmp> 
 * <dubbo:reference id="testService1" interface="test.TestService" protocol="dubbo" filter="DubboTx" /> 
 * </xmp><br/>
 * 4.如果需要全局传递事务，可进行如下配置：
 * <xmp>
 * <dubbo:consumer filter="DubboTx" retries="0" /> 
 * </xmp> <b>建议不要启用这个配置，因为微服务调用中多数不需要全局控制事务，此配置会增加不必要的性能损耗。</b>
 * 
 * @ClassName DubboTransactionFilter
 * @author songlixiao_qd@163.com
 * @date 2018年8月17日 上午11:02:24
 * @ModifyRemarks
 * @version:V1.0
 * 
 */
public class DubboTransactionFilter implements Filter {

	protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		// 传递从ThreadLocal中拿到的ID，而不是生成新的ID再传递，考虑到调用端自身无事务情况
		logger.debug("DUBBOTX，传递参数TXID:" + CurrtenTransaction.getTX_ID());
		RpcContext.getContext().setAttachment(TxContextInterface.KEY_RPC_TX_ID, CurrtenTransaction.getTX_ID());
		return invoker.invoke(invocation);
	}
}
