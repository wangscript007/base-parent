package com.application.base.boot.result;

import com.application.base.boot.listener.BaseSpringContextRefreshListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @desc 子系统结果消息监听器
 * @ClassName:  ResultMessageListener
 * @author 孤狼
 */
public class ResultMessageListener extends BaseSpringContextRefreshListener {

	private Logger logger = LoggerFactory.getLogger(ResultMessageListener.class);
	
	private String resultMessage;
	
	public String getResultMessage() {
		return resultMessage;
	}

	public void setResultMessage(String resultMessage) {
		this.resultMessage = resultMessage;
	}

	/**
	 * 向系统的结果消息容器中添加自定义配置
	 * <p>Title: doListen</p>   
	 * <p>Description: </p>   
	 * @param event   
	 */
	@Override
	protected void doListen(ContextRefreshedEvent event) {
		//获得集合中的对象bean
		MessageContext result = event.getApplicationContext().getBean("resultMessageContext", MessageContext.class);
		logger.debug("[<============初始化子系统结果信息开始============>]");
		result.addMessageResource(resultMessage);
		logger.debug("[<============初始化子系统结果信息结束============>]");
	}
	
}
