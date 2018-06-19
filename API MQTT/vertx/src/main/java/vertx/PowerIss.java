package vertx;

public class PowerIss {
	private long id;
	private String user;
	private String sensor;
	private long fechaHora;
	
	public PowerIss() {
		
		this(0L, "", "",0L);
	}
	
	public PowerIss(long id, String user, String sensor, long fechaHora) {
		super();
		this.id = id;
		this.user = user;
		this.sensor = sensor;
		this.fechaHora = fechaHora;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getSensor() {
		return sensor;
	}

	public void setSensor(String sensor) {
		this.sensor = sensor;
	}

	public long getFechaHora() {
		return fechaHora;
	}

	public void setFechaHora(long fechaHora) {
		this.fechaHora = fechaHora;
	}

	@Override
	public String toString() {
		return "Power [id=" + id + ", user=" + user + ", sensor=" + sensor
				+ ", fechaHora=" + fechaHora + "]";
	}
	
	

}
