package net.exiva.quicktwit;

import danger.app.Application;
import danger.app.Event;
import danger.app.EventType;
import danger.app.IPCMessage;
import danger.app.Registrar;
import danger.app.SettingsDB;
import danger.app.SettingsDBException;

import danger.audio.Meta;

import danger.mime.Base64;

import danger.net.HTTPConnection;
import danger.net.HTTPTransaction;

import danger.ui.AlertWindow;
import danger.ui.DialogWindow;
import danger.ui.MarqueeAlert;
import danger.ui.NotificationManager;
import danger.ui.TextField;
import danger.ui.TextInputAlertWindow;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import danger.util.DEBUG;

import java.net.URLEncoder;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class quickTwit extends Application implements Resources, Commands {
	public boolean firstLaunch = true;
	static public int mMarquee;
	static private int mPrefix;
	static public int mSound;
	static public SettingsDB qtPrefs;
	static private String apiKey = "e61e57a5e0ac975724e55a9f27c65178";
	static private String baseURL;
	static private String password;
	static private String pingfmKey;
	static private String postStatus;
	static private String preset1;
	static private String preset2;
	static private String preset3;
	static private String source = "quickTwit";
	static private String tagName;
	static private String text;
	static private String twitterLogin;
	static private String username;
	AlertWindow error;
	TextInputAlertWindow login,pingfm,presets,quickTwit;
	// AlertWindow quickTwit;
	public quickTwit() {
        Registrar.registerProvider("quickTwit", this, 0);
		quickTwit = Application.getCurrentApp().getResources().getTextInputAlert(ID_QUICKTWIT, this);
		login = Application.getCurrentApp().getResources().getTextInputAlert(ID_TWITTER_LOGIN, this);
		pingfm = Application.getCurrentApp().getResources().getTextInputAlert(ID_PINGFM_LOGIN, this);
		error = Application.getCurrentApp().getResources().getAlert(ID_TWITTER_ERROR, this);
		presets = Application.getCurrentApp().getResources().getTextInputAlert(ID_PRESETS, this);
		((TextField)presets.getDescendantWithID(ID_MESSAGE_1)).setSpellCheckEnabled(true);
		((TextField)presets.getDescendantWithID(ID_MESSAGE_2)).setSpellCheckEnabled(true);
		((TextField)presets.getDescendantWithID(ID_MESSAGE_3)).setSpellCheckEnabled(true);
		((TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT)).setSpellCheckEnabled(true);
    }

	public void launch() {
		IPCMessage ipc = new IPCMessage();
		ipc.addItem("action" , "send");
		registerForLeftShoulderHeld("quickTwit", ipc , 99);
		setPresets(preset1, preset2, preset3);
		firstLaunch=false;
		restoreData();
	}
	
	public void resume() {
		if (firstLaunch)
			return;
	}
	
	public void quit() {
		unregisterForLeftShoulderHeld("quickTwit");
		Registrar.deregisterAllProviders();
	}

	public void restoreData() {
		if (SettingsDB.findDB("qtPrefs") == false) {
			qtPrefs = new SettingsDB("qtPrefs", true);
			qtPrefs.setAutoSyncNotifyee(this);
		} else {
			qtPrefs = new SettingsDB("qtPrefs", true);
			baseURL = qtPrefs.getStringValue("baseURL");
			if (baseURL==null) {
				DEBUG.p("quickTwit: No Base URL specified. Defaulting to twitter.com");
				baseURL="twitter.com";
			}
			username = qtPrefs.getStringValue("username");
			password = qtPrefs.getStringValue("password");
			pingfmKey = qtPrefs.getStringValue("pingfmkey");
			preset1 = qtPrefs.getStringValue("preset1");
			if (preset1==null || "".equals(preset1)) { preset1 = "Wakeup"; }
			preset2 = qtPrefs.getStringValue("preset2");
			if (preset2==null || "".equals(preset2)) { preset2 = "Lunch"; }
			preset3 = qtPrefs.getStringValue("preset3");
			if (preset3==null || "".equals(preset3)) { preset3 = "Goodnight"; }
			mMarquee = 1;
			mSound = 1;
			try {
				mMarquee = qtPrefs.getIntValue("marquee");
				mSound = qtPrefs.getIntValue("sound");
				mPrefix = qtPrefs.getIntValue("prefix");
			} catch (SettingsDBException exception) {}
			auth(username, password);
		}
	}

	public void handleTwitter(String message) {
		String command = message.toUpperCase();
		if ("?SYSMON".equals(command)) {
			IPCMessage sysMon = new IPCMessage();
			sysMon.addItem("what", 1234);
			Registrar.sendMessage("batteryIcon", sysMon, null);
			clearText();
		} else if ("?CONFIGURE".equals(command)) {
			login.show();
			clearText();
		} else if ("?PINGFM".equals(command)) {
			pingfm.show();
			clearText();
		} else if ("?PRESETS".equals(command)) {
			presets.show();
			clearText();
		} else if (command.startsWith("?BASEURL ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				message = message.substring(message.indexOf(" ")+1);
				baseURL = message;
				qtPrefs.setStringValue("baseURL", baseURL);
			}
		} else if (command.startsWith("?SOUND ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				message = message.substring(message.indexOf(" ")+1).toUpperCase();
				if ("ON".equals(message)) {
					mSound=1;
					qtPrefs.setIntValue("sound", 1);
					clearText();
				} else if ("OFF".equals(message)) {
					mSound=0;
					qtPrefs.setIntValue("sound", 0);
					clearText();
				}
			}
		} else if (command.startsWith("?MARQUEE ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				message = message.substring(message.indexOf(" ")+1).toUpperCase();
				if ("ON".equals(message)) {
					mMarquee=1;
					qtPrefs.setIntValue("marquee", 1);
					clearText();
				} else if ("OFF".equals(message)) {
					mMarquee=0;
					qtPrefs.setIntValue("marquee", 0);
					clearText();
				}
			}
		} else if (command.startsWith("?UPDATES ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				message = message.substring(message.indexOf(" ")+1).toUpperCase();
				if ("SMS".equals(message)) {
					twitterSetDelivery("sms");
					clearText();
				} else if ("IM".equals(message)) {
					twitterSetDelivery("im");
					clearText();
				} else if ("OFF".equals(message)) {
					twitterSetDelivery("none");
					clearText();
				}
			}
		} else if (command.startsWith("?PREFIX ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				message = message.substring(message.indexOf(" ")+1).toUpperCase();
				if ("ON".equals(message)) {
					mPrefix=1;
					qtPrefs.setIntValue("prefix", 1);
					clearText();
				} else if ("OFF".equals(message)) {
					mPrefix=0;
					qtPrefs.setIntValue("prefix", 0);
					clearText();
				}
			}
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
		} else if (command.startsWith("!FACEBOOK ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				sendFacebook(message.substring(message.indexOf(" ")+1));
			}
		} else if (command.startsWith("!FBTWITTER ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				sendFbookTwitter(message.substring(message.indexOf(" ")+1));
			}
		} else if (command.startsWith("!MYSPACE ")) {
			if (message.indexOf(" ")+1 <= message.length()) {
				sendMySpace(message.substring(message.indexOf(" ")+1));
			} 
		} else if (command.startsWith("?LOCATION ")) {
				if (message.indexOf(" ")+1 <= message.length()) {
					twitterSetLocation(message.substring(message.indexOf(" ")+1));
				} 
		} else if (!"".equals(command)) {
			sendTwitter(message);
		} else if ("".equals(command)) {
			error.show();
		}
	}

	public void sendTwitter(String message) {
		twitterLogin = Base64.encode((username+":"+password).getBytes());
		message=encodeMsg(message,0);
		HTTPConnection.post("http://"+baseURL+"/statuses/update.json", "Authorization: Basic "+twitterLogin, "source="+source+"&status="+message, (short) 0, 1);
		DEBUG.p("quickTwit: sendTwitter: http://"+baseURL+"/statuses/update.json");
	}

	public void sendFacebook(String message) {
		message=encodeMsg(message,1);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=facebook", (short) 0, 2);
	}

	public void sendFbookTwitter(String message) {
		message=encodeMsg(message,0);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=facebook", (short) 0, 2);
		HTTPConnection.post("http://"+baseURL+"/statuses/update.json", "Authorization: Basic "+twitterLogin, "source="+source+"&status="+message, (short) 0, 1);
	}

	public void sendMySpace(String message) {
		message=encodeMsg(message,0);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=myspace", (short) 0, 2);
	}

	public void auth(String auuser, String aupass) {
		twitterLogin = Base64.encode((auuser+":"+aupass).getBytes());
		HTTPConnection.get("http://"+baseURL+"/account/verify_credentials.json", "Authorization: Basic "+twitterLogin, (short) 0, 3);
		DEBUG.p("quickTwit: auth: http://"+baseURL+"/account/verify_credentials.json");
		// HTTPConnection.get("http://identi.ca/api/statuses/followers.xml", "Authorization: Basic "+twitterLogin, (short) 0, 4);
	}

	public void twitterSetLocation(String location) {
		try { location = URLEncoder.encode(location, "UTF-8"); } catch (UnsupportedEncodingException e) { }
		HTTPConnection.post("http://"+baseURL+"/account/update_location.json", "Authorization: Basic "+twitterLogin, "location="+location, (short) 0, 1);
	}

	public void twitterSetDelivery(String device) {
		HTTPConnection.post("http://"+baseURL+"/account/update_delivery_device.json", "Authorization: Basic "+twitterLogin, "device="+device, (short) 0, 1);
	}

	public String encodeMsg(String message, int prefix) {
		if (prefix==1 && mPrefix==1) {
			message = "is "+message;
		}
		try { message = URLEncoder.encode(message, "UTF-8"); }
		catch (UnsupportedEncodingException e) { }
		return message;
	}

	public void storeTwitterLogin(String user, String pass) {
		qtPrefs.setStringValue("username", user);
		qtPrefs.setStringValue("password", pass);
		username = user;
		password = pass;
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
	}

	public void setPresets(String preset1, String preset2, String preset3) {
		((TextField)presets.getDescendantWithID(ID_MESSAGE_1)).setText(preset1);
		((TextField)presets.getDescendantWithID(ID_MESSAGE_2)).setText(preset2);
		((TextField)presets.getDescendantWithID(ID_MESSAGE_3)).setText(preset3);
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
							if (mSound==1) {
								Meta.play(Meta.BEEP_ACTION_SUCCESS);
							} if (mMarquee==1) {
								MarqueeAlert mNewItemsAlert = new MarqueeAlert("Message posted.", 1);
								NotificationManager.marqueeAlertNotify(mNewItemsAlert);
							}
						}
					}
					eventType = xpp.next();
					if (postStatus.equals("FAIL")) {
						if (eventType == xpp.TEXT) {
							text = xpp.getText();
							if (tagName.equals("message")) {
								if (mSound==1) {
									NotificationManager.playErrorSound();
								} if (mMarquee==1) {
									MarqueeAlert mTwitterError = new MarqueeAlert(text, 1);
									NotificationManager.marqueeAlertNotify(mTwitterError);
								}
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

	// public void getFollowers(String response) { 
	// 	DEBUG.p("------------------Get Followers--------------------");
	// 	StringReader sr = new StringReader(response);
	// 	KXmlParser xpp = new KXmlParser();
	// 	try {
	// 		xpp.setInput(sr);
	// 	} catch (XmlPullParserException ex) {}
	// 
	// 	try {
	// 		int eventType = xpp.getEventType();
	// 		while (eventType != xpp.END_DOCUMENT) {
	// 			if (eventType == xpp.START_TAG) {
	// 				tagName = xpp.getName();
	// 				if (tagName.equals("screen_name")) {
	// 					eventType = xpp.next();
	// 					if (eventType == xpp.TEXT) {
	// 						text = xpp.getText();
	// 						DEBUG.p("The follower's name is: @"+text);
	// 					}
	// 				}
	// 			}
	// 			eventType = xpp.next();
	// 		}
	// 	} catch (XmlPullParserException ex) {}
	// 	catch (IOException ioex) {}
	// 	DEBUG.p("------------------Get Followers--------------------");
	// }
	
	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case EventType.EVENT_MESSAGE:
				switch(e.what) {
					case 0: {
						if ((username == null) || (password == null)) {
							login.show();
						} else {
							quickTwit.show();
						}
						return true;
					}
				}
			case EVENT_SEND_TWITTER: {
				handleTwitter(quickTwit.getTextFieldValue((IPCMessage) e.argument, ID_TWIT_TEXT));
				return true;
			}
			case EVENT_STORE_TWITTER_LOGIN: {
				username = login.getTextFieldValue((IPCMessage) e.argument, ID_TWITTER_USERNAME);
				password = login.getTextFieldValue((IPCMessage) e.argument, ID_TWITTER_PASSWORD);
				auth(login.getTextFieldValue((IPCMessage) e.argument, ID_TWITTER_USERNAME), login.getTextFieldValue((IPCMessage) e.argument, ID_TWITTER_PASSWORD));
				return true;
			}
			case EVENT_STORE_PINGFM_KEY: {
				storepingfmKey(pingfm.getTextFieldValue((IPCMessage) e.argument, ID_PINGFM_KEY));
				return true;
			}
			case EVENT_STORE_PRESETS: {
				storePresetMessages(presets.getTextFieldValue((IPCMessage) e.argument, ID_MESSAGE_1), presets.getTextFieldValue((IPCMessage) e.argument, ID_MESSAGE_2), presets.getTextFieldValue((IPCMessage) e.argument, ID_MESSAGE_3));
				return true;
			}
			case EVENT_CANCEL: {
				login.hide();
				pingfm.hide();
				presets.hide();
				quickTwit.hide();
				return true;
			}
			case Event.EVENT_DATASTORE_RESTORED: {
				restoreData();
				return true;
			}
		}
		return super.receiveEvent(e);
	}
		
	public void networkEvent(Object object) {
		if (object instanceof HTTPTransaction) {
			HTTPTransaction qt = (HTTPTransaction) object;
			int response = qt.getResponse();
			if (qt.getSequenceID() == 1) {
				if(response == 200) {
					clearText();
					if (mSound==1) {
						Meta.play(Meta.BEEP_ACTION_SUCCESS);
					} if (mMarquee==1) {
							MarqueeAlert mNewItemsAlert = new MarqueeAlert("Message posted.", 1);
							NotificationManager.marqueeAlertNotify(mNewItemsAlert);
					}
				} else if (response == 401) {
					if (mMarquee==1) {
						MarqueeAlert mAuthError = new MarqueeAlert("Authorization Error.", 1);
						NotificationManager.marqueeAlertNotify(mAuthError);
					}
					login.show();	
				} else if ((response == 502) || (response == 500) || (response == 503)) {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (mMarquee==1) {
						MarqueeAlert mTwitterError = new MarqueeAlert("Something is technically wrong.", 1);
						NotificationManager.marqueeAlertNotify(mTwitterError);
					}
				}
			}
			if (qt.getSequenceID() == 2) {
				if (response == 200) {
					parsePostResponse(qt.getString());
				} else {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (mMarquee==1) {
						MarqueeAlert mTwitterError = new MarqueeAlert("Something is technically wrong.", 1);
						NotificationManager.marqueeAlertNotify(mTwitterError);
					}
				}
			}
			// if (qt.getSequenceID() == 4) {
			// 	if (response == 200) {
			// 		getFollowers(qt.getString());
			// 	} else {
			// 		if (mSound==1) {
			// 			NotificationManager.playErrorSound();
			// 		} if (mMarquee==1) {
			// 			MarqueeAlert mTwitterError = new MarqueeAlert("Something is technically wrong.", 1);
			// 			NotificationManager.marqueeAlertNotify(mTwitterError);
			// 		}
			// 	}
			// }
			if (qt.getSequenceID() == 3) {
				if (response != 200) {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (mMarquee==1) {
						MarqueeAlert mTwitterError = new MarqueeAlert("Authorization Error.", 1);
						NotificationManager.marqueeAlertNotify(mTwitterError);
						login.show();
					}
				} else if (response == 200) {
					storeTwitterLogin(username, password);
					twitterLogin = Base64.encode((username+":"+password).getBytes());
				}
			}
		}
	}
}//NOM NOM NOM