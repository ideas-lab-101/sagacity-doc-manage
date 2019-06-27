var active = {
    tabAdd: function (name, url, id) {
        //新增一个Tab项
        element.tabAdd('main', {
            title: name //用于演示
            , content: '<iframe data-frameid="'+id+'" frameborder="0" name="content" scrolling="auto" width="100%" src="' + url + '"></iframe>'
            , id: id //实际使用一般是规定好的id，这里以时间戳模拟下
        })
        customRightClick(id);//绑定右键菜单
        FrameWH();//计算框架高度
    }
    , tabChange: function (id) {
        //切换到指定Tab项
        element.tabChange('main', id); //切换
        $("iframe[data-frameid='"+id+"']").attr("src",$("iframe[data-frameid='"+id+"']").attr("src"))//切换后刷新框架
    }
    , tabDelete: function (id) {
        element.tabDelete("main", id);//删除
    }
    , tabDeleteAll: function (ids) {//删除所有
        $.each(ids, function (i,item) {
            element.tabDelete("main", item);
        })
    }
};

function customRightClick(id) {
    //取消右键
    $('.layui-tab-title li').on('contextmenu', function () { return false; })
    $('.layui-tab-title,.layui-tab-title li').click(function () {
        $('.rightmenu').hide();
    });
    //桌面点击右击
    $('.layui-tab-title li').on('contextmenu', function (e) {
        var popupmenu = $(".rightmenu");
        popupmenu.find("li").attr("data-id", $(this).attr("lay-id"));
        l = ($(document).width() - e.clientX) < popupmenu.width() ? (e.clientX - popupmenu.width()) : e.clientX;
        t = ($(document).height() - e.clientY) < popupmenu.height() ? (e.clientY - popupmenu.height()) : e.clientY;
        popupmenu.css({ left: l, top: t }).show();
        //alert("右键菜单")
        return false;
    });
}

$(".rightmenu li").click(function () {
    if ($(this).attr("data-type") == "close-this") {
        active.tabDelete($(this).attr("data-id"))
    } else if ($(this).attr("data-type") == "close-all") {
        delAllTabs();
    }

    $('.rightmenu').hide();
})

function delAllTabs() {
    var tabtitle = $(".layui-tab-title li");
    var ids = new Array();
    $.each(tabtitle, function (i) {
        ids[i] = $(this).attr("lay-id");
    })

    active.tabDeleteAll(ids);
}

function FrameWH() {
    var h = $(window).height() -41- 10 - 60 -10-44 -10;
    $("iframe").css("height",h+"px");
}

$(window).resize(function () {
    FrameWH();
})

/**
 * 关闭导航
 */
$('.admin-side-toggle').on('click', function () {
    var sideWidth = $('#admin-side').width();
    if (sideWidth === 200) {
        $('#admin-body').animate({
            left: '0'
        }); //admin-footer
        $('#admin-footer').animate({
            left: '0'
        });
        $('#admin-side').animate({
            width: '0'
        });
    } else {
        $('#admin-body').animate({
            left: '200px'
        });
        $('#admin-footer').animate({
            left: '200px'
        });
        $('#admin-side').animate({
            width: '200px'
        });
    }
});

/**
 * 全屏
 */
$('.admin-side-full').on('click', function () {
    var docElm = document.documentElement;
    //W3C
    if (docElm.requestFullscreen) {
        docElm.requestFullscreen();
    }
    //FireFox
    else if (docElm.mozRequestFullScreen) {
        docElm.mozRequestFullScreen();
    }
    //Chrome等
    else if (docElm.webkitRequestFullScreen) {
        docElm.webkitRequestFullScreen();
    }
    //IE11
    else if (elem.msRequestFullscreen) {
        elem.msRequestFullscreen();
    }
    layer.msg('按ESC退出全屏');
});