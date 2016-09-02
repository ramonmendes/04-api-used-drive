package br.com.google.api;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.appengine.api.users.User;

/**
 * Defines v1 of a helloworld API, which provides simple "greeting" methods.
 */
@Api(name = "tratarEmail", version = "v1", scopes = { Constants.EMAIL_SCOPE,
		Constants.GMAIL_SCOPE_READ, Constants.GMAIL_SCOPE_WRITE,
		Constants.DRIVE_SCOPE, Constants.DRIVE_SCOPE_FILE, Constants.SPREADSHEET_SCOPE }, clientIds = {
		Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID,
		Constants.IOS_CLIENT_ID, Constant.API_EXPLORER_CLIENT_ID }, audiences = { Constants.ANDROID_AUDIENCE })
public class EmailApi {

	@ApiMethod(name = "obterEmails", path = "email/obterEmails", httpMethod = HttpMethod.POST)
	public <T> Wrapper buscarEmails(final User user,
			final HttpServletRequest request) throws GeneralSecurityException,
			IOException {
		// http and json factory
		NetHttpTransport httpTransport = GoogleNetHttpTransport
				.newTrustedTransport();
		JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		// Token authorizator
		String authorizationHeader = request.getHeader("Authorization");
		String token = authorizationHeader.substring(7);
		GoogleCredential credential = new GoogleCredential()
				.setAccessToken(token);
		// gmail api
		Gmail gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName("tratarEmail").build();

		List<Message> messages = gmail.users().messages().list(user.getEmail())
				.setMaxResults(20l).execute().getMessages();

		

		//Spread
		Sheets spreadSheet = new Sheets.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("email")
        .build();
		
		Spreadsheet planilha = new Spreadsheet();
		
		Wrapper retorno = null;
		for (Message message : messages) {
			Message email = gmail.users().messages()
					.get(user.getEmail(), message.getId()).execute();
			
			List<MessagePartHeader> headers = email.getPayload().getHeaders();
			retorno = new Wrapper();
			for (MessagePartHeader h : headers) {
				if ("Subject".equals(h.getName())) {
					retorno.setAssunto(h.getValue());
				}
				if ("Date".equals(h.getName())) {
					retorno.setData(h.getValue());
				}
				if ("To".equals(h.getName())) {
					retorno.setDestinatario(h.getValue());
				}
			}			
		}

		
		SpreadsheetProperties propriedades = new SpreadsheetProperties();
		propriedades.setTitle("Planilha-Mails-" + new SimpleDateFormat("dd-MMM-yyyy-HH_mm_ss").format(new Date()));
		planilha.setProperties(propriedades);
		Spreadsheet execute = spreadSheet.spreadsheets().create(planilha).execute();
		ArrayList<GridData> gridData = new ArrayList<GridData>();
		GridData cell = new GridData();
		cell.set("A1", "AAAA");
		gridData.add(cell);
		Sheet sheet = execute.getSheets().get(0);
		sheet.setData(gridData);
		
		return new Wrapper(execute.getSpreadsheetId().toString(), execute.getProperties().getTitle());
	}

}

class Wrapper {
	private String assunto;
	private String data;
	private String destinatario;

	public Wrapper(String assunto, String data) {
		super();
		this.assunto = assunto;
		this.data = data;
	}

	public Wrapper() {
		// TODO Auto-generated constructor stub
	}

	public String getAssunto() {
		return assunto;
	}

	public String getDestinatario() {
		return destinatario;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setDestinatario(String destinatario) {
		this.destinatario = destinatario;
	}

	public void setAssunto(String assunto) {
		this.assunto = assunto;
	}

}
