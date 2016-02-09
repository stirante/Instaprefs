package com.stirante.instaprefs;

import android.app.Application;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.stirante.instaprefs.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by stirante
 */
public class XposedMain implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam packageParam) throws Throwable {
        if (packageParam.packageName.equalsIgnoreCase("com.instagram.android")) {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Context context = (Context) param.args[0];
                    //disable double tap to like
                    XposedHelpers.findAndHookMethod("com.instagram.android.feed.d.a.b", packageParam.classLoader, "c", XposedHelpers.findClass("com.instagram.feed.a.x", packageParam.classLoader), XposedHelpers.findClass("com.instagram.feed.ui.h", packageParam.classLoader), int.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    });
                    //Add download and zoom
                    XposedHelpers.findAndHookMethod("com.instagram.android.feed.adapter.a.am", packageParam.classLoader, "b", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            CharSequence[] result = (CharSequence[]) param.getResult();
                            ArrayList<CharSequence> newResult = new ArrayList<>();
                            Collections.addAll(newResult, result);
                            newResult.add("Download");
                            Object media = XposedHelpers.getObjectField(param.thisObject, "e");
                            int type = XposedHelpers.getIntField(XposedHelpers.getObjectField(media, "f"), "e");
                            if (type == 1)
                                newResult.add("Zoom");
                            CharSequence[] arr = new CharSequence[newResult.size()];
                            newResult.toArray(arr);
                            param.setResult(arr);
                        }
                    });
                    //handle download and zoom
                    XposedHelpers.findAndHookMethod("com.instagram.android.feed.adapter.a.ai", packageParam.classLoader, "onClick", XposedHelpers.findClass("android.content.DialogInterface", packageParam.classLoader), int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String clicked = ((Object[]) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "a"), "b"))[(int) param.args[1]].toString();
                            if (clicked.equalsIgnoreCase("Download") || clicked.equalsIgnoreCase("Zoom")) {
                                ((DialogInterface) param.args[0]).dismiss();
                                Object media = XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "a"), "e");
                                if (clicked.equalsIgnoreCase("Download")) {
                                    Object userObject = XposedHelpers.getObjectField(media, "e");
                                    String user = (String) XposedHelpers.getObjectField(userObject, "a");
                                    String video = (String) XposedHelpers.getObjectField(media, "ac");
                                    String image = (String) XposedHelpers.callMethod(media, "a", context);
                                    String id = (String) XposedHelpers.callMethod(media, "e");
                                    int type = XposedHelpers.getIntField(XposedHelpers.getObjectField(media, "f"), "e");
//                                    int likes = XposedHelpers.getIntField(media, "k");
                                    if (type == 1) {
                                        FileUtils.download(image, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".jpg"), context);
                                    } else if (type == 2) {
                                        FileUtils.download(video, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".mp4"), context);
                                    } else {
                                        XposedBridge.log("Unsupported media type " + XposedHelpers.getObjectField(media, "f").toString());
                                    }
                                } else if (clicked.equalsIgnoreCase("Zoom")) {
                                    String image = (String) XposedHelpers.callMethod(media, "a", context);
                                    Intent intent = new Intent();
                                    intent.setComponent(ComponentName.unflattenFromString("com.stirante.instaprefs/.MainActivity"));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("url", image);
                                    context.startActivity(intent);
                                }
                                param.setResult(null);
                            }
                        }
                    });
                    //add download and zoom to direct
                    XposedHelpers.findAndHookMethod("com.instagram.android.directsharev2.b.dn", packageParam.classLoader, "b", XposedHelpers.findClass("com.instagram.direct.model.l", packageParam.classLoader), new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object directMessage_l = param.args[0];
                            ArrayList<CharSequence> arrayList = new ArrayList<>();
                            if ((boolean) XposedHelpers.callMethod(directMessage_l, "t")) {
                                arrayList.add(context.getResources().getString(context.getResources().getIdentifier("direct_unsend_message", "string", context.getPackageName())));
                            }
                            if ((boolean) XposedHelpers.callMethod(directMessage_l, "u")) {
                                arrayList.add(context.getResources().getString(context.getResources().getIdentifier("direct_report_message", "string", context.getPackageName())));
                            }
                            CharSequence a = (CharSequence) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.instagram.direct.model.o", packageParam.classLoader), "a", new Class[]{XposedHelpers.findClass("com.instagram.direct.model.l", packageParam.classLoader), XposedHelpers.findClass("android.content.res.Resources", packageParam.classLoader)}, directMessage_l, XposedHelpers.callMethod(param.thisObject, "getResources"));
                            String type = (String) XposedHelpers.callMethod(XposedHelpers.callMethod(directMessage_l, "b"), "name");
                            if (!(type.equalsIgnoreCase("MEDIA") || type.equalsIgnoreCase("MEDIA_SHARE") || TextUtils.isEmpty(a))) {
                                arrayList.add(context.getResources().getString(context.getResources().getIdentifier("direct_copy_message_text", "string", context.getPackageName())));
                            }
                            if (type.equalsIgnoreCase("MEDIA") || type.equalsIgnoreCase("MEDIA_SHARE")) {
                                arrayList.add("Download");
                                arrayList.add("Zoom");
                            }
                            boolean z = !arrayList.isEmpty();
                            if (z) {
                                Object onClick = XposedHelpers.newInstance(XposedHelpers.findClass("com.instagram.android.directsharev2.b.cw", packageParam.classLoader), new Class[]{XposedHelpers.findClass("com.instagram.android.directsharev2.b.dn", packageParam.classLoader), ArrayList.class, XposedHelpers.findClass("com.instagram.direct.model.l", packageParam.classLoader), String.class}, param.thisObject, arrayList, directMessage_l, a);
                                Object builder = XposedHelpers.newInstance(XposedHelpers.findClass("com.instagram.ui.dialog.f", packageParam.classLoader), new Class[]{Context.class}, XposedHelpers.callMethod(param.thisObject, "getContext"));
                                XposedHelpers.callMethod(builder, "a", arrayList.toArray(new CharSequence[arrayList.size()]), onClick);
                                XposedHelpers.callMethod(builder, "a", true);
                                XposedHelpers.callMethod(builder, "b", true);
                                Dialog dialog = (Dialog) XposedHelpers.callMethod(builder, "c");
                                dialog.show();
                            }
                            XposedHelpers.callMethod(param.thisObject, "m");
                            return z;
                        }
                    });
                    //handle download and zoom in direct
                    XposedHelpers.findAndHookMethod("com.instagram.android.directsharev2.b.cw", packageParam.classLoader, "onClick", XposedHelpers.findClass("android.content.DialogInterface", packageParam.classLoader), int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String clicked = (String) ((ArrayList) XposedHelpers.getObjectField(param.thisObject, "a")).get((Integer) param.args[1]);
                            if (clicked.equalsIgnoreCase("Download")) {
                                Object media = XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "b"), "B");
                                Object userObject = XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "b"), "d");
                                String user = (String) XposedHelpers.getObjectField(userObject, "a");
                                String video = (String) XposedHelpers.getObjectField(media, "ac");
                                String image = (String) XposedHelpers.callMethod(media, "a", context);
                                String id = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "b"), "d").toString();
                                int type = XposedHelpers.getIntField(XposedHelpers.getObjectField(media, "f"), "e");
                                if (type == 1) {
                                    FileUtils.download(image, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".jpg"), context);
                                } else if (type == 2) {
                                    FileUtils.download(video, new File(FileUtils.INSTAPREFS_DIR, user + "/" + id + ".mp4"), context);
                                } else {
                                    XposedBridge.log("Unsupported media type " + XposedHelpers.getObjectField(media, "f").toString());
                                }
                            }
                            else if (clicked.equalsIgnoreCase("Zoom")) {
                                Object media = XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "b"), "B");
                                String image = (String) XposedHelpers.callMethod(media, "a", context);
                                int type = XposedHelpers.getIntField(XposedHelpers.getObjectField(media, "f"), "e");
                                if (type == 1) {
                                    Intent intent = new Intent();
                                    intent.setComponent(ComponentName.unflattenFromString("com.stirante.instaprefs/.MainActivity"));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("url", image);
                                    context.startActivity(intent);
                                } else if (type == 2) {
                                    Toast.makeText(context, "Can't zoom video!", Toast.LENGTH_SHORT).show();
                                } else {
                                    XposedBridge.log("Unsupported media type " + XposedHelpers.getObjectField(media, "f").toString());
                                }
                            }
                        }
                    });
                    //hide ads
                    XposedHelpers.findAndHookMethod("com.instagram.feed.a.z", packageParam.classLoader, "a", XposedHelpers.findClass("com.instagram.feed.a.x", packageParam.classLoader), new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object media = param.args[0];
                            if (XposedHelpers.getObjectField(media, "Q") != null) {
                                param.setResult(null);
                            }
                        }
                    });
                    //hide recommendation
                    XposedHelpers.findAndHookMethod("com.instagram.g.o", packageParam.classLoader, "a", XposedHelpers.findClass("com.instagram.common.analytics.f", packageParam.classLoader), View.class, XposedHelpers.findClass("com.instagram.g.a.g", packageParam.classLoader), XposedHelpers.findClass("com.instagram.g.p", packageParam.classLoader), new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedHelpers.callMethod(param.args[3], "c", param.args[2]);
                            XposedBridge.log("Hid the recommendation");
                        }
                    });
                }
            });
        }
    }
}
