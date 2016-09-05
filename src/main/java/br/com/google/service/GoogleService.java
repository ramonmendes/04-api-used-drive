package br.com.google.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.model.Spreadsheet;

public interface GoogleService {

	GoogleCredential getCredential(HttpServletRequest request);

	Spreadsheet createNewSpreadSheet(GoogleCredential credential)
			throws IOException;

	GoogleCredential getCredential(String authorizationHeader);

	void preencherPlanilhaEmail(String user, String autorizacao,
			String idPlanilha) throws IOException, InterruptedException;

	EmailWrapper gmailData(String user, String autorization,
			EmailWrapper gmailData) throws IOException;
	
}
