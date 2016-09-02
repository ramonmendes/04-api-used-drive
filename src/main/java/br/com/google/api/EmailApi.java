package br.com.google.api;

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
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.appengine.api.users.User;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Defines v1 of a helloworld API, which provides simple "greeting" methods.
 */
@Api(name = "tratarEmail", version = "v1", scopes = {Constants.EMAIL_SCOPE,
    Constants.GMAIL_SCOPE_READ, Constants.GMAIL_SCOPE_WRITE,
    Constants.DRIVE_SCOPE, Constants.DRIVE_SCOPE_FILE, Constants.SPREADSHEET_SCOPE}, clientIds = {
    Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID,
    Constants.IOS_CLIENT_ID, Constant.API_EXPLORER_CLIENT_ID},
    audiences = {Constants.ANDROID_AUDIENCE})
public class EmailApi {

  /**
   *
   */
  private final String appName = "E-mail List";
  private NetHttpTransport httpTransport;
  private JacksonFactory jsonFactory;

  /**
   * @throws IOException
   * @throws GeneralSecurityException
   *
   */
  public EmailApi() throws GeneralSecurityException, IOException {
    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    jsonFactory = JacksonFactory.getDefaultInstance();
  }

  @ApiMethod(name = "obterEmails", path = "email/obterEmails", httpMethod = HttpMethod.POST)
  public <T> Wrapper buscarEmails(final User user,
      final HttpServletRequest request) throws GeneralSecurityException,
      IOException {

    // Obtendo credencial de acesso a API
    GoogleCredential credential = getCredential(request);

    // Obtendo dados de emails
    List<Wrapper> emails = gmailData(user, credential);

    // Spread
    Spreadsheet execute = createNewSpreadSheet(credential);

    // Populando planilha
    int i = 1;
    for (Wrapper wrapper : emails) {
      inserirLinhaPlanilha(execute, credential, wrapper, ++i);
    }

    return new Wrapper(execute.getSpreadsheetId().toString(), execute.getProperties().getTitle());
  }

  /**
   * @param request
   * @return
   */
  private GoogleCredential getCredential(final HttpServletRequest request) {
    // Token authorizator
    String authorizationHeader = request.getHeader("Authorization");
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
  private Spreadsheet createNewSpreadSheet(GoogleCredential credential) throws IOException {
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
        + new SimpleDateFormat("dd-MMM-yyyy-HH_mm_ss").format(new Date());
    propriedades.setTitle(nomePlanilha);
    return propriedades;
  }

  /**
   * @param service
   * @param values
   * @param execute
   * @throws IOException
   */
  private void inserirLinhaPlanilha(Spreadsheet execute, GoogleCredential credential, Wrapper item,
      int line)
      throws IOException {

    ValueRange value = new ValueRange();
    ArrayList<Object> conjuntoLinhas = new ArrayList<Object>();
    ArrayList<Object> linha = new ArrayList<Object>();
    String sheet = execute.getSheets().get(0).getProperties().getTitle();
    String range = sheet + "!A" + line + ":D" + line;

    linha.add(item.getId());
    linha.add(item.getAssunto());
    linha.add((item.getData() == null ? "-" : item.getData()));
    linha.add(item.getDestinatario());

    conjuntoLinhas.add(linha);

    value.set("values", conjuntoLinhas);
    value.setRange(range);

    getSpreadSheetService(credential).spreadsheets().values()
        .append(execute.getSpreadsheetId(), range, value).setValueInputOption("RAW").execute();
  }

  /**
   * @param user
   * @param httpTransport
   * @param jsonFactory
   * @param credential
   * @return
   * @throws IOException
   */
  private List<Wrapper> gmailData(final User user, GoogleCredential credential) throws IOException {
    // gmail api
    Gmail gmailService = getGmailService(credential);

    // Obtem lista de mensagens
    List<Message> messages = gmailService.users().messages().list(user.getEmail())
        .setMaxResults(50l).execute().getMessages();

    Wrapper retorno = null;

    List<Wrapper> lista = new ArrayList<Wrapper>();
    for (Message message : messages) {
      String idMessage = message.getId();
      Message email = gmailService.users().messages()
          .get(user.getEmail(), idMessage).execute();

      retorno = new Wrapper(idMessage);

      retorno.setData(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(email
          .getInternalDate())));

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

      lista.add(retorno);
    }

    return lista;
  }

  /**
   * @param credential
   * @return
   */
  private Gmail getGmailService(GoogleCredential credential) {
    return new Gmail.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName(appName).build();
  }

  /**
   * @param credential
   * @return
   */
  private Sheets getSpreadSheetService(GoogleCredential credential) {
    return new Sheets.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName(appName)
        .build();
  }

}


/**
 * The Class Wrapper.
 */
class Wrapper {
  private String assunto;
  private String data;
  private String destinatario;
  private String id;

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }



  /**
   * @param id
   */
  public Wrapper(String id) {
    super();
    this.id = id;
  }

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
