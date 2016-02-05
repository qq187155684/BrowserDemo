package com.android.myapidemo.smartisan.browser.util;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jude on 14-12-25.
 */
public class StringUtils {

    private static final String oneKeyList = "^http://ccs\\.qq\\.com/comm-htdocs/milo_mobile/login\\.html|^http://ui\\.ptlogin2.*.com/cgi-bin/login";
    public static boolean inOneKeyLoginList(String url) {
        if (TextUtils.isEmpty(url))
            return false;

        Pattern pattern = Pattern.compile(oneKeyList);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    private static final String mobileList = "<meta((?!<).)*(maximum-scale\\s*=\\s*1|user-scalable\\s*=\\s*0|user-scalable\\s*=\\s*no)+((?!<).)*>";
    public static boolean inMobileList(String data) {
        if (TextUtils.isEmpty(data))
            return false;

        Pattern pattern = Pattern.compile(mobileList);
        Matcher matcher = pattern.matcher(data);
        return matcher.find();
    }

    private static final String resourceWhiteList = "^http://inews\\.gtimg\\.com|mantis|\\.gif$|\\.jpg$|\\.png$|\\.png-app\\.webp$|\\.jpg-app\\.webp$|^http://att\\.newsmth\\.net/nForum/att/|^http://images\\.newsmth\\.net/nForum/";
    public static boolean inResourceWhiteList(String url) {
        if (TextUtils.isEmpty(url))
            return true;

        Pattern pattern = Pattern.compile(resourceWhiteList);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    private static final String whiteList = "^http://news\\.sina\\.cn/|^http://top\\.sina\\.cn/news/|^http://yd\\.sina\\.cn/article/|^http://xw\\.qq\\.com/|^http://m\\.sohu\\.com/n/|^http://news\\.163\\.com/15/|^http://news\\.ifeng\\.com/a/|^http://*\\.baijia\\.baidu\\.com/article/|^http://m\\.huanqiu\\.com/view\\.html\\?id=";
    public static boolean inWhiteList(String url) {
        if (TextUtils.isEmpty(url))
            return true;

        Pattern pattern = Pattern.compile(whiteList);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    private static final String blackList = "^http://cn\\.shopbop\\.com/|^http://www\\.besgold\\.com/$|^http://bbs\\.besgold\\.com/|^http://ddfs.webxgame.com/$|^http://sgs.51job.com/$|^http://www\\.zxxk\\.com/$|^http://www.jianggame.com/$|^http://www.southcn.com/\\?type=pc$|^http://www.southcn.com/$|^http://www.ccb.com/cn/home/index.html$|^http://www\\.cnkjz\\.com/|^http://jiaju\\.sina\\.com\\.cn/$|^http://www\\.jinghua\\.cn/$|^http://blog\\.sina\\.com\\.cn/$|^http://m\\.huanqiu\\.com/$|^http://sports\\.ifeng\\.com/|^http://www\\.crazyenglish\\.com/$|^http://www\\.crazyenglish\\.org/$|^http://www\\.zhuyin\\.com/|^http://www\\.vaikan\\.com/|^http://xw\\.qq\\.com/zt/|^http://www\\.pcauto\\.com\\.cn/|^http://www\\.autohome\\.com\\.cn/|^http://www\\.imobile\\.com\\.cn/|^http://www\\.zhibo8\\.cc/|^http://www\\.bjgjj\\.gov\\.cn/|^http://live\\.hao123\\.com/|^http://www\\.bjld\\.gov\\.cn/|ssid=CMCC-WEB$|^http://.*.pengyou\\.com/|^http://news\\.ifeng\\.com/hotnews/|^http://www.baidu.com/from=844b/s\\?word=%E5%8D%8E%E4%B8%BA%E7%BD%91%E7%9B%98|^http://image\\.baidu\\.com|^http://dbank\\.vmall\\.com|^http://login\\.dbank\\.com";
    public static boolean inBlackList(String url) {
        if (TextUtils.isEmpty(url))
            return true;

        Pattern pattern = Pattern.compile(blackList);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    private static final String slowList = "^http://m\\.baidu\\.com/news|^http://www\\.zaobao\\.com/";
    private static final String fastList = "^http://ent\\.sina\\.com\\.cn/";
    public static int getDelayTime(String url) {
        int postDelay = 300;
        if (TextUtils.isEmpty(url))
            return postDelay;

        Pattern pattern = Pattern.compile(slowList);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find())
            postDelay = 500;
        else {
            pattern = Pattern.compile(fastList);
            matcher = pattern.matcher(url);
            if (matcher.find())
                postDelay = -1;
        }

        return postDelay;
    }

    private static String escapeHtml(String content) {
        while (content.indexOf("&amp;") > 0)
            content = content.replaceAll("&amp;", "&");

        while (content.indexOf("&lt;") > 0)
            content = content.replaceAll("&lt;", "<");

        while (content.indexOf("&gt;") > 0)
            content = content.replaceAll("&gt;", ">");

        while (content.indexOf("&quot;") > 0)
            content = content.replaceAll("&quot;", "\"");

        return content;
    }

    /**
     * remove all script and noscript tag
     * remove meta about refresh html
     * remove css stylesheet
     *
     * @return clean html
     */
    public static String prepareHtml(String html){
        if (TextUtils.isEmpty(html))
            return html;

        //html = escapeHtml(html);
        html = html.replaceAll("<meta http-equiv=\"refresh\"((?!<).)*(</meta>|/>)", "");
        html = html.replaceAll("<noscript>.*</noscript>", "");
        html = html.replaceAll("<script>((?!<).)*</script>", "");
        html = html.replaceAll("<link((?!<).)*href((?!<).)*=((?!<).)*>", "");
        return html;
    }
}
