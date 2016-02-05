
package com.android.myapidemo.smartisan.browser.util;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONObject;

import com.android.myapidemo.smartisan.reflect.ReflectHelper;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

public class CommonUtil {
    public static int dip2px(Context context, double dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static String readTextFromStream(InputStream input, boolean autoClose)
            throws IOException {
        int len;
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = input.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        if (autoClose && input != null) {
            input.close();
        }
        return baos.toString();
    }

    public static String getRootDomain(String url) {
        String domain = getDomain(url);
        ArrayList<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(domain, ".");
        while (tokenizer.hasMoreTokens()) {
            String object = (String) tokenizer.nextElement();
            tokens.add(object);
        }
        int tokenSize = tokens.size();
        int tag = -1;
        for (int i = tokenSize - 1; i > 0; i--) {
            if (Patterns.TOP_LEVEL_DOMAIN.matcher(tokens.get(i)).matches()) {
                tag = i;
                break;
            }
        }
        if (tag != -1) {
            domain = tokens.get(tag - 1) + "." + tokens.get(tag);
            if (domain.equals("com.cn")) {
                domain = tokens.get(tag - 2) + "." + domain;
            }
        }
        return domain;
    }

    public static String getDomain(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        int index1 = url.indexOf("/");
        if (index1 != -1) {
            url = url.substring(0, index1);
        }
        if (url.contains("www.")) {
            url = url.substring(4);
        }
        return url;
    }

    public static boolean saveObject(Context context, Object object, String fileName) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    public static boolean saveString(Context context, String content, String fileName) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    public static String readString(Context context, String fileName) {
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString();
        } catch (FileNotFoundException e) {
            // ignore it
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
    public static <T> T readObject(Context context, Class<T> clazz, String fileName) {
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(fileName);
            ObjectInputStream osi = new ObjectInputStream(fis);
            Object readObject = osi.readObject();
            if (readObject.getClass() == clazz) {
                return (T) readObject;
            }
        } catch (FileNotFoundException e) {
            // ignore it
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static void crashTrace() {
        try {
            int i = 1 / 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String downloadContent(Context context, String url) {
        AndroidHttpClient client = null;
        HttpGet request = null;
        try {
            client = AndroidHttpClient.newInstance(null);
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10 * 1000);
            // HttpHost httpHost = Proxy.getPreferredHttpHost(mContext, url);
            Object[] params = {
                    context, url
            };
            Class<?>[] type = new Class[] {
                    android.content.Context.class, String.class
            };
            HttpHost httpHost = (HttpHost) ReflectHelper.invokeMethod(
                    "android.net.Proxy", "getPreferredHttpHost",
                    type, params);
            if (httpHost != null) {
                ConnRouteParams.setDefaultProxy(client.getParams(), httpHost);
            }
            request = new HttpGet(url);
            request.setHeader("Accept-Encoding", "gzip, deflate");
            // Follow redirects
            HttpClientParams.setRedirecting(client.getParams(), true);
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                InputStream instream = entity.getContent();
                Header header = entity.getContentEncoding();
                if (header != null) {
                    String value = header.getValue();
                    if (TextUtils.isEmpty(value) == false && value.contains("gzip")) {
                        instream = new GZIPInputStream(instream);
                    }
                }
                return readTextFromStream(instream, false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (request != null) {
                request.abort();
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    public static boolean isNetworkAvailable(Context context) {
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isAvailable();
        }
        return false;
    }
    public static boolean isJson(String content){
        try {
            new JSONObject(content);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Animator createAlphaAnimation(final ArrayList<View> deletes, float start, float end) {
        ValueAnimator anim = ValueAnimator.ofObject(new TypeEvaluator<Number>() {
            @Override
            public Number evaluate(float fraction, Number startValue, Number endValue) {
                float startFloat = startValue.floatValue();
                float retVal = startFloat + fraction * (endValue.floatValue() - startFloat);
                for (int i = 0; i < deletes.size(); i++) {
                    View delete = deletes.get(i);
                    delete.setAlpha(retVal);
                }
                return retVal;
            }
        }, start, end);
        return anim;
    }
    public static Animator createAlphaAnimation(final View view, float start, float end) {
        ValueAnimator anim = ValueAnimator.ofObject(new TypeEvaluator<Number>() {
            @Override
            public Number evaluate(float fraction, Number startValue, Number endValue) {
                float startFloat = startValue.floatValue();
                float retVal = startFloat + fraction * (endValue.floatValue() - startFloat);
                view.setAlpha(retVal);
                return retVal;
            }
        }, start, end);
        return anim;
    }

    public static Animator createZoomAnimation(final ArrayList<View> icons, float start, float end) {
        ValueAnimator anim = ValueAnimator.ofObject(new TypeEvaluator<Number>() {
            @Override
            public Number evaluate(float fraction, Number startValue, Number endValue) {
                float startFloat = startValue.floatValue();
                float retVal = startFloat + fraction * (endValue.floatValue() - startFloat);
                int size = icons.size();
                for (int i = 0; i < size; i++) {
                    View icon = icons.get(i);
                    icon.setScaleX(retVal);
                    icon.setScaleY(retVal);
                }
                return retVal;
            }
        }, start, end);
        return anim;
    }
    public static Animator createZoomAnimation(final View icon, float start, float end) {
        ValueAnimator anim = ValueAnimator.ofObject(new TypeEvaluator<Number>() {
            @Override
            public Number evaluate(float fraction, Number startValue, Number endValue) {
                float startFloat = startValue.floatValue();
                float retVal = startFloat + fraction * (endValue.floatValue() - startFloat);
                icon.setScaleX(retVal);
                icon.setScaleY(retVal);
                return retVal;
            }
        }, start, end);
        return anim;
    }
}
