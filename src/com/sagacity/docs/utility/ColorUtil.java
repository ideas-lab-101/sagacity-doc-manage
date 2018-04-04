package com.sagacity.docs.utility;

import java.awt.*;
import java.util.Random;

public class ColorUtil {

    public static String getColorRandom() {
        String r, g, b;

        //生成随机对象
        Random random = new Random();
        //生成红色颜色代码
        r = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成绿色颜色代码
        g = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成蓝色颜色代码
        b = Integer.toHexString(random.nextInt(256)).toUpperCase();

        //判断红色代码的位数
        r = r.length()==1 ? "0" + r : r ;
        //判断绿色代码的位数
        g = g.length()==1 ? "0" + g : g ;
        //判断蓝色代码的位数
        b = b.length()==1 ? "0" + b : b ;

        return "#"+r+g+b;
    }
}
