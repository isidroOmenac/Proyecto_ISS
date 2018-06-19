package vertx;

public class Lectura {

	private int humedad;
	private int temperatura;
	
	
	public Lectura() {
		super();
	}
	
	public int getHumedad() {
		return humedad;
	}
	public void setHumedad(int humedad) {
		this.humedad = humedad;
	}
	public int getTemperatura() {
		return temperatura;
	}
	public void setTemperatura(int temperatura) {
		this.temperatura = temperatura;
	}
	@Override
	public String toString() {
		return "Lectura [humedad=" + humedad + ", temperatura=" + temperatura + "]";
	}
	
	
	
}
