package br.com.google.to;

/**
 * The Class Wrapper.
 */
public class EmailDetalhe {
	private String assunto;
	private String data;
	private String destinatario;
	private String id;
	private String tamanho = "0";

	public String getTamanho() {
		return tamanho;
	}

	public void setTamanho(String string) {
		this.tamanho = string;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param id
	 */
	public EmailDetalhe(String id) {
		super();
		this.id = id;
	}

	public EmailDetalhe(String assunto, String data) {
		super();
		this.assunto = assunto;
		this.data = data;
	}

	public EmailDetalhe() {
		this.tamanho = "0 mb";
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
