package br.com.google.servlet;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.com.google.api.EmailApi;
import br.com.google.service.GoogleService;
import br.com.google.service.GoogleServiceImpl;

public class TaskQueueServlet extends HttpServlet {
	Logger LOG = Logger.getLogger(TaskQueueServlet.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = -4905283914962403575L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		execute(req, resp, "post");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		execute(req, resp, "get");
	}

	public void execute(HttpServletRequest req,HttpServletResponse resp, String method)
			throws IOException {
	    String spread = req.getParameter(EmailApi.SPREAD_SHEET_ID);
	    String oauth = req.getParameter(EmailApi.OAUTH_AUTORIZATION);
	    String login = req.getParameter(EmailApi.USER_MAIL);
	    String msg = "Informacoes:" + method + "::" + spread + "::" + oauth + "::" + login;
	    LOG.info(msg);

	    try {
			GoogleService service = new GoogleServiceImpl();
			//Preencher planilha
			service.preencherPlanilhaEmail(login, oauth, spread);
		} catch (GeneralSecurityException | InterruptedException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	    
		resp.getWriter().println(msg);
		resp.getWriter().flush();
	}

}
