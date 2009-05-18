package net.exiva.quicktwit;

import danger.app.Application;
import danger.app.Bundle;
import danger.app.Event;
import danger.app.EventType;
import danger.app.IPCIncoming;
import danger.app.IPCMessage;
import danger.app.Registrar;
import danger.app.SettingsDB;
import danger.app.SettingsDBException;
import danger.app.Timer;

import danger.audio.Meta;

import danger.mime.Base64;

import danger.net.HiptopConnection;
import danger.net.HTTPConnection;
import danger.net.HTTPTransaction;

import danger.ui.AlertWindow;
import danger.ui.CheckBox;
import danger.ui.DialogWindow;
import danger.ui.MarqueeAlert;
import danger.ui.NotificationManager;
import danger.ui.TextField;
import danger.ui.TextInputAlertWindow;
import danger.ui.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

//This is just for the logfile debugging
import danger.storage.MountPoint;
import danger.storage.StorageManager;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Date;
import danger.util.format.DateFormat;

// import java.util.Vector;
import danger.util.DEBUG;
import danger.util.MetaStrings;

import java.net.URLEncoder;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class quickTwit extends Application implements Resources, Commands {
	static private Boolean loggedin = false;
	static private int callHome;
	static private int mPrefix;
	static private int mResize;
	static private SettingsDB qtPrefs;
	static private String apiKey = "e61e57a5e0ac975724e55a9f27c65178";
	static private String msg;
	static private String password;
	static private String pingfmKey;
	static private String postStatus;
	static private String preset1;
	static private String preset2;
	static private String preset3;
	static private String source = "quickTwit";
	static private String version = "1.0";
	static private String tagName;
	static private String text;
	static private String trim_password;
	static private String trim_username;
	static private String twitterLogin;
	static private String username;
	static private Timer mTimer;
	
	//DEBUGGING
	public static Date date;
	public static String sDate;
	
	AlertWindow chooser, error, pictureWarning;
	Button twitPic;
	CheckBox facebookPrefix, twitpicResize;
	MarqueeAlert mPingMarquee, mTwitterMarquee, mTrimMarquee;
	TextField bodyField, usernameField, passwordField, pfmField, trimUsernameField;
	TextField trimPasswordField, presetField1, presetField2, presetField3;
	TextInputAlertWindow quickTwit;

	// DialogWindow quickTwit;
	DialogWindow settings;
	
	public quickTwit() {
		Registrar.registerProvider("quickTwit", this, 0);
		Registrar.registerProvider("send-via", this, 1, Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE), "quickTwit", 'T');
		Registrar.registerProvider("qtURL", this, 2);
		quickTwit = Application.getCurrentApp().getResources().getTextInputAlert(ID_QUICKTWIT, this);
		// quickTwit = Application.getCurrentApp().getResources().getDialog(ID_QUICKTWIT, this);
		settings = Application.getCurrentApp().getResources().getDialog(ID_SETTINGS, this);
		error = Application.getCurrentApp().getResources().getAlert(ID_TWITTER_ERROR, this);
		chooser = Application.getCurrentApp().getResources().getAlert(chooserAlert, this);
		pictureWarning = Application.getCurrentApp().getResources().getAlert(warningAlert, this);
		mTwitterMarquee = new MarqueeAlert("null", Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE),1);
		mPingMarquee = new MarqueeAlert("null", Application.getCurrentApp().getResources().getBitmap(ID_PING_MARQUEE),1);
		mTrimMarquee = new MarqueeAlert("null", Application.getCurrentApp().getResources().getBitmap(ID_TRIM_MARQUEE),1);
		bodyField = (TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT);
		twitPic = (Button)quickTwit.getDescendantWithID(ID_PHOTO_BUTTON);
		usernameField = (TextField)settings.getDescendantWithID(ID_TWITTER_USERNAME);
		passwordField = (TextField)settings.getDescendantWithID(ID_TWITTER_PASSWORD);
		pfmField = (TextField)settings.getDescendantWithID(ID_PING_KEY);
		trimUsernameField = (TextField)settings.getDescendantWithID(ID_TRIM_USERNAME);
		trimPasswordField = (TextField)settings.getDescendantWithID(ID_TRIM_PASSWORD);
		presetField1 = (TextField)settings.getDescendantWithID(ID_MESSAGE_1);
		presetField2 = (TextField)settings.getDescendantWithID(ID_MESSAGE_2);
		presetField3 = (TextField)settings.getDescendantWithID(ID_MESSAGE_3);
		facebookPrefix = (CheckBox)settings.getDescendantWithID(ID_FACEBOOK_PREFIX);
		twitpicResize = (CheckBox)settings.getDescendantWithID(ID_TWITPIC_RESIZE);
		mTimer = new Timer(2000, true, this, 1);
		((TextField)settings.getDescendantWithID(ID_MESSAGE_1)).setSpellCheckEnabled(true);
		((TextField)settings.getDescendantWithID(ID_MESSAGE_2)).setSpellCheckEnabled(true);
		((TextField)settings.getDescendantWithID(ID_MESSAGE_3)).setSpellCheckEnabled(true);
		((TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT)).setSpellCheckEnabled(true);
    }

	public void launch() {
		IPCMessage ipc = new IPCMessage();
		ipc.addItem("action" , "send");
		registerForLeftShoulderHeld("quickTwit", ipc , 99);
		restoreData();
		// updateFollowers();
		callHome();
	}

	// public void resume() {
	// 	quickTwit.show();
	// }

	public void restoreData() {
		if (SettingsDB.findDB("qtPrefs") == false) {
			qtPrefs = new SettingsDB("qtPrefs", true);
			qtPrefs.setAutoSyncNotifyee(this);
		} else {
			qtPrefs = new SettingsDB("qtPrefs", true);
			username = qtPrefs.getStringValue("username");
			password = qtPrefs.getStringValue("password");
			trim_username = qtPrefs.getStringValue("trimusername");
			trim_password = qtPrefs.getStringValue("trimpassword");
			pingfmKey = qtPrefs.getStringValue("pingfmkey");
			preset1 = qtPrefs.getStringValue("preset1");
			if (preset1==null || "".equals(preset1)) { preset1 = "Wakeup"; }
			preset2 = qtPrefs.getStringValue("preset2");
			if (preset2==null || "".equals(preset2)) { preset2 = "Lunch"; }
			preset3 = qtPrefs.getStringValue("preset3");
			if (preset3==null || "".equals(preset3)) { preset3 = "Goodnight"; }
			setPresets(preset1, preset2, preset3);
			setTwitterLogin(username, password);
			setTrimLogin(trim_username, trim_password);
			mResize = 1;
			try {
				mPrefix = qtPrefs.getIntValue("prefix");
				mResize = qtPrefs.getIntValue("resize");
			} catch (SettingsDBException exception) {}
			if (!loggedin) {
				auth(username, password);
			}
			facebookPrefix.setValue(mPrefix);
			twitpicResize.setValue(mResize);
		}
	}

	public void callHome() {
		HTTPConnection.get("http://static.tmblr.us/hiptop/hiptopLog2.php?a="+getBundle().getClassName()+"&n="+MetaStrings.get(MetaStrings.ID_PARTNER_NAME)+"&d="+MetaStrings.get(MetaStrings.ID_DEVICE_MODEL)+"&b="+MetaStrings.get(MetaStrings.ID_BRAND_NAME)+"&u="+HiptopConnection.getUserName(), null, (short) 0, 98);
	}

	public void handleTwitter(String message) {
		String command = message.toUpperCase();
		if ("?SYSMON".equals(command)) {
			IPCMessage sysMon = new IPCMessage();
			sysMon.addItem("foo", "bar");
			Registrar.sendMessage("batteryIcon", sysMon, null);
			clearText();
		} else if ("?HELP".equals(command)) {
			 try{
				danger.net.URL.gotoURL("http://static.tmblr.us/hiptop/qTManual.htm");
			}
			catch (danger.net.URLException exc) {}
		} else if ("?CONFIGURE".equals(command) || "?SETTINGS".equals(command)) {
			settings.show();
			clearText();
		} else if ("?1".equals(command)) {
			if ("".equals(preset1)) {
				error.show();
			} else {
				sendTwitter(preset1);
			}
		} else if ("?2".equals(command)) {
			if ("".equals(preset2)) {
				error.show();
			} else {
				sendTwitter(preset2);
			}
		} else if ("?3".equals(command)) {
			if ("".equals(preset3)) {
				error.show();
			} else {
				sendTwitter(preset3);
			}
		} else if (command.startsWith("!FBTWITTER ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				sendFbookTwitter(message.substring(message.indexOf(" ")+1));
			}
		} else if (command.startsWith("!MYSPACE ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				sendMySpace(message.substring(message.indexOf(" ")+1));
			} 
		} else if (command.startsWith("!FACEBOOK ") || command.startsWith("!FB ")) {
				if (message.indexOf(" ")+1 <= message.length()) {
					sendFacebook(message.substring(message.indexOf(" ")+1));
			} 
		} else if (command.startsWith("!BRIGHTKITE ") || command.startsWith("!BKITE ")) {
				if (message.indexOf(" ")+1 <= message.length()) {
					sendBkite(message.substring(message.indexOf(" ")+1));
			}
		} else if (!"".equals(command)) {
			sendTwitter(message);
		} else if ("".equals(command)) {
			error.show();
		}
	}

	public void sendTwitter(String message) {
		twitterLogin = Base64.encode((username+":"+password).getBytes());
		msg=encodeMsg(message,0);
		HTTPConnection.post("https://twitter.com/statuses/update.json", "Authorization: Basic "+twitterLogin+"\nX-Twitter-Client: "+source+"\n X-Twitter-Client-URL: http://static.tmblr.us/hiptop/quickTwit.htm\n X-Twitter-Client-Version: 1.0", "source="+source+"&status="+msg, (short) 0, 1);
		if (message.length() == 140) {
			mTimer.start();
		}
	}

	public void sendTwitABit() {
		HTTPConnection.post("http://api.switchabit.com/twitter.updateStatus", "Authorization: Basic "+twitterLogin, "status="+msg, (short) 0, 5);
	}

	public void sendFacebook(String message) {
		message=encodeMsg(message,1);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=facebook", (short) 0, 2);
	}

	public void sendFbookTwitter(String message) {
		message=encodeMsg(message,0);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=facebook", (short) 0, 2);
		HTTPConnection.post("https://twitter.com/statuses/update.json", "Authorization: Basic "+twitterLogin+"\nX-Twitter-Client: "+source+"\n X-Twitter-Client-URL: http://static.tmblr.us/hiptop/quickTwit.htm\n X-Twitter-Client-Version: 1.0", "source="+source+"&status="+message, (short) 0, 1);
	}

	public void sendMySpace(String message) {
		message=encodeMsg(message,0);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=myspace", (short) 0, 2);
	}

	public void sendBkite(String message) {
		message=encodeMsg(message,0);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=brightkite", (short) 0, 2);
	}

	public void getPingKey(String inKey) {
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.key.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&mobile_key="+inKey, (short) 0, 5);
	}

	public void tweetShrink(String message) {
		HTTPConnection.post("http://tweetshrink.com/shrink", "Authorization: Basic", "text="+message, (short) 0, 6);
	}

	public void trimURL(String url) {
		if ((trim_username == null) || (trim_password == null)) {
			HTTPConnection.post("http://api.tr.im/api/trim_simple", "Authorization: Basic", "url="+url, (short) 0, 7);
		} else {
			HTTPConnection.post("http://api.tr.im/api/trim_simple", "Authorization: Basic", "url="+url+"&username="+trim_username+"&password="+trim_password, (short) 0, 7);
		}
	}

	public void auth(String auuser, String aupass) {
		twitterLogin = Base64.encode((auuser+":"+aupass).getBytes());
		HTTPConnection.get("https://twitter.com/account/verify_credentials.json", "Authorization: Basic "+twitterLogin+"\nX-Twitter-Client: "+source+"\n X-Twitter-Client-URL: http://static.tmblr.us/hiptop/quickTwit.htm\n X-Twitter-Client-Version: 1.0", (short) 0, 3);
	}

	// public void updateFollowers() {
	// 	String auuser = "exiva";
	// 	String aupass = "sap18cypress";
	// 	twitterLogin = Base64.encode((auuser+":"+aupass).getBytes());
	// 	// HTTPConnection.get("http://twitter.com/account/verify_credentials.json", "Authorization: Basic "+twitterLogin, (short) 0, 3);
	// 	HTTPConnection.get("http://twitter.com/statuses/followers/exiva.json", "Authorization: Basic "+twitterLogin, (short) 0, 97);
	// }

	public void twitterSetLocation(String location) {
		try { location = URLEncoder.encode(location, "UTF-8"); } catch (UnsupportedEncodingException e) { }
		HTTPConnection.post("https://twitter.com/account/update_location.json", "Authorization: Basic "+twitterLogin+"\nX-Twitter-Client: "+source+"\n X-Twitter-Client-URL: http://static.tmblr.us/hiptop/quickTwit.htm\n X-Twitter-Client-Version: 1.0", "location="+location, (short) 0, 1);
	}

	public void twitterSetDelivery(String device) {
		HTTPConnection.post("https://twitter.com/account/update_delivery_device.json", "Authorization: Basic "+twitterLogin+"\nX-Twitter-Client: "+source+"\n X-Twitter-Client-URL: http://static.tmblr.us/hiptop/quickTwit.htm\n X-Twitter-Client-Version: 1.0", "device="+device, (short) 0, 1);
	}

	public String encodeMsg(String message, int prefix) {
		if (prefix==1 && mPrefix==1) {
			message = "is "+message;
		}
		try { message = URLEncoder.encode(message, "UTF-8"); }
		catch (UnsupportedEncodingException e) { }
		return message;
	}

	public void storeTwitterLogin(String newUser, String newPass) {
		DEBUG.p("Storing Twitter Login");
		if (newUser != username) { 
			username = newUser;
			qtPrefs.setStringValue("username", newUser);
			DEBUG.p("twitter login saved");
		}
		if (newPass != password) {
			password = newPass;
			qtPrefs.setStringValue("password", newPass);
			DEBUG.p("twitter password saved");
		}
		auth(username, password);
	}

	public void storeTrimLogin(String newTrimLogin, String newTrimPassword) {
		DEBUG.p("Storing Tr.IM Login...");
		if (newTrimLogin != trim_username) {
			trim_username = trimUsernameField.getText();
			qtPrefs.setStringValue("trimusername", trim_username);
			DEBUG.p("tr.im username saved.");
		}
		if (newTrimPassword != trim_password) {
			trim_password = trimPasswordField.getText();
			qtPrefs.setStringValue("trimpassword", trim_password);
			DEBUG.p("tr.im password saved.");
		}
	}
	
	public void storeFBPrefix(int newFBSetting) {
		DEBUG.p("Storing Facebook Prefix");
		if (newFBSetting != mPrefix) {
			mPrefix = newFBSetting;
			qtPrefs.setIntValue("prefix", newFBSetting);
			DEBUG.p("Facebook prefix saved");
		}
	}

	public void storeTPResize(int newTPSetting) {
		DEBUG.p("Storing TwitPic Resize");
		if (newTPSetting != mPrefix) {
			mResize = newTPSetting;
			qtPrefs.setIntValue("resize", newTPSetting);
			DEBUG.p("TwitPic Resize Saved");
		}
	}

	public void storepingfmKey(String key) {
		qtPrefs.setStringValue("pingfmkey", key);
		pingfmKey = key;
	}

	public void storePresetMessages(String msg1, String msg2, String msg3) {
		if (!msg1.equals(preset1)) {
			qtPrefs.setStringValue("preset1", msg1);
			preset1 = msg1;
		}
		if (!msg2.equals(preset2)) {
			qtPrefs.setStringValue("preset2", msg2);
			preset2 = msg2;
		}
		if (!msg3.equals(preset3)) {
			qtPrefs.setStringValue("preset3", msg3);
			preset3 = msg3;
		}
		setPresets(preset1, preset2, preset3);
	}

	public void clearText() {
		((TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT)).setText(null);
		//move focus to the text input box.
		focus();
	}

	public void focus() {
		quickTwit.setFocusedDescendant(bodyField);
	}

	public void setPresets(String preset1, String preset2, String preset3) {
		presetField1.setText(preset1);
		presetField2.setText(preset2);
		presetField3.setText(preset3);
	}

	public void setTwitterLogin(String username, String password) {
		usernameField.setText(username);
		passwordField.setText(password);
	}

	public void setTrimLogin(String trimName, String trimPassword) {
		trimUsernameField.setText(trimName);
		trimPasswordField.setText(trimPassword);
	}

	public void parsePostResponse(String response) { 
		StringReader sr = new StringReader(response);
		KXmlParser xpp = new KXmlParser();
		try {
			xpp.setInput(sr);
		} catch (XmlPullParserException ex) {}

		try {
			int eventType = xpp.getEventType();
			while (eventType != xpp.END_DOCUMENT) {
				if (eventType == xpp.START_TAG) {
					tagName = xpp.getName();
					if (tagName.equals("rsp")) {
						postStatus = xpp.getAttributeValue(0);
						if (postStatus.equals("OK")) {
							clearText();
							Meta.play(Meta.FEEDBACK_NETWORK_G);
							mPingMarquee.setText(getString(ID_PINGD));
							NotificationManager.marqueeAlertNotify(mPingMarquee);
						}
					}
					eventType = xpp.next();
					if (postStatus.equals("FAIL")) {
						if (eventType == xpp.TEXT) {
							text = xpp.getText();
							if (tagName.equals("message")) {
								NotificationManager.playErrorSound();
								mPingMarquee.setText(text);
								NotificationManager.marqueeAlertNotify(mPingMarquee);
							}
						}
					}
				}
				eventType = xpp.next();
			}
		}
		catch (XmlPullParserException ex) {}
		catch (IOException ioex) {}
	}

	public void parseKeyResponse(String response) { 
			StringReader sr = new StringReader(response);
			KXmlParser xpp = new KXmlParser();
			try {
				xpp.setInput(sr);
			} catch (XmlPullParserException ex) {}

			try {
				int eventType = xpp.getEventType();
				while (eventType != xpp.END_DOCUMENT) {
					if (eventType == xpp.START_TAG) {
						tagName = xpp.getName();
						if (tagName.equals("rsp")) {
							postStatus = xpp.getAttributeValue(0);
						}
						eventType = xpp.next();
						if (postStatus.equals("OK")) {
							if (eventType == xpp.TEXT) {
								text = xpp.getText();
								if (tagName.equals("key")) {
									storepingfmKey(text);
								}
							}
						} else if (postStatus.equals("FAIL")) {
							if (eventType == xpp.TEXT) {
								text = xpp.getText();
								if (tagName.equals("message")) {
									settings.show();
									NotificationManager.playErrorSound();
									mPingMarquee.setText(text);
									NotificationManager.marqueeAlertNotify(mPingMarquee);
								}
							}
						}
					}
					eventType = xpp.next();
				}
			}
			catch (XmlPullParserException ex) {}
			catch (IOException ioex) {}
		}

	public void sendTwitPic(String data, int camera) {
		IPCMessage msg = new IPCMessage();
		msg.addItem("message", data);
		msg.addItem("username", username);
		msg.addItem("password", password);
		msg.addItem("source", "quickTwit");
		msg.addItem("camera", camera);
		msg.addItem("resize", mResize);
		Registrar.sendMessage("TwitPicPlugin", msg, null);
		Bundle twitpic = Bundle.findByClassName("net.exiva.twitpicplugin.twitpicplugin");
		Registrar.bringToForeground(twitpic);		
		clearText();
	}

	// public void getFollowers(String response) {
	// 	Vector v = new Vector();
	// 	//add in the command list...
	// 	v.add("?help");
	// 	v.add("?configure");
	// 	v.add("?pingfm");
	// 	v.add("?presets");
	// 	v.add("?baseurl");
	// 	v.add("?sound on|off");
	// 	v.add("?marquee on|off");
	// 	v.add("?updates sms|off");
	// 	v.add("?prefix on|off");
	// 	v.add("?location");
	// 	v.add("?1");
	// 	v.add("?2");
	// 	v.add("?3");
	// 	v.add("!facebook (message)");
	// 	v.add("!fbtwitter (message)");
	// 	v.add("!myspace (message)");
	// 	try {
	// 		JSONArray arr = new JSONArray(response);
	// 		for (int i = 0; i < arr.length(); i++) {
	// 			JSONObject obj = arr.getJSONObject(i);
	// 			DEBUG.p("screen_name: "+obj.get("screen_name"));
	// 			v.add("@"+obj.get("screen_name"));
	// 			v.add("D "+obj.get("screen_name"));
	// 		}
	// 	} catch (JSONException e) {
	// 		DEBUG.p("Exceptioned: "+e);
	// 	}
	// 	DEBUG.p("Temp: "+v);
	// }

	public void parseTweetShrink(String response) {
		try {
			// Parse a JSONObject without an array.
			JSONObject obj = new JSONObject(response);
			int difference = obj.getInt("difference");
			String newText = obj.getString("text");
			mTwitterMarquee.setText("Shrunk tweet by "+difference+" characters");
			NotificationManager.marqueeAlertNotify(mTwitterMarquee);
			((TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT)).setText(newText);
			quickTwit.show();
			obj=null;
			newText=null;
		} catch (JSONException e) {}
	}

	public void handleMessage(IPCMessage ipcmessage, int i) {
		switch(i) {
			case 1:
				trimURL(ipcmessage.findString("body"));
				break;
			case 2:
				if ("".equals(bodyField.getText())) {
					bodyField.setText(ipcmessage.findString("tpurl"));
				} else {
					bodyField.setText(ipcmessage.findString("tpurl")+" "+bodyField.getText()+" ");
				}
				qtShow();
				break;
		}
	}

	public void writeLog(int seq, int res, String msg, String hdrs) {
		String[] storage;
		storage = StorageManager.getRemovablePaths();
		if(storage.length > 0) {
			try {
				FileWriter fstream = new FileWriter(storage[0]+"/quickTwit.log",true);
				BufferedWriter out = new BufferedWriter(fstream);
				date = new Date();
				sDate = DateFormat.withFormat("MM.dd.yyyy hh:mm:ss", date);
				out.write("==============================\n");
				out.write("Time: "+sDate+"\n");
				out.write("Sequence ID: "+seq+"\n");
				out.write("Response Code: "+res+"\n");
				out.write("Response Message: "+msg+"\n");
				out.write("Response Headers: "+hdrs+"\n");
				out.write("==============================\n\n");
				out.close();
			} catch (Exception e) {
				DEBUG.p("File Write exceptioned! "+e);
			}
		}
	}

	public void qtShow() {
		if ((username == null) || (password == null)) {
			settings.show();
		} else {
			if (Bundle.findByClassName("net.exiva.twitpicplugin.twitpicplugin")==null) {
				twitPic.disable();
			} else {
				twitPic.enable();
			}
			quickTwit.show();
		}
	}

	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case Event.EVENT_TIMER: {
				if (e.data==1) {
					Meta.play(Meta.FEEDBACK_NETWORK_3);
					mTwitterMarquee.setText("Twoosh!");
					NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					mTimer.stop();
				}
				return false;
			}
			case EventType.EVENT_MESSAGE:
				switch(e.what) {
					case 0: {
						qtShow();
						// settings.show();
						return true;
					}
					case 1: {
						handleMessage(((IPCIncoming)e.argument).getMessage(), e.what);
						return true;
					}
					case 2: {
						handleMessage(((IPCIncoming)e.argument).getMessage(), e.what);
						return true;
					}
				}
			case EVENT_SEND_TWITTER: {
				handleTwitter(bodyField.getText());
				return true;
			}
			case EVENT_SETTINGS_DONE: {
				storeTwitterLogin(usernameField.getText(), passwordField.getText());
				storeTrimLogin(trimUsernameField.getText(), trimPasswordField.getText());
				storeFBPrefix(facebookPrefix.getValue());
				storeTPResize(twitpicResize.getValue());
				DEBUG.p("PingFM Text: "+pfmField.getText());
				if (!"".equals(pfmField.getText())) {
					DEBUG.p("PingFM key entered!");
					getPingKey(pfmField.getText());
				}
				return true;
			}
			case EVENT_TWITPIC: {
				if ("".equals(bodyField.getText().toString())) {
					pictureWarning.show();
					msg = "";
				} else {
					chooser.show();
					msg = bodyField.getText().toString();
				}
				return true;
			}
			case EVENT_SHRINK: {
				if ("".equals(bodyField.getText().toString())) {
					error.show();
				} else {
					mTwitterMarquee.setText("Shrinking tweet...");
					NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					tweetShrink(bodyField.getText().toString());
				}
				return true;
			}
			case EVENT_CHOOSE_PHOTO: {
				if ("".equals(msg)) {
					sendTwitPic("", 0);
				} else {
					sendTwitPic(msg, 0);
				}
				return true;
			}
			case EVENT_TAKE_PHOTO: {
				if ("".equals(msg)) {
					sendTwitPic("", 1);
				} else {
					sendTwitPic(msg, 1);
				}
				return true;
			}
			//event_warning_back closes the warning dialog and opens twitpic dialog
			case EVENT_WARNING_BACK: {
				pictureWarning.hide();
				focus();
				quickTwit.show();
				return true;
			}
			//event_chooser_back closes the choose dialog and opens twitpic dialog
			case EVENT_CHOOSER_BACK: {
				chooser.hide();
				focus();
				quickTwit.show();
				return true;
			}
			//event_open_alert brings up the chooser from the warning dialog.
			case EVENT_OPEN_ALERT: {
				pictureWarning.hide();
				chooser.show();
				return true;
			}
			case EVENT_OPEN_KEY_PAGE: {
				 try{
					danger.net.URL.gotoURL("http://69.44.44.70/m/key");
					return true;
				}
				catch (danger.net.URLException exc) {}
			}
			case Event.EVENT_AUTO_SYNC_DONE: {
				restoreData();
				return true;
			}
		}
		return super.receiveEvent(e);
	}

	public void networkEvent(Object object) {
		if (object instanceof HTTPTransaction) {
			HTTPTransaction qt = (HTTPTransaction) object;
			writeLog(qt.getSequenceID(), qt.getResponse(), qt.getResponseMessage(), qt.getHTTPHeaders());
			if (qt.getSequenceID() == 1) {
				if(qt.getResponse() == 200) {
					clearText();
						Meta.play(Meta.FEEDBACK_NETWORK_G);
						mTwitterMarquee.setText(getString(ID_TWITTERED));
						NotificationManager.marqueeAlertNotify(mTwitterMarquee);
				} else if (qt.getResponse() == 401) {
					mTwitterMarquee.setText(getString(ID_AUTH_FAIL));
					NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					settings.show();
				} else {
					NotificationManager.playErrorSound();
					mTwitterMarquee.setText(getString(ID_TWITTER_DOWN));
					NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					sendTwitABit();
				}
			}
			if (qt.getSequenceID() == 2) {
				if (qt.getResponse() == 200) {
					parsePostResponse(qt.getString());
				} else {
					NotificationManager.playErrorSound();
					mTwitterMarquee.setText(getString(ID_FAIL));
					NotificationManager.marqueeAlertNotify(mPingMarquee);
				}
			}
			if (qt.getSequenceID() == 3) {
				if (qt.getResponse() != 200) {
					NotificationManager.playErrorSound();
					mTwitterMarquee.setText(getString(ID_AUTH_FAIL));
					NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					settings.show();
				} else if (qt.getResponse() == 200) {
					twitterLogin = Base64.encode((username+":"+password).getBytes());
					loggedin=true;
				}
			}
			if (qt.getSequenceID() == 4) {
				if (qt.getResponse() == 200) {
					clearText();
					Meta.play(Meta.FEEDBACK_NETWORK_G);
					mTwitterMarquee.setText(getString(ID_QUEUED));
					NotificationManager.marqueeAlertNotify(mTwitterMarquee);
				} else {
					NotificationManager.playErrorSound();
					MarqueeAlert mTwitterError = new MarqueeAlert("Something is technically wrong.", 1);
					NotificationManager.marqueeAlertNotify(mTwitterError);
				}
			}
			if (qt.getSequenceID() == 5) {
				if (qt.getResponse() == 200) {
					parseKeyResponse(qt.getString());
				} else {
					NotificationManager.playErrorSound();
					mTwitterMarquee.setText(getString(ID_FAIL));
					NotificationManager.marqueeAlertNotify(mPingMarquee);
				}
			}
			if (qt.getSequenceID() == 6) {
				if (qt.getResponse() == 200) {
					parseTweetShrink(qt.getString());
				} else {
					NotificationManager.playErrorSound();
					mTwitterMarquee.setText(getString(ID_FAIL));
					NotificationManager.marqueeAlertNotify(mPingMarquee);
				}
			}
			if (qt.getSequenceID() == 7) {
				if (qt.getResponse() == 200) {
					if (!"".equals(qt.getResponse())) { 
						String[] trimmd = qt.getString().split("\n");
						if ("".equals(bodyField.getText())) {
							bodyField.setText(trimmd[0]);
						} else {
							bodyField.setText(bodyField.getText()+" "+trimmd[0]);
						}
						mTrimMarquee.setText(getString(ID_TRIMD));
						NotificationManager.marqueeAlertNotify(mTrimMarquee);
					}
				} else {
					NotificationManager.playErrorSound();
					mTrimMarquee.setText(getString(ID_FAIL));
					NotificationManager.marqueeAlertNotify(mTrimMarquee);
				}
			}
			// if (qt.getSequenceID() == 97) {
			// 	DEBUG.p("Here i am!");
			// 	if (qt.getResponse() == 200) {
			// 		getFollowers(qt.getString());
			// 		// sinceString = parseSinceDate(qt.getHTTPHeaders());
			// 		// DEBUG.p("Headers: "+qt.getHTTPHeaders());
			// 		// DEBUG.p("Since String: "+sinceString);
			// 		Meta.play(Meta.FEEDBACK_NETWORK_G);
			// 		mTwitterMarquee.setText("Twitter refreshed. Check for CPU usage.");
			// 		NotificationManager.marqueeAlertNotify(mTwitterMarquee);
			// 	} else {
			// 		if (mSound==1) {
			// 			NotificationManager.playErrorSound();
			// 		} if (iMarquee==1) {
			// 			mTwitterMarquee.setText(getString(ID_FAIL));
			// 			NotificationManager.marqueeAlertNotify(mTwitterMarquee);
			// 		}
			// 	}
			// }
		}
	}
}//NOM NOM NOM