package com.sagacity.docs.common;


import com.jfinal.config.*;
import com.jfinal.core.Const;
import com.jfinal.core.JFinal;
import com.jfinal.ext.plugin.quartz.QuartzPlugin;
import com.jfinal.ext.plugin.sqlinxml.SqlInXmlPlugin;
import com.jfinal.ext.plugin.tablebind.AutoTableBindPlugin;
import com.jfinal.ext.plugin.tablebind.SimpleNameStyles;
import com.jfinal.ext.route.AutoBindRoutes;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.ehcache.EhCachePlugin;

/**
 * API引导式配置
 */
public class SystemConfig extends JFinalConfig {
	
	/**
	 * 配置常量
	 */
	public void configConstant(Constants me) {
		// 加载少量必要配置，随后可用getProperty(...)获取值
		loadPropertyFile("mysql_config.txt");
		me.setDevMode(getPropertyToBoolean("devMode", false));

		//默认10M,此处设置为最大1000M
		me.setMaxPostSize(100 * Const.DEFAULT_MAX_POST_SIZE);
        //设置上传文件的基地址,如果不设置会上传到项目根目录的upload/目录下
        me.setBaseUploadPath("/");

		me.setError500View("/500Page.html"); //出错页面
		me.setError404View("/404Page.html"); //找不到页面
	}
	
	/**
	 * 配置路由
	 */
	public void configRoute(Routes me) {
        //配置自动路由
		//me.add(new AutoBindRoutes());
		AutoBindRoutes route = new AutoBindRoutes();
		route.includeAllJarsInLib(false);
		me.add(route);
	}
	
	/**
	 * 配置插件
	 */
	public void configPlugin(Plugins me) {
		// 配置C3p0数据库连接池插件
//		C3p0Plugin c3p0Plugin = new C3p0Plugin(getProperty("jdbcUrl"), getProperty("user"), getProperty("password").trim());
//		me.add(c3p0Plugin);

		// 配置Druid数据库连接池
        DruidPlugin druidPlugin = new DruidPlugin(getProperty("jdbcUrl"), getProperty("user"), getProperty("password"));
        druidPlugin.setFilters("stat,log4j");
		me.add(druidPlugin);


		// 配置TableBind插件
		AutoTableBindPlugin autoTableBindPlugin=new AutoTableBindPlugin(druidPlugin, SimpleNameStyles.LOWER);
		autoTableBindPlugin.setShowSql(true);//显示sql查询语句
		me.add(autoTableBindPlugin);
		
		//配置EHcache缓存插件
		EhCachePlugin cachePlugin=new EhCachePlugin(SystemConfig.class.getResource("ehcache.xml"));
		me.add(cachePlugin);
		
		//注册sql配置文件
		SqlInXmlPlugin sqlInXmlPlugin=new SqlInXmlPlugin();
		me.add(sqlInXmlPlugin);
		
		//注册定时器插件
		QuartzPlugin qzPlugin=new QuartzPlugin("job.properties");
		me.add(qzPlugin);
				
	}
	
	/**
	 * 配置全局拦截器
	 */
	public void configInterceptor(Interceptors me) {
		//me.add(new SessionInViewInterceptor()); //未采用系统session,改用ehCache
		//不再配置全局拦截器，web端与微信端分别拦截
		//me.add(new LoginInterceptor());

	}
	
	/**
	 * 配置处理器
	 */
	public void configHandler(Handlers me) {
		//me.add(new ServletHandler());
        //将webSocket请求单独处理
		//me.add(new WebSocketHandler("^/websocket"));
	}
	
	/**
	 * 建议使用 JFinal 手册推荐的方式启动项目
	 * 运行此 main 方法可以启动项目，此main方法可以放置在任意的Class类定义中，不一定要放于此
	 */
	public static void main(String[] args) {
		JFinal.start("WebRoot", 80, "/", 2);
	}
}
