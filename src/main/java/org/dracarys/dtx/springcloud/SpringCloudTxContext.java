/**    
 * 文件名：SpringCloudTxContext.java
 *
 * 版本信息： V1.0
 * 日期：2019年2月20日
 * @author songlixiao_qd@163.com
 * Copyright BAHEAL 2015 版权所有    
 *     
 */
package org.dracarys.dtx.springcloud;

import org.dracarys.dtx.transaction.context.TxContextInterface;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * SpringCloud 事务上下文
 * 
 * @ClassName SpringCloudTxContext
 * @author songlixiao_qd@163.com
 * @date 2019年2月20日 下午4:50:08
 * @ModifyRemarks
 * @version:V1.0
 * 
 */
public class SpringCloudTxContext implements TxContextInterface {

	public static ThreadLocal<Boolean> RPCTX_CALLED = new ThreadLocal<>();

	@Override
	public boolean isInRpc() {
		return getTxIDFromContext() != null || (RPCTX_CALLED.get() != null && RPCTX_CALLED.get());
	}

	@Override
	public String getTxIDFromContext() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return null;
		}
		if (((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest() == null) {
			return null;
		}
		return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader(KEY_RPC_TX_ID);
	}

}
