function formChange(form) {
    for (var i = 0; i < form.elements.length; i++) {
        var element = form.elements[i];
        var type = element.type;
        if (type == "checkbox" || type == "radio") {
            if (element.checked != element.defaultChecked) {
                return true;
            }
        }
        else if (type == "hidden" || type == "password" || type == "text" || type == "textarea") {
            if (element.value != element.defaultValue) {
                return true;
            }
        }
        else if (type == "select-one" || type == "select-multiple") {
            for (var j = 0; j < element.options.length; j++) {
                if (element.options[j].selected != element.options[j].defaultSelected) {
                    return true;
                }
            }
        }
    }
    return false;
}

/**
 * js获取url参数
 * @param {*} key 参数名
 */
function getUrlParam(key) {
    let reg = new RegExp("(^|&)" + key + "=([^&]*)(&|$)"); //构造一个含有目标参数的正则表达式对象
    let r = window.location.search.substr(1).match(reg);  //匹配目标参数
    if (r != null) return unescape(r[2]); return null; //返回参数值
}
/**
 * 修改url参数
 * @param {*} key 参数名
 * @param {*} value 参数值
 */
function replaceParamVal(key, value) {
    let oUrl = window.location.href.toString();
    let re = eval('/(' + key + '=)([^&]*)/gi');
    let nUrl = oUrl.replace(re, key + '=' + value);
    return nUrl
}
/**
 * 修改页面url指定参数,并跳转(单个参数)
 * @param {*} key 参数名
 * @param {*} value 参数值
 */
function setParamVal(key, value) {
    let url = window.location.href
    if (getUrlParam(key) != null) {
        window.location.href = replaceParamVal(key, value)
    } else {
        if (url.indexOf("?") == -1) {
            window.location.href = url + '?' + key + '=' + value
        } else {
            window.location.href = url + '&' + key + '=' + value
        }
    }
}
/**
 * 修改页面url指定参数,并跳转(多个参数)
 * @param {*} arr 数组对象
 */
function setMoreParamVal(arr) {
    let url = window.location.href
    arr.map((item) => {
        if (getUrlParam(item.key) != null) {
            let re = eval('/(' + item.key + '=)([^&]*)/gi');
            url = url.replace(re, item.key + '=' + item.value);
        } else {
            if (url.indexOf("?") == -1) {
                url = url + '?' + item.key + '=' + item.value
            } else {
                url = url + '&' + item.key + '=' + item.value
            }
        }
    })
    return url
}
/**
 * 删除指定url上的参数
 * @param {*} url url路径
 * @param {*} name 删除的参数名
 */
function urlDelP(url, name) {
    var urlArr = url.split('?');
    if (urlArr.length > 1 && urlArr[1].indexOf(name) > -1) {
        var query = urlArr[1];
        var obj = {}
        var arr = query.split("&");
        for (var i = 0; i < arr.length; i++) {
            arr[i] = arr[i].split("=");
            obj[arr[i][0]] = arr[i][1];
        }
        delete obj[name];
        var urlte = urlArr[0] + '?' + JSON.stringify(obj).replace(/[\"\{\}]/g, "").replace(/\:/g, "=").replace(/\,/g, "&");
        return urlte;
    } else {
        return url;
    }
}