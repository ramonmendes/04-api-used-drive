package br.com.google.api;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.servlet.http.HttpServletRequest;

import br.com.google.service.GoogleService;
import br.com.google.service.GoogleServiceImpl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;

/**
 * Defines v1 of a helloworld API, which provides simple "greeting" methods.
 */
@Api(name = "tratarEmail", version = "v1", scopes = { Constants.EMAIL_SCOPE,
		Constants.GMAIL_SCOPE_READ, Constants.GMAIL_SCOPE_WRITE,
		Constants.DRIVE_SCOPE, Constants.DRIVE_SCOPE_FILE,
		Constants.SPREADSHEET_SCOPE }, clientIds = { Constants.WEB_CLIENT_ID,
		Constants.ANDROID_CLIENT_ID, Constants.IOS_CLIENT_ID,
		Constant.API_EXPLORER_CLIENT_ID }, audiences = { Constants.ANDROID_AUDIENCE })
public class EmailApi {

	public static final String USER_MAIL = "user";
	public static final String OAUTH_AUTORIZATION = "OAUTH_AUTORIZATION";
	public static final String SPREAD_SHEET_ID = "spreadSheetId";

	/**
	 * @throws IOException
	 * @throws GeneralSecurityException
	 *
	 */
	public EmailApi() throws GeneralSecurityException, IOException {

	}

	@ApiMethod(name = "obterEmails", path = "email/obterEmails", httpMethod = HttpMethod.POST)
	public WraperResponse buscarEmails(final User user,
			final HttpServletRequest request) throws GeneralSecurityException,
			IOException {

		GoogleService service = new GoogleServiceImpl();

		// Obtendo credencial de acesso a API
		GoogleCredential credential = service.getCredential(request);

		// Spread
		Spreadsheet execute = service.createNewSpreadSheet(credential);

		// Add item in task queue
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(TaskOptions.Builder.withUrl("/taskqueue/leremail")
				.param(SPREAD_SHEET_ID, execute.getSpreadsheetId())
				.param(OAUTH_AUTORIZATION, request.getHeader("Authorization"))
				.param(USER_MAIL, user.getEmail()));

		return new WraperResponse(execute.getSpreadsheetId().toString(), execute
				.getProperties().getTitle());
	}

}

class WraperResponse {
	private String idPlanilha;
	private String nomePlanilha;
	
	public WraperResponse(String idPlanilha, String nomePlanilha) {
		this.idPlanilha = idPlanilha;
		this.nomePlanilha = nomePlanilha;
	}
	
	public String getIdPlanilha() {
		return idPlanilha;
	}
	public void setIdPlanilha(String idPlanilha) {
		this.idPlanilha = idPlanilha;
	}
	public String getNomePlanilha() {
		return nomePlanilha;
	}
	public void setNomePlanilha(String nomePlanilha) {
		this.nomePlanilha = nomePlanilha;
	}
}
