package br.com.google.api;

/**
 * Contains the client IDs and scopes for allowed clients consuming the helloworld API.
 */
public class Constants {
  public static final String WEB_CLIENT_ID = "340121606461-9ln7rfv9mtvepo6iv9csua80iccki8do.apps.googleusercontent.com";
  public static final String ANDROID_CLIENT_ID = "replace this with your Android client ID";
  public static final String IOS_CLIENT_ID = "replace this with your iOS client ID";
  public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

  public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
  public static final String GMAIL_SCOPE_READ = "https://www.googleapis.com/auth/gmail.readonly";
  public static final String GMAIL_SCOPE_WRITE = "https://www.googleapis.com/auth/gmail.modify";
  public static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
  public static final String DRIVE_SCOPE_FILE = "https://www.googleapis.com/auth/drive.file";
  public static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
  public static final String SPREADSHEET_SCOPE = "https://www.googleapis.com/auth/spreadsheets";
}
