package vertx;

public class OrdenIss {
	private long id;
	private float temp;
	private String user;
	private long fechaHora;
	private String sensor;
	
	public OrdenIss() {
		this(0L,0f,"",0L,"");
	}
	
	public OrdenIss(long id, float temp, String user, long fechaHora, String sensor) {
		super();
		this.id = id;
		this.temp = temp;
		this.user = user;
		this.fechaHora = fechaHora;
		this.sensor = sensor;
	}

	@Override
	public String toString() {
		return "Orden [id=" + id + ", temp=" + temp + ", user=" + user + ", fechaHora=" + fechaHora + ", sensor="
				+ sensor + "]";
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public float getTemp() {
		return temp;
	}

	public void setTemp(float temp) {
		this.temp = temp;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public long getFechaHora() {
		return fechaHora;
	}

	public void setFechaHora(long fechaHora) {
		this.fechaHora = fechaHora;
	}

	public String getSensor() {
		return sensor;
	}

	public void setSensor(String sensor) {
		this.sensor = sensor;
	}
	
	
	
	

}
