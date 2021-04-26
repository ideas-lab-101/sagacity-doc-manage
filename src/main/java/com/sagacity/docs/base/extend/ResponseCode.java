package com.sagacity.docs.base.extend;
/**
 * 常用返回状态码
 * @author Mula.liu
 *
 */
public final class ResponseCode {

	public final static String MSG = "msg";
	
	public final static String RESULT = "result";

	public final static  String CODE = "code"; //0：失败; 1：成功；6：token失效

	public final static String PAGE = "page";

	public final static String LIST = "list";

	public final static String DATA = "data"; //返回的单数据包

	public final static String PageSize = "pageSize"; //返回list的单页数据数。

	public final static String PageIndex = "pageIndex"; //返回list的当前页码，页码计数从0开始。

	public final static String PageNumber = "pageNumber";

	public final static String TotalPage = "totalPage"; //返回list的分页总数。

	public final static String LastPage = "lastPage";

	public final static String TotalRow = "totalRow";
}
