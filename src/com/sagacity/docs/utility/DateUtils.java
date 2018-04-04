package com.sagacity.docs.utility;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @类名字：DateUtils
 * @类描述：日期操作的公共方
 * @author:Carl.Wu
 * @版本信息：
 * @日期：2013-11-14
 * @Copyright 足下 Corporation 2013
 * @版权所有
 *
 */

public class DateUtils {
	/**
	 * 获得当前日期 格式:2008-01-01
	 *
	 * @return
	 */
	public static String nowDate() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.format(date);
	}

	/**
	 * 获得当前日期时间 格式:2008-01-01 12:00:00
	 *
	 * @return
	 */
	public static String nowDateTime() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format(date);
	}

	public static long getTimeLong(String str){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return  formatter.parse(str).getTime();
		}catch (Exception ex){
			return  0l;
		}
	}


	/**
	 * 把日期格式转换成String格式
	 *
	 * @param dateDate
	 * @return
	 */
	public static String dateToStr(Date dateDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = formatter.format(dateDate);
		return dateString;
	}

	/**
	 * 把日期格式转换成String格式
	 *
	 * @param dateDate
	 *
	 * @param type
	 *            要转换成Date类型的形式（例如："yyyy-MM-dd"，"yyyy-MM-dddd hh:mm:ss"）
	 * @return
	 */
	public static String dateToStr(Date dateDate, String type) {
		SimpleDateFormat formatter = new SimpleDateFormat(type);
		String dateString = formatter.format(dateDate);
		return dateString;
	}

	/**
	 * 获得当前时间(格式自定义)
	 *
	 * @param param
	 *            你想要让该方法给你返回什么形式的值，例如传入"yyyymmdd"返回0080101
	 * @return
	 */
	public static String nowParamDateTime(String param) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(param);
		return formatter.format(date);
	}

	/**
	 * 根据当前时间获得明天的时�?
	 *
	 * @return string
	 *
	 */
	public static String tomorrow_date() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		String tomorrow = new SimpleDateFormat("yyyy-MM-dd ").format(cal
				.getTime());
		return tomorrow;
	}

	/**
	 * 根据当前时间获得昨天的时间
	 *
	 * @return string
	 *
	 */
	public static String yesterday_date() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		String yesterday = new SimpleDateFormat("yyyy-MM-dd ").format(cal
				.getTime());
		return yesterday;
	}

	/**
	 * 把字符串形式的日期转换成标准的日期类
	 *
	 * @param str
	 *            传入字符串类型的日期
	 * @return 返回格式 2009-01-01
	 * @throws ParseException
	 */
	public static Date strToDate(String str) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = simpleDateFormat.parse(str);
		return date;

	}

	/**
	 * 把字符串形式的日期转换成标准的日期类
	 *
	 * @param str
	 *            传入字符串类型的日期
	 * @param type
	 *            要转换成Date类型的形式（例如：“yyyy-MM-dd”，“yyyy-MM-dd hh:mm:ss”）
	 * @return
	 * @throws ParseException
	 */
	public static Date strToDate(String str, String type) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(type);
		Date date = simpleDateFormat.parse(str);
		return date;

	}

	/**
	 * 根据当前时间的毫秒数返回当前日期
	 *
	 * @return
	 */
	public static Date millToDate() {
		long time = System.currentTimeMillis();
		Date dNow = new Date(time);
		return dNow;
	}

	/**
	 * 将毫秒数转换成String类型
	 *
	 * @param time
	 * @return
	 */
	public static String millToDate(long time) {
		Date dNow = new Date(time);
		return dateToStr(dNow);
	}

	/**
	 * 获取时间 小时:�?�?HH:mm:ss
	 *
	 * @return
	 */
	public static String getTimeShort() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		Date currentTime = new Date();
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	/**
	 * 得到现在时间
	 *
	 * @return
	 */
	public static Date getNow() {
		Date currentTime = new Date();
		return currentTime;
	}

	/**
	 * 得到现在小时
	 */
	public static String getHour() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = formatter.format(currentTime);
		String hour;
		hour = dateString.substring(11, 13);
		return hour;
	}
	/**
	 * 方法描述: 把时间格式化成小时<br>
	 */
	public static String getHour(String currentTime) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date  date=null;
		try {
			date = formatter.parse(currentTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String dateString = formatter.format(date);
		String hour;
		hour = dateString.substring(11, 13);
		return hour;
	}

	/**
	 * 得到现在分钟
	 *
	 * @return
	 */
	public static String getTime() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String dateString = formatter.format(currentTime);
		String min;
		min = dateString.substring(14, 16);
		return min;
	}

	/**
	 * 方法描述:把时间格式化成分种<br>
	 */
	public static String getTime(String currentTime) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date  date = null;
		try {
			date = formatter.parse(currentTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String dateString = formatter.format(date);
		String min;
		min = dateString.substring(14, 16);
		return min;
	}

	/**
	 * 得到二个日期间的间隔天数
	 *
	 * @param sj1
	 *            结束日期 格式009-12-31
	 * @param sj2
	 *            日期 格式009-11-31
	 * @return (结束日期-日期)
	 */
	public static String getTwoDay(String sj1, String sj2) {
		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
		long day = 0;
		try {
			Date date = myFormatter.parse(sj1);
			Date mydate = myFormatter.parse(sj2);
			day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
		} catch (Exception e) {
			return "";
		}
		return day + "";
	}

	/**
	 * 判断是否润年
	 *
	 * @param year
	 *
	 * @return
	 * @throws ParseException
	 */
	public static boolean isLeapYear(int year) throws ParseException {

		/**
		 * 详细设计�?1.�?00整除是闰年，否则�?2.不能�?整除则不是闰�?3.能被4整除同时不能�?00整除则是闰年
		 * 3.能被4整除同时能被100整除则不是闰�?
		 */
		if ((year % 400) == 0)
			return true;
		else if ((year % 4) == 0) {
			if ((year % 100) == 0)
				return false;
			else
				return true;
		} else
			return false;
	}

	/**
	 * @方法名:getEndDateOfMonth
	 * @方法描述：
	 * @author: Carl.Wu
	 * @return: String
	 * @version: 2013-11-14 下午2:53:11
	 */
	public static String getEndDateOfMonth(String dat) throws ParseException {
		String str = dat;
		str = str + "-";
		String month = dat.substring(5, 7);
		int mon = Integer.parseInt(month);
		if (mon == 1 || mon == 3 || mon == 5 || mon == 7 || mon == 8
				|| mon == 10 || mon == 12) {
			str += "31";
		} else if (mon == 4 || mon == 6 || mon == 9 || mon == 11) {
			str += "30";
		} else {
			String substring = dat.substring(0, 4);
			int parseInt = Integer.parseInt(substring);
			if (isLeapYear(parseInt)) {
				str += "29";
			} else {
				str += "28";
			}
		}
		return str;
	}

	/**
	 * 判断二个时间是否在同
	 *
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean isSameWeekDates(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		int subYear = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR);
		if (0 == subYear) {
			if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2
					.get(Calendar.WEEK_OF_YEAR))
				return true;
		} else if (1 == subYear && 11 == cal2.get(Calendar.MONTH)) {
			// 如果12月的�?���?��横跨来年第一周的话则�?���?��即算做来年的第一�?
			if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2
					.get(Calendar.WEEK_OF_YEAR))
				return true;
		} else if (-1 == subYear && 11 == cal1.get(Calendar.MONTH)) {
			if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2
					.get(Calendar.WEEK_OF_YEAR))
				return true;
		}
		return false;
	}

	/**
	 * 两个时间之间的间隔天
	 *
	 * @param date1
	 *            结束时间 格式009-12-31
	 * @param date2
	 *            时间 格式009-12-30
	 * @return
	 */
	public static long getDays(String date1, String date2) {
		if (date1 == null || date1.equals(""))
			return 0;
		if (date2 == null || date2.equals(""))
			return 0;
		// 转换为标准时
		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		Date mydate = null;
		try {
			date = myFormatter.parse(date1);
			mydate = myFormatter.parse(date2);
		} catch (Exception e) {
		}
		if (date!=null&&mydate!=null) {
			long day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
			return day;
		}else return 0;

	}

	// 返回当前年份的前两位
	public static String getLastYear() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
		return formatter.format(date).substring(2);
	}

	public static String getYear() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
		return formatter.format(date);
	}

	/**
	 * 方法名称: getLongDate<br>
	 * 方法描述: 返回当前日期 例如:20121128<br>
	 * 2012-11-27上午10:53:07
	 * CarlWu
	 * @return
	 * @throws ParseException
	 */
	public static long getLongDate()  {
		try {
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
			Date date = sdf1.parse(nowDate());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String s = sdf.format(date);
			Long longDate = Long.parseLong(s);
			return longDate;
		} catch (Exception e) {
			return -1;
		}


	}

	public static long getLongDateTime()  {
		try {
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = sdf1.parse(nowDateTime());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			String s = sdf.format(date);
			Long longDate = Long.parseLong(s);
			return longDate;
		} catch (Exception e) {
			return -1;
		}
	}

	public static long getLongDateMilliSecond()  {
		try {
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			String s = sdf.format(date);
			Long longDate = Long.parseLong(s);
			return longDate;
		} catch (Exception e) {
			return -1;
		}
	}

	public static void main(String argv[]){
		try {
			System.out.println(getTimeLong("2016-12-12 12:00:00"));
		}catch (Exception ex){
			ex.printStackTrace();
		}

	}

}