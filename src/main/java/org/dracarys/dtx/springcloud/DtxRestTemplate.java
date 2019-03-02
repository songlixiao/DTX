/**    
 * 文件名：DtxRestTemplate.java
 *
 * 版本信息： V1.0
 * 日期：2019年2月20日
 * @author songlixiao_qd@163.com
 * Copyright BAHEAL 2015 版权所有    
 *     
 */
package org.dracarys.dtx.springcloud;

import java.io.IOException;
import java.net.URI;

import org.dracarys.dtx.transaction.context.TxContextInterface;
import org.dracarys.dtx.transaction.manager.CurrtenTransaction;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.web.client.RestTemplate;

/**
 * SpringCloud事务ID向下传递的工具（如需要向下层微服务传递事务，请使用此类）
 * 
 * @ClassName DtxRestTemplate
 * @author songlixiao_qd@163.com
 * @date 2019年2月20日 下午4:45:58
 * @ModifyRemarks
 * @version:V1.0
 * 
 */
public class DtxRestTemplate extends RestTemplate {

	@Override
	protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
		ClientHttpRequest request = super.createRequest(url, method);
		// 将当前的TxId放入到Header中，向下传递。
		request.getHeaders().set(TxContextInterface.KEY_RPC_TX_ID, CurrtenTransaction.getTX_ID());
		SpringCloudTxContext.RPCTX_CALLED.set(true);
		return request;
	}

}
