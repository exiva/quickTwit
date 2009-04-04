package net.exiva.quicktwit;

import danger.app.Application;
import danger.app.Bundle;
import danger.app.Event;
import danger.app.EventType;
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
import danger.ui.DialogWindow;
import danger.ui.MarqueeAlert;
import danger.ui.NotificationManager;
import danger.ui.TextField;
import danger.ui.TextInputAlertWindow;
import danger.ui.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import danger.util.DEBUG;
import danger.util.MetaStrings;

import java.net.URLEncoder;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class quickTwit extends Application implements Resources, Commands {
	static private int callHome;
	static private int iMarquee;
	static private int mPrefix;
	static private int mSound;
	static private SettingsDB qtPrefs;
	static private String apiKey = "e61e57a5e0ac975724e55a9f27c65178";
	static private String baseURL;
	static private String msg;
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
	static private Timer mTimer;
	
	AlertWindow error;
	MarqueeAlert mTwitterMarquee, mPingMarquee;
	TextField bodyField;
	TextInputAlertWindow login,pingfm,presets,quickTwit;

	public quickTwit() {
        Registrar.registerProvider("quickTwit", this, 0);
		quickTwit = Application.getCurrentApp().getResources().getTextInputAlert(ID_QUICKTWIT, this);
		login = Application.getCurrentApp().getResources().getTextInputAlert(ID_TWITTER_LOGIN, this);
		pingfm = Application.getCurrentApp().getResources().getTextInputAlert(ID_PINGFM_LOGIN, this);
		error = Application.getCurrentApp().getResources().getAlert(ID_TWITTER_ERROR, this);
		presets = Application.getCurrentApp().getResources().getTextInputAlert(ID_PRESETS, this);
		mTwitterMarquee = new MarqueeAlert("null", Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE),1);
		mPingMarquee = new MarqueeAlert("null", Application.getCurrentApp().getResources().getBitmap(ID_PING_MARQUEE),1);
		bodyField = ((TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT));
		mTimer = new Timer(2000, true, this, 1);
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
		restoreData();
	}

	public void resume() {
		quickTwit.show();
	}

	public void quit() {
		unregisterForLeftShoulderHeld("quickTwit");
		Registrar.deregisterAllProviders();
	}

	public void restoreData() {
		DEBUG.p("quickTwit: restoreData method called!");
		if (SettingsDB.findDB("qtPrefs") == false) {
			qtPrefs = new SettingsDB("qtPrefs", true);
			qtPrefs.setAutoSyncNotifyee(this);
			callHome();
		} else {
			qtPrefs = new SettingsDB("qtPrefs", true);
			baseURL = qtPrefs.getStringValue("baseURL");
			if (baseURL==null) {
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
			iMarquee = 1;
			mSound = 1;
			try {
				iMarquee = qtPrefs.getIntValue("marquee");
				mSound = qtPrefs.getIntValue("sound");
				mPrefix = qtPrefs.getIntValue("prefix");
				callHome = qtPrefs.getIntValue("callHome");
			} catch (SettingsDBException exception) {}
			auth(username, password);
			if (callHome != 1) {
				callHome();
			}
		}
	}

	public void callHome() {
		HTTPConnection.get("http://static.tmblr.us/hiptop/hiptopLog2.php?a="+getBundle().getClassName()+"&n="+MetaStrings.get(MetaStrings.ID_PARTNER_NAME)+"&d="+MetaStrings.get(MetaStrings.ID_DEVICE_MODEL)+"&b="+MetaStrings.get(MetaStrings.ID_BRAND_NAME)+"&u="+HiptopConnection.getUserName(), null, (short) 0, 98);
		qtPrefs.setIntValue("callHome", 1);
	}

	public void handleTwitter(String message) {
		String command = message.toUpperCase();
		if ("?SYSMON".equals(command)) {
			IPCMessage sysMon = new IPCMessage();
			sysMon.addItem("what", 1234);
			Registrar.sendMessage("batteryIcon", sysMon, null);
			clearText();
		} else if ("?HELP".equals(command)) {
			 try{
				danger.net.URL.gotoURL("http://static.tmblr.us/hiptop/qTManual.htm");
			}
			catch (danger.net.URLException exc) {}
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
					iMarquee=1;
					qtPrefs.setIntValue("marquee", 1);
					clearText();
				} else if ("OFF".equals(message)) {
					iMarquee=0;
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
		msg=encodeMsg(message,0);
		HTTPConnection.post("http://"+baseURL+"/statuses/update.json", "Authorization: Basic "+twitterLogin, "source="+source+"&status="+msg, (short) 0, 1);
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
		HTTPConnection.post("http://"+baseURL+"/statuses/update.json", "Authorization: Basic "+twitterLogin, "source="+source+"&status="+message, (short) 0, 1);
	}

	public void sendMySpace(String message) {
		message=encodeMsg(message,0);
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.post.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&user_app_key="+pingfmKey+"&post_method=status&body="+message+"&service=myspace", (short) 0, 2);
	}

	public void getPingKey(String inKey) {
		HTTPConnection.post("http://static.tmblr.us/hiptop/pfm.user.key.php", "Content-Type: application/x-www-form-urlencoded","api_key="+apiKey+"&mobile_key="+inKey, (short) 0, 5);
	}
	
	public void auth(String auuser, String aupass) {
		twitterLogin = Base64.encode((auuser+":"+aupass).getBytes());
		HTTPConnection.get("http://"+baseURL+"/account/verify_credentials.json", "Authorization: Basic "+twitterLogin, (short) 0, 3);
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
		//move focus to the text input box.
		TextField foo = (TextField)quickTwit.getDescendantWithID(ID_TWIT_TEXT);
		quickTwit.setFocusedDescendant(foo);
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
								Meta.play(Meta.FEEDBACK_NETWORK_G);
							} if (iMarquee==1) {
								mPingMarquee.setText(getString(ID_PINGD));
								NotificationManager.marqueeAlertNotify(mPingMarquee);
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
								} if (iMarquee==1) {
									mPingMarquee.setText(text);
									NotificationManager.marqueeAlertNotify(mPingMarquee);
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
									pingfm.show();
									if (mSound==1) {
										NotificationManager.playErrorSound();
									} if (iMarquee==1) {
										mPingMarquee.setText(text);
										NotificationManager.marqueeAlertNotify(mPingMarquee);
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
				getPingKey(pingfm.getTextFieldValue((IPCMessage) e.argument, ID_PINGFM_KEY));
				return true;
			}
			case EVENT_STORE_PRESETS: {
				storePresetMessages(presets.getTextFieldValue((IPCMessage) e.argument, ID_MESSAGE_1), presets.getTextFieldValue((IPCMessage) e.argument, ID_MESSAGE_2), presets.getTextFieldValue((IPCMessage) e.argument, ID_MESSAGE_3));
				return true;
			}
			case EVENT_TWITPIC: {
					String data = quickTwit.getTextFieldValue((IPCMessage) e.argument, ID_TWIT_TEXT).toString();
					IPCMessage msg = new IPCMessage();
					msg.addItem("data", data.getBytes());
					msg.addItem("username", username.getBytes());
					msg.addItem("password", password.getBytes());
					msg.addItem("source", "quickTwit".getBytes());
					Registrar.sendMessage("quickTwitPlugin", msg, null);
					Bundle twitpic = Bundle.findByClassName("net.exiva.twitpicplugin.twitpicplugin");
					Registrar.bringToForeground(twitpic);
					clearText();
					return true;
			}
			case EVENT_OPEN_KEY_PAGE: {
				 try{
					danger.net.URL.gotoURL("http://69.44.44.70/m/key");
					return true;
				}
				catch (danger.net.URLException exc) {}
			}
			case EVENT_CANCEL: {
				login.hide();
				pingfm.hide();
				presets.hide();
				quickTwit.hide();
				return true;
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
			int response = qt.getResponse();
			if (qt.getSequenceID() == 1) {
				if(response == 200) {
					clearText();
					if (mSound==1) {
						Meta.play(Meta.FEEDBACK_NETWORK_G);
					} if (iMarquee==1) {
							mTwitterMarquee.setText(getString(ID_TWITTERED));
							NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					}
				} else if (response == 401) {
					if (iMarquee==1) {
						mTwitterMarquee.setText(getString(ID_AUTH_FAIL));
						NotificationManager.marqueeAlertNotify(mTwitterMarquee);
						
					}
					login.show();	
				} else if ((response == 502) || (response == 500) || (response == 503) || (response == 400) || (response == 403) || (response == 404)) {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (iMarquee==1) {
						mTwitterMarquee.setText(getString(ID_TWITTER_DOWN));
						NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					}
					sendTwitABit();
				}
			}
			if (qt.getSequenceID() == 2) {
				if (response == 200) {
					parsePostResponse(qt.getString());
				} else {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (iMarquee==1) {
						mTwitterMarquee.setText(getString(ID_FAIL));
						NotificationManager.marqueeAlertNotify(mPingMarquee);
					}
				}
			}
			if (qt.getSequenceID() == 3) {
				if (response != 200) {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (iMarquee==1) {
						mTwitterMarquee.setText(getString(ID_AUTH_FAIL));
						NotificationManager.marqueeAlertNotify(mTwitterMarquee);
						login.show();
					}
				} else if (response == 200) {
					storeTwitterLogin(username, password);
					twitterLogin = Base64.encode((username+":"+password).getBytes());
				}
			}
			if (qt.getSequenceID() == 4) {
				if (response == 200) {
					clearText();
					if (mSound==1) {
						Meta.play(Meta.FEEDBACK_NETWORK_G);
					} if (iMarquee==1) {
							mTwitterMarquee.setText(getString(ID_QUEUED));
							NotificationManager.marqueeAlertNotify(mTwitterMarquee);
					}
				} else {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (iMarquee==1) {
						MarqueeAlert mTwitterError = new MarqueeAlert("Something is technically wrong.", 1);
						NotificationManager.marqueeAlertNotify(mTwitterError);
					}
				}
			}
			if (qt.getSequenceID() == 5) {
				if (response == 200) {
					parseKeyResponse(qt.getString());
				} else {
					if (mSound==1) {
						NotificationManager.playErrorSound();
					} if (iMarquee==1) {
						mTwitterMarquee.setText(getString(ID_FAIL));
						NotificationManager.marqueeAlertNotify(mPingMarquee);
					}
				}
			}
		}
	}
}//NOM NOM NOM