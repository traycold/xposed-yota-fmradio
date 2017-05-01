package com.vassoiofreddo.xposed.fmradio;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

	private final static String PKG = "com.yotadevices.yotaphone2.fmradio";

	private final static int RADIO_TEXT_VIEW_ID = View.generateViewId();
	private final static int RADIO_TEXT_LAND_VIEW_ID = View.generateViewId();
	
	private static int mNotchId = -1;
	
	/*
	 * replace the text "mhz" in the main view with the program name from RDS + add a new text view with RDS radio text
	 */
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {



		if (!lpparam.packageName.equals(PKG))
			return;

		// set the program name to null when the frequency has been changed
		findAndHookMethod("com.caf.fmradio.FMRadio", lpparam.classLoader, "updateStationInfoToUI", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TextView mMHzTV = (TextView) getObjectField(param.thisObject, "mMHzTV");
				mMHzTV.setText(null);
				TextView radioTextTV = (TextView) ((Activity)param.thisObject).findViewById(RADIO_TEXT_VIEW_ID);
				if(radioTextTV!=null)
					radioTextTV.setText(null);
				radioTextTV = (TextView) ((Activity)param.thisObject).findViewById(RADIO_TEXT_LAND_VIEW_ID);
				if(radioTextTV!=null)
					radioTextTV.setText(null);
			}
		});
		// change color of field radio text when radio on/off
		findAndHookMethod("com.caf.fmradio.FMRadio", lpparam.classLoader, "enableRadioOnOffUI", "boolean", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TextView mMHzTV = (TextView) getObjectField(param.thisObject, "mMHzTV");
				TextView radioTextTV = (TextView) ((Activity)param.thisObject).findViewById(RADIO_TEXT_VIEW_ID);
				if(radioTextTV!=null)
					radioTextTV.setTextColor(mMHzTV.getTextColors());
				radioTextTV = (TextView) ((Activity)param.thisObject).findViewById(RADIO_TEXT_LAND_VIEW_ID);
				if(radioTextTV!=null)
					radioTextTV.setTextColor(mMHzTV.getTextColors());
			}
		});

		// set the "mhz" field with RDS field "program service" when it is received
		findAndHookMethod("com.caf.fmradio.FMRadio$46", lpparam.classLoader, "run", new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					Object fmRadioObj = getObjectField(param.thisObject, "this$0");
					Object mServiceObj = getObjectField(fmRadioObj, "mService");
					String programService = mServiceObj==null?null:(String) findMethodBestMatch(mServiceObj.getClass(), "getProgramService").invoke(mServiceObj);
					String radioText = mServiceObj==null?null:(String) findMethodBestMatch(mServiceObj.getClass(), "getRadioText").invoke(mServiceObj);
					programService = programService==null?null:programService.trim();
					radioText = radioText==null?null:radioText.trim();
					TextView mMHzTV = (TextView) getObjectField(fmRadioObj, "mMHzTV");
					mMHzTV.setText(programService);
					//XposedBridge.log("radio text ["+(radioText!=null?radioText.length():"-")+"]: "+radioText);
					TextView radioTextTV = (TextView) ((Activity)fmRadioObj).findViewById(RADIO_TEXT_VIEW_ID);
					if(radioTextTV!=null){
						radioTextTV.setText(radioText);
						radioTextTV.setSelected(true);
					}
					radioTextTV = (TextView) ((Activity)fmRadioObj).findViewById(RADIO_TEXT_LAND_VIEW_ID);
					if(radioTextTV!=null){
						radioTextTV.setText(radioText);
						radioTextTV.setSelected(true);
					}

				} catch (Throwable e) {
					XposedBridge.log(e);
				}
				return null;
			}
		});
		// notify when "radio text" is received
		findAndHookMethod("com.caf.fmradio.FMRadio$48", lpparam.classLoader, "onRadioTextChanged", new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					findMethodBestMatch(param.thisObject.getClass(), "onProgramServiceChanged").invoke(param.thisObject);
				} catch (Throwable e) {
					XposedBridge.log(e);
				}
				return null;
			}
		});
		
		// fix the notch
		findAndHookMethod("com.caf.fmradio.HorizontalNumberPicker", lpparam.classLoader, "onDraw", Canvas.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				mNotchId =getObjectField(param.thisObject, "mNotch").hashCode();
			}
		});
		
		// fix the notch
		findAndHookMethod("android.graphics.Canvas", lpparam.classLoader, "drawRect", "float","float","float","float",Paint.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(((Paint) param.args[4]).hashCode()==mNotchId){
					boolean land = (float)param.args[0]>800;
					param.args[0] = (float)param.args[0]-(land?9:0);
					param.args[2] = (float)param.args[2]-(land?9:0);
					param.args[1] = (float)param.args[1]-(land?60:35);
					param.args[3] = (float)param.args[3]-(land?60:35);
				}
			}
		});		
		
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
	    if (!resparam.packageName.equals(PKG))
	        return;

	    resparam.res.hookLayout(PKG, "layout", "station_info", new XC_LayoutInflated() {
	        @Override
	        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
        		try {
        			//XposedBridge.log("setting view for variant: "+liparam.variant);
        			boolean land = liparam!=null && liparam.variant.startsWith("layout-land-finger");

							RelativeLayout relativeLayout = (RelativeLayout)liparam.view.findViewById(liparam.res.getIdentifier("second_layout", "id", PKG));
							TextView mMHzView = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("mhz_tv", "id", PKG));

							TextView radioTextTV = new TextView(mMHzView.getContext());
							radioTextTV.setTextAppearance(android.R.style.TextAppearance_Large);
							radioTextTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP,15);
							radioTextTV.setTextColor(mMHzView.getTextColors());
							radioTextTV.setGravity(Gravity.CENTER);
							radioTextTV.setId(RADIO_TEXT_VIEW_ID);
							radioTextTV.setText(null);
							radioTextTV.setSingleLine();
							radioTextTV.setEllipsize(TruncateAt.MARQUEE);
							radioTextTV.setMarqueeRepeatLimit(-1);
							radioTextTV.setHorizontallyScrolling(true);

      				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mMHzView.getLayoutParams());
							layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
							layoutParams.removeRule(RelativeLayout.BELOW);
							radioTextTV.setLayoutParams(layoutParams);

        			if(land){
        				mMHzView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,30);
        				mMHzView.setGravity(Gravity.BOTTOM | Gravity.START);
        				LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) mMHzView.getLayoutParams();
        				p.setMarginStart(15);
        				LinearLayout linearLayout = (LinearLayout) relativeLayout.getChildAt(0);
        				if(linearLayout.getId()==View.NO_ID)
        					linearLayout.setId(View.generateViewId());
  							layoutParams.addRule(RelativeLayout.BELOW, linearLayout.getId());
        			} else {
  							layoutParams.addRule(RelativeLayout.BELOW, mMHzView.getId());
        			}

        			relativeLayout.addView(radioTextTV);
						} catch (Throwable e) {
							XposedBridge.log(e);
						}
	        }
	    });
	}

}