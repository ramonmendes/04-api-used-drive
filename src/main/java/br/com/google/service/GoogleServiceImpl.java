package br.com.google.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import br.com.google.to.EmailDetalhe;
import br.com.google.utils.Utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleServiceImpl implements GoogleService {
	Logger LOG = Logger.getLogger(GoogleServiceImpl.class.getName());

	/**
	   *
	   */
	private final String appName = "E-mail List";
	private NetHttpTransport httpTransport;
	private JacksonFactory jsonFactory;

	public GoogleServiceImpl() throws GeneralSecurityException, IOException {
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		jsonFactory = JacksonFactory.getDefaultInstance();
	}

	/**
	 * @param request
	 * @return
	 */
	@Override
	public GoogleCredential getCredential(final HttpServletRequest request) {
		// Token authorizator
		String authorizationHeader = request.getHeader("Authorization");
		GoogleCredential credential = getCredential(authorizationHeader);
		return credential;
	}

	@Override
	public void preencherPlanilhaEmail(String user, String autorizacao,
			String idPlanilha) throws IOException, InterruptedException {
		Integer x = 0;
		List<EmailDetalhe> emailDetalhes = null;
		EmailWrapper gmailData = new EmailWrapper();
		do{
			// Resgantando os e-mails
			gmailData = gmailData(user, autorizacao,gmailData);
			emailDetalhes = gmailData.getEmailDetalhes();
			// Preenchendo planilha
			x = preencherPlanilhaComEmail(autorizacao, idPlanilha, emailDetalhes, x);
		}while(emailDetalhes!=null && emailDetalhes.size() > 0);
	}

	private int preencherPlanilhaComEmail(String autorizacao,
			String idPlanilha, List<EmailDetalhe> emails, int indice) throws IOException,
			InterruptedException {
		//Inicio
		long logWatchStart = Utils.logWatchStart();
		
		GoogleCredential credential = getCredential(autorizacao);
		Sheets planilhaService = getSpreadSheetService(credential);
		Spreadsheet spread = planilhaService.spreadsheets().get(idPlanilha)
				.execute();
		ArrayList<Object> conjuntoLinhas = new ArrayList<Object>();
		ArrayList<Object> linha = new ArrayList<Object>();
		ValueRange value = new ValueRange();

		for (EmailDetalhe emailDetalhe : emails) {
			linha.add(emailDetalhe.getId());
			linha.add(emailDetalhe.getAssunto());
			linha.add((emailDetalhe.getData() == null ? "-" : emailDetalhe
					.getData()));
			linha.add(emailDetalhe.getDestinatario() == null ? "-" : emailDetalhe.getDestinatario());
			linha.add(emailDetalhe.getTamanho());
			conjuntoLinhas.add(linha);
			linha = new ArrayList<Object>();
			++indice;
		}

		String range = spread.getSheets().get(0).getProperties().getTitle()
				+ "!A"+(indice - 499)+":E" + indice;
		value.set("values", conjuntoLinhas);
		value.setRange(range);

		//ArrayList<ValueRange> values = new ArrayList<>();
		//values.add(value);
		//BatchUpdateValuesRequest request = new BatchUpdateValuesRequest();
		//request.setValueInputOption("RAW");
		//request.setData(values);
		//batchUpdate(idPlanilha, request)

		long logApi = Utils.logWatchStart();
		
		planilhaService.spreadsheets().values()
				.append(idPlanilha, range, value)
				.setValueInputOption("RAW")
				.setInsertDataOption("INSERT_ROWS")
				.execute();
		
		Utils.logWatchStop(logApi,"API_SPREADSHEET");
		
		Utils.logWatchStop(logWatchStart);
		
		return indice;

	}

	/**
	 * 
	 * @param authorizationHeader
	 * @return
	 */
	@Override
	public GoogleCredential getCredential(String authorizationHeader) {
		String token = authorizationHeader.substring(7);
		GoogleCredential credential = new GoogleCredential()
				.setAccessToken(token);
		return credential;
	}

	/**
	 * @param httpTransport
	 * @param jsonFactory
	 * @param credential
	 * @return
	 * @throws IOException
	 */
	@Override
	public Spreadsheet createNewSpreadSheet(GoogleCredential credential)
			throws IOException {
		Sheets service = getSpreadSheetService(credential);

		// Criando uma nova planilha
		Spreadsheet planilha = new Spreadsheet();

		SpreadsheetProperties propriedades = getPropriedadesPlanilha();
		planilha.setProperties(propriedades);

		Spreadsheet execute = service.spreadsheets().create(planilha).execute();

		return execute;
	}

	/**
	 * @return
	 */
	private SpreadsheetProperties getPropriedadesPlanilha() {
		SpreadsheetProperties propriedades = new SpreadsheetProperties();
		String nomePlanilha = "Planilha-Mails-"
				+ new SimpleDateFormat("dd-MMM-yyyy-HH_mm_ss")
						.format(new Date());
		propriedades.setTitle(nomePlanilha);
		return propriedades;
	}

	/**
	 * @param user
	 * @param gmailData 
	 * @param httpTransport
	 * @param jsonFactory
	 * @param credential
	 * @return
	 * @throws IOException
	 */
	@Override
	public EmailWrapper gmailData(final String user,
			final String autorization, EmailWrapper gmailData) throws IOException {
		
		GoogleCredential credential = getCredential(autorization);
		return retornaOsEmails(user, credential, gmailData.getToken());
	}

	private EmailWrapper retornaOsEmails(final String user,
			GoogleCredential credential, String pageToken) throws IOException {
		long logWatchStart = Utils.logWatchStart();
		// gmail api
		Gmail gmailService = getGmailService(credential);

		long logApi = Utils.logWatchStart();
		// Obtem lista de mensagens
		ListMessagesResponse execute = gmailService
				.users()
				.messages()
				.list(user)
				.setPageToken(pageToken)
				.setMaxResults(500l)
				.setFields("payload,sizeEstimate,snippet,threadId,nextPageToken,resultSizeEstimate")
				// .setQ("larger:5MB")
				.execute();
		Utils.logWatchStop(logApi,"API_GMAIL");

		List<Message> messages = execute.getMessages();
		EmailDetalhe retorno = null;
		List<EmailDetalhe> lista = new ArrayList<EmailDetalhe>();
		for (Message message : messages) {
			String idMessage = message.getId();
			LOG.info(message.getPayload().toString());
			Message email = gmailService.users().messages()
					.get(user, idMessage).execute();

			retorno = new EmailDetalhe(idMessage);

			retorno.setData(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
					.format(new Date(email.getInternalDate())));

			List<MessagePartHeader> headers = email.getPayload().getHeaders();
			List<String> labelIds = email.getLabelIds();
			if (labelIds.contains("CHAT")) {

				retorno.setAssunto("Hangout:" + email.getSnippet());

				for (MessagePartHeader h : headers) {
					if ("From".equals(h.getName())) {
						retorno.setDestinatario("Origem:" + h.getValue());
					}
				}
			} else {
				for (MessagePartHeader h : headers) {
					if ("Subject".equals(h.getName())) {
						retorno.setAssunto(h.getValue());
					}
					if ("To".equals(h.getName())) {
						retorno.setDestinatario(h.getValue());
					}
				}

			}

			Integer size = 0;
			List<MessagePart> parts = email.getPayload().getParts();
			if (parts != null && parts.size() > 0) {
				for (MessagePart messagePart : parts) {
					size += messagePart.getBody().getSize();
				}
			}

			if (size > 0) {
				Double tamanho = Double.parseDouble(size.toString()) / 1024d / 1024d;
				retorno.setTamanho(new DecimalFormat("#.##").format(tamanho));
			}

			lista.add(retorno);
		}
		
		EmailWrapper emailWrapper = new EmailWrapper();
		emailWrapper.setEmailDetalhes(lista);
		emailWrapper.setToken(execute.getNextPageToken());

		Utils.logWatchStop(logWatchStart);
		
		return emailWrapper;
	}

	/**
	 * @param credential
	 * @return
	 */
	public Gmail getGmailService(GoogleCredential credential) {
		return new Gmail.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName(appName).build();
	}

	/**
	 * @param credential
	 * @return
	 */
	public Sheets getSpreadSheetService(GoogleCredential credential) {
		return new Sheets.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName(appName).build();
	}

}

class EmailWrapper{
	private String token;
	private List<EmailDetalhe> emailDetalhes;
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public List<EmailDetalhe> getEmailDetalhes() {
		return emailDetalhes;
	}
	public void setEmailDetalhes(List<EmailDetalhe> emailDetalhes) {
		this.emailDetalhes = emailDetalhes;
	}
}
