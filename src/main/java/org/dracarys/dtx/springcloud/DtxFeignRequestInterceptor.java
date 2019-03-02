package org.dracarys.dtx.springcloud;

import org.dracarys.dtx.transaction.context.TxContextInterface;
import org.dracarys.dtx.transaction.manager.CurrtenTransaction;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Spring Cloud 客户端用Feign时，添加调用拦截，传递TxID。
 * 
 * @ClassName DtxFeignRequestInterceptor
 * @author songlixiao_qd@163.com
 * @date 2019年2月25日 下午5:27:33
 * @ModifyRemarks
 * @version:V1.0
 *
 */
public class DtxFeignRequestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate template) {
		template.header(TxContextInterface.KEY_RPC_TX_ID, CurrtenTransaction.getTX_ID());
		SpringCloudTxContext.RPCTX_CALLED.set(true);
	}

}
