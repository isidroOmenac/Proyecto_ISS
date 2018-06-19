package vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class DadFirst extends AbstractVerticle{
	
	@Override
	public void start() throws Exception {
		super.start();
		/*
		JsonObject json = new JsonObject();
		json.put("numero", 18);
		json.put("plazas", 32);
		json.put("nombre", "AV74");
		
		JsonObject plaza = new JsonObject();
		plaza.put("posicion", "3A");
		plaza.put("emergencia", true);
		json.put("plaza", plaza);
		
		json.put("hora", 18);
		
		System.out.println(json.encodePrettily());
		
		String jsonString = json.encode();
		JsonObject jsonResult = new JsonObject(jsonString);
		System.out.println(jsonResult.getInteger("numero"));
		System.out.println(jsonResult.getJsonObject("plaza"));
		*/
		/*Lectura lectura = new Lectura();
		lectura.setHumedad(89);
		lectura.setTemperatura(20);
		String lecturaStr = Json.encode(lectura);
		System.out.println(lecturaStr);
		
		Lectura l = Json.decodeValue(lecturaStr, Lectura.class);
		System.out.println(l.toString());
		
		vertx.executeBlocking(param -> {
			try {
				Thread.sleep(10000);
				param.complete("Finalizado");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, param2 -> {
			System.out.println(param2.result().toString());
		});
		
		
		vertx.deployVerticle(DadSecond.class.getName(), 
			res -> {
				if (res.succeeded()) {
					System.out.println("DadSecond lanzado correctamente");
					
					vertx.setPeriodic(5000, exe -> {
					
					vertx.eventBus().send("mensaje-punto-a-punto", 
							"DadSecond, ¿estás ahí?", 
							reply -> {
								System.out.println(reply.result().body());
								reply.result().reply("Pues vale");
							});
					
					});
					
					vertx.eventBus().publish("mensaje-broadcast", 
							"Soy un mensaje broadcast, ¿alguien me lee?");
					
					
				}else {
					System.out.println("Error en el lanzamiento de DadSecond");					
				}
			});
				
		vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
			
			public void handle(HttpServerRequest arg0) {
				arg0.response().end("Hola mundo");
			}
		}).listen(8081);*/
		
		
	}
	
}
