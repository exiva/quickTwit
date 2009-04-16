package net.exiva.quicktwit;

import danger.app.Application;
import danger.app.AppResources;
import danger.app.Bundle;
import danger.app.Event;
import danger.app.EventType;
import danger.app.IPCIncoming;
import danger.app.IPCMessage;
import danger.app.Registrar;

//4.6+
// import danger.app.GalleryItemIPCPayload;
// import danger.app.GalleryItem;
//Older OS's
import danger.app.PhotoRecord;
import danger.app.PhotoRecordIPCPayload;

import danger.ui.Bitmap;
import danger.ui.ImageCodec;
import danger.ui.NotificationManager;
import danger.ui.MarqueeAlert;
import danger.ui.photopicker.PhotoPicker;

import danger.audio.Meta;

import danger.net.HTTPConnection;
import danger.net.HTTPTransaction;
import danger.net.HiptopConnection;

import danger.util.MetaStrings;

import java.io.StringReader;
import java.io.IOException;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class twitpic extends Application implements Resources, Commands {
	public static boolean isJPEG;
	public static byte[] photoData;
	public static int camera, photoSize, width, height;
	public static String password, url, username, className, source, photoname, body;
	MarqueeAlert mMarquee;

	public twitpic() {
		className = getBundle().getClassName();
		mMarquee = new MarqueeAlert("null",Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE),1);
		//register an IPC Service
		Registrar.registerProvider("TwitPicPlugin", this, 99);
	}

	// public void launch() {
	// 	HTTPConnection.get("http://static.tmblr.us/hiptop/hiptopLog2.php?a="+className+"&n="+MetaStrings.get(MetaStrings.ID_PARTNER_NAME)+"&d="+MetaStrings.get(MetaStrings.ID_DEVICE_MODEL)+"&b="+MetaStrings.get(MetaStrings.ID_BRAND_NAME)+"&u="+HiptopConnection.getUserName(), null, (short) 0, 99);
	// }
	// 
	// public void resume() {
	// 	// showPhotoPicker();
	// 	IPCMessage msg = new IPCMessage();
	// 	msg.addItem("action", "send");
	// 	Registrar.sendMessage("quickTwit", msg, null);
	// }
	// 
	// public void suspend() {}

	public static void showPhotoPicker() {
		//4.6+
		// PhotoPicker p = PhotoPicker.createPicker(true, false);
		//legacy
		PhotoPicker p = PhotoPicker.createPicker();
		p.setMaxSelectionCount(1);
		p.setTitle("Choose Existing Photo");
		p.setIcon(Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE));
		if (camera==1) { 
			p.setStartInCaptureView(true);
		}
		// p.setEvent(this, EVENT_HANDLE_PHOTOS, 0, 0);
		p.show();
	}

	//4.6+
	// public void handleSelectedPhoto(GalleryItemIPCPayload photos) {
	//legacy devices
	public void handleSelectedPhoto(PhotoRecordIPCPayload photos) {
		for (int i = 0; i < photos.getRecordCount(); i++) {
			//clear the old imageview and photodata.
			//4.6+
			// GalleryItem record = photos.getGalleryItemAt(i);
			//legacy
			PhotoRecord record = photos.getRecordAt(i);
			photoname = record.getName();
			if (record.getWidth() > 640 && record.getHeight() > 480 || record.getWidth() > 480 && record.getHeight() > 640) {
				int width=record.getWidth()/2;
				int height=record.getHeight()/2;
				//4.6+
				// Bitmap tmp1 = record.getBitmap(width, height);
				//legacy
				Bitmap tmp1 = record.getDecodedBitmap();
				Bitmap tmp2 = tmp1.scaleTo(width, height);
				byte[] tmp = new byte[width * height + 1000];
				//4.6+
				// int len = ImageCodec.encodeJPEG(tmp1, tmp, 65);
				//legacy
				int len = ImageCodec.encodeJPEG(tmp2, tmp, 65);
				photoData = new byte[len];
				System.arraycopy(tmp, 0, photoData, 0, len);
				photoSize = photoData.length;
			} else {
				//4.6+
				// photoData = record.getData();
				// photoSize = record.getDataSize();
				//legacy
				photoData = record.getRawBitmapData();
				photoSize = record.getRawBitmapDataSize();
			}
			isJPEG = ImageCodec.isJPEG(photoData);
			if (isJPEG) {
				postEntry(body, photoData, photoname, photoSize);
			}
		}
	}

	public static void postEntry(String body, byte[] oJPEG, String filename, int size) {
		if (body.equals("")) {
			url = "http://twitpic.com/api/upload";
		} else {
			url = "http://twitpic.com/api/uploadAndPost";
		}

		byte[] start = new String("--AaB03x\r\n" +
		   					"Content-Disposition: form-data; name=\"username\"\r\n" +
							"\r\n" +
							username+"\r\n" +
							"--AaB03x\r\n" +
							"Content-Disposition: form-data; name=\"password\"\r\n" +
							"\r\n" +
							password+"\r\n" +
							"--AaB03x\r\n" +
							"Content-Disposition: form-data; name=\"message\"\r\n" +
							"\r\n" +
							body+"\r\n" +
							"--AaB03x\r\n" +
							"Content-Disposition: form-data; name=\"source\"\r\n" +
							"\r\n" +
							source+"\r\n" +
							"--AaB03x\r\n" +
							"content-disposition: form-data; name=\"media\"; filename=\""+filename+"\"\r\n" +
							"Content-Type: image/jpeg\r\n" +
							"\r\n").getBytes();
		byte[] end = new String("\r\n" +
								"--AaB03x--").getBytes();

		byte[] body2 = new byte[start.length + oJPEG.length + end.length];
		System.arraycopy(start, 0, body2, 0, start.length);
		System.arraycopy(oJPEG, 0, body2, start.length, oJPEG.length);
		System.arraycopy(end, 0, body2, start.length + oJPEG.length, end.length);

		String headers = "Content-type: multipart/form-data, boundary=AaB03x\r\n" +
						 "Content-length: " + body2.length;

		HTTPConnection.post(url, headers, body2, (short) 0, 1);
		Registrar.showChooser(Registrar.getDefaultAppBundle());
	}

	public void parsePostResponse(String response) {
		StringReader sr = new StringReader(response);
		String postStatus, postMessage, tagName, text;
		KXmlParser xpp = new KXmlParser();
		try {
			xpp.setInput(sr);
		}
		catch (XmlPullParserException ex) { }

		try {
			int eventType = xpp.getEventType();
 			while (eventType != xpp.END_DOCUMENT) {
				if (eventType == xpp.START_TAG) {
					tagName = xpp.getName();
					if (tagName.equals("rsp")) {
						postStatus = xpp.getAttributeValue(0);
						if (postStatus.equals("ok")) {
								mMarquee.setText("Picture successfully posted.");
								NotificationManager.marqueeAlertNotify(mMarquee);
								Meta.play(Meta.BEEP_ACTION_SUCCESS);
						} else if (postStatus.equals("fail")) {
							mMarquee.setText("There was an error, try again.");
							NotificationManager.marqueeAlertNotify(mMarquee);
							NotificationManager.playErrorSound();
						}
					}
				}
				eventType = xpp.next();
				if (eventType == 2) {
					text = xpp.getText();
					tagName = xpp.getName();
					if (tagName.equals("err")) {
						mMarquee.setText(xpp.getAttributeValue(1));
						NotificationManager.marqueeAlertNotify(mMarquee);
						NotificationManager.playErrorSound();
					}
				}
			}
		}
		catch (XmlPullParserException ex) { }
		catch (IOException ioex) { }
	}

	public void handleMessage(IPCMessage ipcmessage, int i) {
			switch(i) {
				case 99:
					camera = ipcmessage.findInt("camera");
					body = ipcmessage.findString("message");
					username = ipcmessage.findString("username");
					password = ipcmessage.findString("password");
					source = ipcmessage.findString("source");
					break;
				}
			}

		public void networkEvent(Object object) {
			if (object instanceof HTTPTransaction) {
				HTTPTransaction t = (HTTPTransaction) object;
				if((t.getSequenceID() == 1)) {
					if (t.getResponse() == 200) {
						parsePostResponse(t.getString());
					}
				}
			}
		}

	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case EVENT_HANDLE_PHOTOS: {
				//4.6+
				// handleSelectedPhoto((GalleryItemIPCPayload) e.argument);
				//legacy
				handleSelectedPhoto((PhotoRecordIPCPayload) e.argument);
				return true;
			}
			case EventType.EVENT_MESSAGE:
				switch(e.what) {
					case 99:
						handleMessage(((IPCIncoming)e.argument).getMessage(), e.what);
					return true;
				}
				break;
		}
		return (super.receiveEvent(e));
	}
}//NOM NOM NOM