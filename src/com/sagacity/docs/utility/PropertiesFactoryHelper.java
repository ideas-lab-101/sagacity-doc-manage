package com.sagacity.docs.utility;


import java.io.InputStream;
import java.util.Properties;

/**
 * @类名字：PropertiesFactoryHelper
 * @类描述：单例模式读取Properties属性文件
 * @author:Carl.Wu
 * @版本信息：
 * @日期：2013-9-27
 * @Copyright 足下 Corporation 2013 
 * @版权所有
 *
 */
public class PropertiesFactoryHelper {

	/**
	 * 私有的属性防止外部引用
	 */
	private static PropertiesFactoryHelper _instance = null;
	private Properties properties = new Properties();

	/**
	 * 私有的默认构造函数，防止使用构造函数进行实例化
	 */
	private PropertiesFactoryHelper() {
		try {
			InputStream inputStream = this.getClass().getResourceAsStream("/_config.properties");
			properties.load(inputStream);
			if (inputStream != null)
				inputStream.close();
		} catch (Exception e) {
			System.out.println(e + "file not found");
		}
	}

	/**
	 * @方法名:getInstance
	 * @方法描述：单例静态工厂方法，同步防止多线程环境同时执行
	 * @author: Carl.Wu
	 * @return: PropertiesFactoryHelper
	 * @version: 2013-9-27 下午11:05:15
	 */
	synchronized public static PropertiesFactoryHelper getInstance() {
		if (_instance == null) {
			_instance = new PropertiesFactoryHelper();
		}
		return _instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public PropertiesFactoryHelper clone() {
		return getInstance();
	}

	/**
	 * @方法名:getConfig
	 * @方法描述：读取配置信息key - value
	 * @author: Carl.Wu
	 * @return: String
	 * @version: 2013-9-27 下午11:05:30
	 */
	public String getConfig(String key) {
		return properties.getProperty(key);
	}

}