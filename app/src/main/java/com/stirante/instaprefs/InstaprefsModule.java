package com.stirante.instaprefs;

import android.app.Application;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.widget.Toast;

import com.stirante.instaprefs.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created by stirante
 */
public class InstaprefsModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private XSharedPreferences prefs;
    private boolean debug;
    private String[] ignored_tags;


    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences("com.stirante.instaprefs");
        prefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam packageParam) throws Throwable {
        if (packageParam.packageName.equalsIgnoreCase("com.instagram.android")) {
            findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.makeWorldReadable();
                    prefs.reload();
                    debug = prefs.getBoolean("enable_spam", false);
                    debug("Hooked into Intagram");
                    ignored_tags = prefs.getString("ignored_tags", "").split(";");
                    if (ignored_tags.length == 1 && ignored_tags[0].isEmpty()) ignored_tags = new String[]{};
                    final Context context = (Context) param.args[0];
                    //disable double tap to like
                    if (prefs.getBoolean("disable_double_tap_like", false)) {
                        findAndHookMethod("com.instagram.android.feed.d.a.b", packageParam.classLoader, "c", findClass("com.instagram.feed.a.x", packageParam.classLoader), findClass("com.instagram.feed.ui.h", packageParam.classLoader), int.class, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                return null;
                            }
                        });
                    }
                    //Add download and zoom
                    findAndHookMethod("com.instagram.android.feed.adapter.a.ao", packageParam.classLoader, "b", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            CharSequence[] result = (CharSequence[]) param.getResult();
                            ArrayList<CharSequence> newResult = new ArrayList<>();
                            Collections.addAll(newResult, result);
                            newResult.add("Download");
                            Object media = getObjectField(param.thisObject, "e");
                            int type = getIntField(getObjectField(media, "f"), "f");
                            if (type == 1)
                                newResult.add("Zoom");
                            CharSequence[] arr = new CharSequence[newResult.size()];
                            newResult.toArray(arr);
                            param.setResult(arr);
                        }
                    });
                    //handle download and zoom
                    findAndHookMethod("com.instagram.android.feed.adapter.a.ak", packageParam.classLoader, "onClick", findClass("android.content.DialogInterface", packageParam.classLoader), int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String clicked = ((Object[]) callMethod(getObjectField(param.thisObject, "a"), "b"))[(int) param.args[1]].toString();
                            if (clicked.equalsIgnoreCase("Download") || clicked.equalsIgnoreCase("Zoom")) {
                                ((DialogInterface) param.args[0]).dismiss();
                                Object media = getObjectField(getObjectField(param.thisObject, "a"), "e");
                                if (clicked.equalsIgnoreCase("Download")) {
                                    Object userObject = getObjectField(media, "e");
                                    String user = (String) getObjectField(userObject, "a");
                                    String video = (String) getObjectField(media, "ac");
                                    String image = (String) callMethod(media, "a", context);
                                    String id = (String) callMethod(media, "e");
                                    int type = getIntField(getObjectField(media, "f"), "f");
                                    if (type == 1) {
                                        FileUtils.download(image, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".jpg"), context);
                                    } else if (type == 2) {
                                        FileUtils.download(video, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".mp4"), context);
                                    } else {
                                        debug("Unsupported media type " + getObjectField(media, "f").toString());
                                    }
                                } else if (clicked.equalsIgnoreCase("Zoom")) {
                                    String image = (String) callMethod(media, "a", context);
                                    Intent intent = new Intent();
                                    intent.setComponent(ComponentName.unflattenFromString("com.stirante.instaprefs/.ZoomActivity"));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("url", image);
                                    context.startActivity(intent);
                                }
                                param.setResult(null);
                            }
                        }
                    });
                    //add download and zoom to direct
                    findAndHookMethod("com.instagram.android.directsharev2.b.dn", packageParam.classLoader, "b", findClass("com.instagram.direct.model.l", packageParam.classLoader), new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object directMessage_l = param.args[0];
                            ArrayList<CharSequence> arrayList = new ArrayList<>();
                            if ((boolean) callMethod(directMessage_l, "t")) {
                                arrayList.add(context.getResources().getString(context.getResources().getIdentifier("direct_unsend_message", "string", context.getPackageName())));
                            }
                            if ((boolean) callMethod(directMessage_l, "u")) {
                                arrayList.add(context.getResources().getString(context.getResources().getIdentifier("direct_report_message", "string", context.getPackageName())));
                            }
                            CharSequence a = (CharSequence) callStaticMethod(findClass("com.instagram.direct.model.o", packageParam.classLoader), "a", new Class[]{findClass("com.instagram.direct.model.l", packageParam.classLoader), findClass("android.content.res.Resources", packageParam.classLoader)}, directMessage_l, callMethod(param.thisObject, "getResources"));
                            String type = (String) callMethod(callMethod(directMessage_l, "b"), "name");
                            if (!(type.equalsIgnoreCase("MEDIA") || type.equalsIgnoreCase("MEDIA_SHARE") || TextUtils.isEmpty(a))) {
                                arrayList.add(context.getResources().getString(context.getResources().getIdentifier("direct_copy_message_text", "string", context.getPackageName())));
                            }
                            if (type.equalsIgnoreCase("MEDIA") || type.equalsIgnoreCase("MEDIA_SHARE")) {
                                arrayList.add("Download");
                                arrayList.add("Zoom");
                            }
                            boolean z = !arrayList.isEmpty();
                            if (z) {
                                Object onClick = newInstance(findClass("com.instagram.android.directsharev2.b.cw", packageParam.classLoader), new Class[]{findClass("com.instagram.android.directsharev2.b.dn", packageParam.classLoader), ArrayList.class, findClass("com.instagram.direct.model.l", packageParam.classLoader), String.class}, param.thisObject, arrayList, directMessage_l, a);
                                Object builder = newInstance(findClass("com.instagram.ui.dialog.e", packageParam.classLoader), new Class[]{Context.class}, callMethod(param.thisObject, "getContext"));
                                callMethod(builder, "a", arrayList.toArray(new CharSequence[arrayList.size()]), onClick);
                                callMethod(builder, "a", true);
                                callMethod(builder, "b", true);
                                Dialog dialog = (Dialog) callMethod(builder, "c");
                                dialog.show();
                            }
                            callMethod(param.thisObject, "m");
                            return z;
                        }
                    });
                    //handle download and zoom in direct
                    findAndHookMethod("com.instagram.android.directsharev2.b.cw", packageParam.classLoader, "onClick", findClass("android.content.DialogInterface", packageParam.classLoader), int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String clicked = (String) ((ArrayList) getObjectField(param.thisObject, "a")).get((Integer) param.args[1]);
                            if (clicked.equalsIgnoreCase("Download")) {
                                Object media = getObjectField(getObjectField(param.thisObject, "b"), "B");
                                Object userObject = getObjectField(getObjectField(param.thisObject, "b"), "d");
                                String user = (String) getObjectField(userObject, "a");
                                String video = (String) getObjectField(media, "ac");
                                String image = (String) callMethod(media, "a", context);
                                String id = callMethod(getObjectField(param.thisObject, "b"), "i").toString();
                                int type = getIntField(getObjectField(media, "f"), "f");
                                if (type == 1) {
                                    FileUtils.download(image, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".jpg"), context);
                                } else if (type == 2) {
                                    FileUtils.download(video, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".mp4"), context);
                                } else {
                                    debug("Unsupported media type " + getObjectField(media, "f").toString());
                                }
                            } else if (clicked.equalsIgnoreCase("Zoom")) {
                                Object media = getObjectField(getObjectField(param.thisObject, "b"), "B");
                                String image = (String) callMethod(media, "a", context);
                                int type = getIntField(getObjectField(media, "f"), "f");
                                if (type == 1) {
                                    Intent intent = new Intent();
                                    intent.setComponent(ComponentName.unflattenFromString("com.stirante.instaprefs/.ZoomActivity"));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("url", image);
                                    context.startActivity(intent);
                                } else if (type == 2) {
                                    Toast.makeText(context, "Can't zoom video!", Toast.LENGTH_SHORT).show();
                                } else {
                                    debug("Unsupported media type " + getObjectField(media, "f").toString());
                                }
                            }
                        }
                    });
                    //hiding some things
                    final boolean disable_ads = prefs.getBoolean("disable_ads", false);
                    final boolean disable_tags = prefs.getBoolean("disable_tags", false);
                    if (disable_ads || disable_tags) {
                        findAndHookMethod("com.instagram.feed.a.z", packageParam.classLoader, "a", findClass("com.instagram.feed.a.x", packageParam.classLoader), new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Object media = param.args[0];
                                //hiding ads
                                if (disable_ads) {
                                    if (getObjectField(media, "z") != null) {
                                        param.setResult(null);
                                        return;
                                    }
                                }
                                //hiding posts with specified hashtags
                                if (disable_tags) {
                                    List o = (List) XposedHelpers.callMethod(XposedHelpers.callMethod(media, "J"), "c");
                                    if (o != null && o.size() > 0) {
                                        Object comment = o.get(0);
                                        if (((Enum) XposedHelpers.callMethod(comment, "i")).name().equalsIgnoreCase("Caption")) {
                                            String text = (String) XposedHelpers.callMethod(comment, "f");
                                            for (String tag : ignored_tags) {
                                                if (text.toLowerCase().contains("#" + tag)) {
                                                    param.setResult(null);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                    //hide recommendation
                    if (prefs.getBoolean("disable_suggested_follow", false)) {
                        XposedHelpers.findAndHookMethod("com.instagram.g.q", packageParam.classLoader, "a", XposedHelpers.findClass("com.instagram.g.a.g", packageParam.classLoader), new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log("Hid the recommendation v3");
                                param.setResult(null);
                            }
                        });
                        XposedHelpers.findAndHookMethod("com.instagram.g.q", packageParam.classLoader, "getCount", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log("Hid the recommendation v2");
                                param.setResult(0);
                            }
                        });
                    }
                    if (prefs.getBoolean("disable_comments", false)) {
                        XposedHelpers.findAndHookMethod("com.instagram.feed.ui.text.af", packageParam.classLoader, "a", Resources.class, SpannableStringBuilder.class, boolean.class, XposedHelpers.findClass("com.instagram.feed.a.i", packageParam.classLoader), XposedHelpers.findClass("com.instagram.feed.ui.text.b", packageParam.classLoader), new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!((Enum) XposedHelpers.callMethod(param.args[3], "i")).name().equalsIgnoreCase("Caption")) {// || !((boolean) param.args[2])
                                    param.setResult(((SpannableStringBuilder) param.args[1]).length());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void traceObject(Object obj, String name) {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field f : fields) {
            try {
                f.setAccessible(true);
                debug(name + "." + f.getName() + " = " + f.get(obj));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        debug("-------------");
    }

    private void debug(String string) {
        if (debug)
            XposedBridge.log("[Instaprefs] " + string);
    }
}
