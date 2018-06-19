package vertx;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class RestEP extends AbstractVerticle {
	
	private SQLClient mySQLClient;
	private MqttClient mqttClient;
	
	private static Multimap<String, MqttEndpoint> clientTopics;
	
	public void start(Future<Void> startFuture) {
		
		JsonObject mySQLClientConfig = new JsonObject()
				.put("host", "127.0.0.1")
				.put("port", 3308)
				.put("database", "iss")
				.put("username", "root")
				.put("password", "root");
		
		mySQLClient = MySQLClient.createShared
				(vertx, mySQLClientConfig);

		Router router = Router.router(vertx);

		vertx.createHttpServer().requestHandler(router::accept).listen(8083, res -> {
			if (res.succeeded()) {
				System.out.println("Servidor REST desplegado");
			} else {
				System.out.println("Error: " + res.cause());
			}
		});

		router.route("/ISS/*").handler(BodyHandler.create());
		router.get("/ISS/registroAmbiente/user/:userFilter").handler(this::getAmbByUser);
		router.get("/ISS/registroAmbiente/user/:userFilter/sensor/:idSensor").handler(this::getAmbByUserSensor);
		router.get("/ISS/registroOrdenes/user/:userFilter").handler(this::getOrdByUser);
		router.get("/ISS/registroOrdenes/user/:userFilter/sensor/:idSensor").handler(this::getOrdByUserSensor);
		router.get("/ISS/mideTemperatura/user/:userFilter/sensor/:idSensor").handler(this::mideTemperatura);
		router.get("/ISS/registroCambios/:userFilter").handler(this::getAllRegistroCambios);
		router.put("/ISS/cambiarTemperatura/user/sensor").handler(this::changeTemp);
		router.put("/ISS/encender/user/sensor").handler(this::powerOn);
		router.put("/ISS/apagar/user/sensor").handler(this::powerOff);
		router.put("/ISS/guardaTemperatura").handler(this::guardaTemp);
	
//	MqttServer mqttServer = MqttServer.create(vertx);
//	initialize(mqttServer);
//	
//	MqttClient mqttClient = MqttClient.create(vertx,
//			new MqttClientOptions().setAutoKeepAlive(true));
//	mqttClient.connect(8123, "localhost", handler->{
//		mqttClient.subscribe("topic_1",MqttQoS.AT_LEAST_ONCE.value(), msg->{
//			System.out.println("Mensaje recibido: " + msg.toString());
//		});
//		mqttClient.publish("topic_1", Buffer.buffer("Mi primer mensaje"), MqttQoS.AT_LEAST_ONCE,	
//				false, false);
//	});
	
	clientTopics = HashMultimap.create();

	// Configuramos el servidor MQTT
	MqttServer mqttServer = MqttServer.create(vertx);
	init(mqttServer);

	// Creamos un cliente de prueba para MQTT que publica mensajes cada 3 segundos
	mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
	
	// Nos conectamos al servidor que est� desplegado por el puerto 1883 en la propia m�quina.
	// Recordad que localhost debe ser sustituido por la IP de vuestro servidor. Esta IP puede
	// cambiar cuando os desconect�is de la red, por lo que aseguraros siempre antes de lanzar
	// el cliente que la IP es correcta.
	mqttClient.connect(1883, "localhost", s -> {
		
		// Nos suscribimos al topic_2. Aqu� deber�a indicar el nombre del topic al que os quer�is
		// suscribir. Adem�s, pod�is indicar el QoS, en este caso AT_LEAST_ONCE para asegurarnos
		// de que el mensaje llega a su destinatario.
		mqttClient.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
			if (handler.succeeded()) {
				// En este punto el cliente ya est� suscrito al servidor, puesto que se ha
				// ejecutado la funci�n de handler
				System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
			}
		});
		
		// Este timer simular� el env�o de mensajes desde el cliente 1 al servidor cada 3 segundos.
		new Timer().scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				// Publicamos un mensaje en el topic "topic_2" con el contenido "Ejemplo" y la hora.
				// Ajustamos el QoS para que se entregue al menos una vez. Indicamos que el
				// mensaje NO es un duplicado (false) y que NO debe ser retenido en el canal
				// (false)
				//mqttClient.publish("topic_2", Buffer.buffer("Ejemplo a las " + Calendar.getInstance().getTime().toString()), MqttQoS.AT_LEAST_ONCE, false, false);
			}
		}, 1000, 3000);
	});

	// Ahora creamos un segundo cliente, al que se supone deben llegar todos los mensajes que el
	// cliente 1 desplegado anteriormente publique en el topic "topic_2". Este era el punto en el 
	// que el proyecto anterior fallaba, debido a que no exist�a ning�n broken que se encargara
	// de realizar el env�o desde el servidor al resto de clientes.
	MqttClient mqttClient2 = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
	mqttClient2.connect(1883, "localhost", s -> {

		// Al igual que antes, este cliente se suscribe al topic_2 para poder recibir los mensajes
		// que el cliente 1 env�e a trav�s de MQTT.
		mqttClient2.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
			if (handler.succeeded()) {
				// En este punto, el cliente 2 tambi�n est� suscrito al servidor, por lo que ya podr�
				// empezar a recibir los mensajes publicados en el topic.
				System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
				
				// Adem�s de suscribirnos al servidor, registraremos un manejador para interceptar los mensajes
				// que lleguen a nuestro cliente. De manera que el proceso ser�a el siguiente:
				// El cliente 1 env�a un mensaje al servidor -> el servidor lo recibe y busca los clientes suscritos
				//     al topic -> el servidor reenv�a el mensaje a esos clientes -> los clientes (en este caso
				//     el cliente 2) recibe el mensaje y lo proceso si fuera necesario.
				mqttClient2.publishHandler(new Handler<MqttPublishMessage>() {
					@Override
					public void handle(MqttPublishMessage arg0) {
						// Si se ejecuta este c�digo es que el cliente 2 ha recibido un mensaje publicado
						// en alg�n topic al que estaba suscrito (en este caso, al topic_2).
						System.out.println("Mensaje recibido por el cliente 2: " + arg0.payload().toString());							
					}
				});
			}
		});

	});
	
	
	
}

//	public void initialize(MqttServer mqttServer) {
//		mqttServer.endpointHandler(new Handler<MqttEndpoint>() {
//			
//			@Override
//			public void handle(MqttEndpoint endpoint) {
//				endpoint.accept(false);
//				handlerSubscription(endpoint);//cliente se autoincluye en un canal
//				handlerUnsubscription(endpoint);
//				handlerPublish(endpoint);//publica en el canal
//				handlerDisconected(endpoint);//te borras del serv
//			}
//		}).listen(8123, handler->{
//			if(handler.succeeded()) {
//				System.out.println("Servidor MQTT desplegado");
//			}else {
//				System.out.println("Error: " + handler.cause());
//			}
//		});
//	}
	

	
//	protected void handlerDisconected(MqttEndpoint endpoint) {
//		endpoint.disconnectHandler(disconnect->{
//			System.out.println("El cliente " + endpoint.clientIdentifier() +
//					" se ha desconectado");
//		});	
//	}
//
//	protected void handlerPublish(MqttEndpoint endpoint) {
//		endpoint.publishHandler(message->{
//			System.out.println("Topic: " + message.topicName() +
//					". Contenido: " + message.payload().toString());//payload contenido del mensaje
//			//TODO: podriamos guardar en BBDD
//			if(message.qosLevel() == MqttQoS.EXACTLY_ONCE) {// Lo saca el mensaje del bus 
//				endpoint.publishRelease(message.messageId());
//			}
//		});
//	}
//
//	protected void handlerUnsubscription(MqttEndpoint endpoint) {
//		endpoint.unsubscribeHandler(unsuscribe->{
//			for(String topic: unsuscribe.topics()) {
//				System.out.println("El cliente " +
//						endpoint.clientIdentifier()
//			+ " ha eliminado la suscripción del canal" + topic);
//			}
//			endpoint.unsubscribeAcknowledge(unsuscribe.messageId());//acuse de recibo
//		});
//		
//	}
//
//	protected void handlerSubscription(MqttEndpoint endpoint) {
//		endpoint.subscribeHandler(subscribe->{
//			
//			List<MqttQoS> grantedQoS = new ArrayList<MqttQoS>();
//			for(MqttTopicSubscription s: subscribe.topicSubscriptions()) {
//				System.out.println("Suscripción al topic: "+s.topicName());
//				grantedQoS.add(s.qualityOfService());//por cada de las salas he añadido el nivel de calidad que el cliente ha solicitado
//			}
//			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQoS);
//		});
//		
//	}

	
	/**
	 * M�todo encargado de inicializar el servidor y ajustar todos los manejadores
	 * @param mqttServer
	 */
	private static void init(MqttServer mqttServer) {
		mqttServer.endpointHandler(endpoint -> {
			// Si se ejecuta este c�digo es que un cliente se ha suscrito al servidor MQTT para 
			// alg�n topic.
			System.out.println("Nuevo cliente MQTT [" + endpoint.clientIdentifier()
					+ "] solicitando suscribirse [Id de sesi�n: " + endpoint.isCleanSession() + "]");
			// Indicamos al cliente que se ha contectado al servidor MQTT y que no ten�a
			// sesi�n previamente creada (par�metro false)
			endpoint.accept(false);

			// Handler para gestionar las suscripciones a un determinado topic. Aqu� registraremos
			// el cliente para poder reenviar todos los mensajes que se publicen en el topic al que
			// se ha suscrito.
			handleSubscription(endpoint);

			// Handler para gestionar las desuscripciones de un determinado topic. Haremos lo contrario
			// que el punto anterior para eliminar al cliente de la lista de clientes registrados en el 
			// topic. De este modo, no seguir� recibiendo mensajes en este topic.
			handleUnsubscription(endpoint);

			// Este handler ser� llamado cuando se publique un mensaje por parte del cliente en alg�n
			// topic creado en el servidor MQTT. En esta funci�n obtendremos todos los clientes
			// suscritos a este topic y reenviaremos el mensaje a cada uno de ellos. Esta es la tarea
			// principal del broken MQTT. En este caso hemos implementado un broker muy muy sencillo. 
			// Para gestionar QoS, asegurar la entregar, guardar los mensajes en una BBDD para despu�s
			// entregarlos, guardar los clientes en caso de ca�da del servidor, etc. debemos recurrir
			// a un c�digo m�s elaborado o usar una soluci�n existente como por ejemplo Mosquitto.
			publishHandler(endpoint);

			// Handler encargado de gestionar las desconexiones de los clientes al servidor. En este caso
			// eliminaremos al cliente de todos los topics a los que estuviera suscrito.
			handleClientDisconnect(endpoint);
		}).listen(ar -> {
			if (ar.succeeded()) {
				System.out.println("MQTT server está a la escucha por el puerto " + ar.result().actualPort());
			} else {
				System.out.println("Error desplegando el MQTT server");
				ar.cause().printStackTrace();
			}
		});
	}

	/**
	 * M�todo encargado de gestionar las suscripciones de los clientes a los diferentes topics.
	 * En este m�todo se registrar� el cliente asociado al topic al que se suscribe
	 * @param endpoint
	 */
	private static void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {
			// Los niveles de QoS permiten saber el tipo de entrega que se realizar�:
			// - AT_LEAST_ONCE: Se asegura que los mensajes llegan a los clientes, pero no
			// que se haga una �nica vez (pueden llegar duplicados)
			// - EXACTLY_ONCE: Se asegura que los mensajes llegan a los clientes un �nica
			// vez (mecanismo m�s costoso)
			// - AT_MOST_ONCE: No se asegura que el mensaje llegue al cliente, por lo que no
			// es necesario ACK por parte de �ste
			List<MqttQoS> grantedQosLevels = new ArrayList<>();
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Suscripción al topic " + s.topicName() + " con QoS " + s.qualityOfService());
				grantedQosLevels.add(s.qualityOfService());
				
				if (clientTopics.keySet().contains(s.topicName())) {
					Optional<MqttEndpoint> opt = clientTopics.get(s.topicName()).stream().filter(elem -> elem.clientIdentifier().equals(endpoint.clientIdentifier())).findAny();
					if (opt.isPresent()) {
						clientTopics.remove(s.topicName(), opt.get());
					}
				}
				
				// A�adimos al cliente en la lista de clientes suscritos al topic
				clientTopics.put(s.topicName(), endpoint);
			}
		
			// Enviamos el ACK al cliente de que se ha suscrito al topic con los niveles de
			// QoS indicados
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
		});
	}

	/**
	 * M�todo encargado de eliminar la suscripci�n de un cliente a un topic.
	 * En este m�todo se eliminar� al cliente de la lista de clientes suscritos a ese topic.
	 * @param endpoint
	 */
	private static void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String t : unsubscribe.topics()) {
				// Eliminos al cliente de la lista de clientes suscritos al topic
				clientTopics.remove(t, endpoint);
				System.out.println("Eliminada la suscripción del topic " + t);
			}
			// Informamos al cliente que la desuscripci�n se ha realizado
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}

	/**
	 * Manejador encargado de interceptar los env�os de mensajes de los diferentes clientes.
	 * Este m�todo deber� procesar el mensaje, identificar los clientes suscritos al topic donde
	 * se publica dicho mensaje y enviar el mensaje a cada uno de esos clientes.
	 * @param endpoint
	 */
	private static void publishHandler(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			// Suscribimos un handler cuando se solicite una publicaci�n de un mensaje en un
			// topic
			handleMessage(message, endpoint);
		}).publishReleaseHandler(messageId -> {
			// Suscribimos un handler cuando haya finalizado la publicaci�n del mensaje en
			// el topic
			endpoint.publishComplete(messageId);
		});
	}

	/**
	 * M�todo de utilidad para la gesti�n de los mensajes salientes.
	 * @param message
	 * @param endpoint
	 */
	private static void handleMessage(MqttPublishMessage message, MqttEndpoint endpoint) {
		System.out.println("Mensaje publicado por el cliente " + endpoint.clientIdentifier() + " en el topic "
				+ message.topicName());
		System.out.println("    Contenido del mensaje: " + message.payload().toString());
		
		// Obtenemos todos los clientes suscritos a ese topic (exceptuando el cliente que env�a el 
		// mensaje) para as� poder reenviar el mensaje a cada uno de ellos. Es aqu� donde nuestro
		// c�digo realiza las funciones de un broken MQTT
		System.out.println("Origen: " + endpoint.clientIdentifier());
		for (MqttEndpoint client: clientTopics.get(message.topicName())) {
			System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				try {
				client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(), message.isRetain()).publishReleaseHandler(idHandler -> {
					client.publishComplete(idHandler);
				});
				}
				catch(Exception e){
					System.out.println("Error:"+e.getCause());
				}
		}
		
		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();
			switch (topicName) {
			// Se podr�a hacer algo con el mensaje como, por ejemplo, almacenar un registro
			// en la base de datos
			}
			// Env�a el ACK al cliente de que el mensaje ha sido publicado
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			// Env�a el ACK al cliente de que el mensaje ha sido publicado y cierra el canal
			// para este mensaje. As� se evita que los mensajes se publiquen por duplicado
			// (QoS)
			endpoint.publishRelease(message.messageId());
		}
	}

	/**
	 * Manejador encargado de notificar y procesar la desconexi�n de los clientes.
	 * @param endpoint
	 */
	private static void handleClientDisconnect(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(h -> {
			// Eliminamos al cliente de todos los topics a los que estaba suscritos
			Stream.of(clientTopics.keySet())
				.filter(e -> clientTopics.containsEntry(e, endpoint))
				.forEach(s -> clientTopics.remove(s, endpoint));
			System.out.println("El cliente remoto se ha desconectado [" + endpoint.clientIdentifier() + "]");
		});
	}
	
	//--------------------API REST---------------------
	
	private void changeTemp(RoutingContext routingContext) {
		try {
			OrdenIss orden = Json.decodeValue(routingContext.getBody(), OrdenIss.class);
			String user = orden.getUser();
			String sensor = orden.getSensor();
			float nTemp = orden.getTemp();
			
			long time = Calendar.getInstance().getTimeInMillis();
			
			JsonObject peticion = new JsonObject();
			peticion.put("tipo", "cambiaTemperatura");
			//peticion.put("id", "");
			peticion.put("temp", nTemp);
			peticion.put("user", user);
			//peticion.put("fechaHora", 0);
			peticion.put("sensor", sensor);
			
//			JsonArray peticion = new JsonArray()
//					.add(tipo)
//					.add(nTemp).add(user)
//					.add(time).add(sensor);
			
			mqttClient.publish("topic_2", Buffer.buffer(Json.encode(peticion)), MqttQoS.AT_LEAST_ONCE, false, false);
			
			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "INSERT INTO registro_user(id,temp,user,fechaHora,sensor)"
							+ "VALUES ('',?,?,?,?) ";
					JsonArray paramQuery = new JsonArray()
							.add(nTemp).add(user)
							.add(time).add(sensor);
					connection.queryWithParams(query, paramQuery, 
							res -> {
								if (res.succeeded()) {
									routingContext.response().end(Json.encodePrettily(res.result().getRows()));
								}else {
									routingContext.response().setStatusCode(400).end(
											"Error: " + res.cause());	
								}
							});
				}else {
					routingContext.response().setStatusCode(400).end(
							"Error: " + conn.cause());
				}
			});
			
			
			
			//routingContext.response().setStatusCode(200).
			//	end(Json.encodePrettily(database.get(param)));
		}catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	private void guardaTemp(RoutingContext routingContext) {
		try {
			OrdenIss orden = Json.decodeValue(routingContext.getBody(), OrdenIss.class);
			String user = orden.getUser();
			String sensor = orden.getSensor();
			float nTemp = orden.getTemp();
			
			long time = Calendar.getInstance().getTimeInMillis();
			
			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "INSERT INTO registro_amb(id,temp,user,fechaHora,sensor)"
							+ "VALUES ('',?,?,?,?) ";
					JsonArray paramQuery = new JsonArray()
							.add(nTemp).add(user)
							.add(time).add(sensor);
					connection.queryWithParams(query, paramQuery, 
							res -> {
								if (res.succeeded()) {
									routingContext.response().end(Json.encodePrettily(res.result().getRows()));
								}else {
									routingContext.response().setStatusCode(400).end(
											"Error: " + res.cause());	
								}
							});
				}else {
					routingContext.response().setStatusCode(400).end(
							"Error: " + conn.cause());
				}
			});
			
			
			
			//routingContext.response().setStatusCode(200).
			//	end(Json.encodePrettily(database.get(param)));
		}catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}
	}

	
	private void getAmbByUser(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("userFilter");
		if (paramStr != null) {
			try {
				String param = paramStr;
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT * "
								+ "FROM registro_amb "
								+ "WHERE user = ?";
						JsonArray paramQuery = new JsonArray()
								.add(param);
						connection.queryWithParams(
								query, 
								paramQuery, 
								res -> {
									if (res.succeeded()) {
										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
									}else {
										routingContext.response().setStatusCode(400).end(
												"Error: " + res.cause());	
									}
								});
					}else {
						routingContext.response().setStatusCode(400).end(
								"Error: " + conn.cause());
					}
				});
				
				
				
				//routingContext.response().setStatusCode(200).
				//	end(Json.encodePrettily(database.get(param)));
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	private void getAmbByUserSensor(RoutingContext routingContext) {
		String paramStr1 = routingContext.request().getParam("userFilter");
		String paramStr2 = routingContext.request().getParam("idSensor");
		if (paramStr1 != null && paramStr2 != null) {
			try {
				String user = paramStr1;
				String sensor = paramStr2;
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT * "
								+ "FROM registro_amb "
								+ "WHERE user = ? AND sensor = ?";
						JsonArray paramQuery = new JsonArray()
								.add(user).add(sensor);
						
						connection.queryWithParams(
								query, 
								paramQuery, 
								res -> {
									if (res.succeeded()) {
										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
									}else {
										routingContext.response().setStatusCode(400).end(
												"Error: " + res.cause());	
									}
								});
					}else {
						routingContext.response().setStatusCode(400).end(
								"Error: " + conn.cause());
					}
				});
				
				
				
				//routingContext.response().setStatusCode(200).
				//	end(Json.encodePrettily(database.get(param)));
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	private void getOrdByUser(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("userFilter");
		if (paramStr != null) {
			try {
				String param = paramStr;
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT * "
								+ "FROM registro_user "
								+ "WHERE user = ?";
						JsonArray paramQuery = new JsonArray()
								.add(param);
						connection.queryWithParams(
								query, 
								paramQuery, 
								res -> {
									if (res.succeeded()) {
										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
									}else {
										routingContext.response().setStatusCode(400).end(
												"Error: " + res.cause());	
									}
								});
					}else {
						routingContext.response().setStatusCode(400).end(
								"Error: " + conn.cause());
					}
				});
				
				
				
				//routingContext.response().setStatusCode(200).
				//	end(Json.encodePrettily(database.get(param)));
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	private void getOrdByUserSensor(RoutingContext routingContext) {
		String paramStr1 = routingContext.request().getParam("userFilter");
		String paramStr2 = routingContext.request().getParam("idSensor");
		if (paramStr1 != null && paramStr2 != null) {
			try {
				String user = paramStr1;
				String sensor = paramStr2;
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT * "
								+ "FROM registro_user "
								+ "WHERE user = ? AND sensor = ?";
						JsonArray paramQuery = new JsonArray()
								.add(user).add(sensor);
						connection.queryWithParams(
								query, 
								paramQuery, 
								res -> {
									if (res.succeeded()) {
										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
									}else {
										routingContext.response().setStatusCode(400).end(
												"Error: " + res.cause());	
									}
								});
					}else {
						routingContext.response().setStatusCode(400).end(
								"Error: " + conn.cause());
					}
				});
				
				
				
				//routingContext.response().setStatusCode(200).
				//	end(Json.encodePrettily(database.get(param)));
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}

	private void getAllParams(RoutingContext routingContext) {
		try {
			
			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT * "
							+ "FROM params ";
					connection.query(query, 
							res -> {
								if (res.succeeded()) {
									routingContext.response().end(Json.encodePrettily(res.result().getRows()));
								}else {
									routingContext.response().setStatusCode(400).end(
											"Error: " + res.cause());	
								}
							});
				}else {
					routingContext.response().setStatusCode(400).end(
							"Error: " + conn.cause());
				}
			});
			
			
			
			//routingContext.response().setStatusCode(200).
			//	end(Json.encodePrettily(database.get(param)));
		}catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}
//		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
//				.end(Json.encode(database.values()));
	}
	
	private void mideTemperatura(RoutingContext routingContext) {
		String user = routingContext.request().getParam("userFilter");
		String sensor = routingContext.request().getParam("idSensor");
		if (user != null && sensor != null) {
			
			JsonObject peticion = new JsonObject();
			peticion.put("tipo", "mideTemperatura");
			//peticion.put("id", "");
			peticion.put("temp", 0);
			peticion.put("user", user);
			//peticion.put("fechaHora", 0);
			peticion.put("sensor", sensor);
			
//			JsonArray peticion = new JsonArray()
//					.add(tipo)
//					.add(nTemp).add(user)
//					.add(time).add(sensor);
			
			mqttClient.publish("topic_2", Buffer.buffer(Json.encode(peticion)), MqttQoS.AT_LEAST_ONCE, false, false);
//			try {
//				
//				mySQLClient.getConnection(conn -> {
//					if (conn.succeeded()) {
//						SQLConnection connection = conn.result();
//						String query = "SELECT * "
//								+ "FROM registro_amb "
//								+ "WHERE user = ? AND sensor = ?";
//						JsonArray paramQuery = new JsonArray()
//								.add(user).add(sensor);
//						
//						connection.queryWithParams(
//								query, 
//								paramQuery, 
//								res -> {
//									if (res.succeeded()) {
//										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
//									}else {
//										routingContext.response().setStatusCode(400).end(
//												"Error: " + res.cause());	
//									}
//								});
//					}else {
//						routingContext.response().setStatusCode(400).end(
//								"Error: " + conn.cause());
//					}
//				});
//				
//				
//				
//				//routingContext.response().setStatusCode(200).
//				//	end(Json.encodePrettily(database.get(param)));
//			}catch (ClassCastException e) {
//				routingContext.response().setStatusCode(400).end();
//			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	private void getAllRegistroCambios(RoutingContext routingContext) {
		try {
			
			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					String query = "SELECT * "
							+ "FROM registro_user ";
					connection.query(query, 
							res -> {
								if (res.succeeded()) {
									routingContext.response().end(Json.encodePrettily(res.result().getRows()));
								}else {
									routingContext.response().setStatusCode(400).end(
											"Error: " + res.cause());	
								}
							});
				}else {
					routingContext.response().setStatusCode(400).end(
							"Error: " + conn.cause());
				}
			});
			
			
			
			//routingContext.response().setStatusCode(200).
			//	end(Json.encodePrettily(database.get(param)));
		}catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}
//		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
//				.end(Json.encode(database.values()));
	}
	
	private void powerOn(RoutingContext routingContext) {
		
		PowerIss orden = Json.decodeValue(routingContext.getBody(), PowerIss.class);
		String user = orden.getUser();
		String sensor = orden.getSensor();
		
		long time = Calendar.getInstance().getTimeInMillis();
		
		JsonObject peticion = new JsonObject();
		peticion.put("tipo", "enciende");
		peticion.put("temp", 0);
		peticion.put("user", user);
		peticion.put("sensor", sensor);
		
		mqttClient.publish("topic_2", Buffer.buffer(Json.encode(peticion)), MqttQoS.AT_LEAST_ONCE, false, false);
			
			try {
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO registro_sensor(id,user,sensor,power,fechaHora)"
								+ "VALUES ('',?,?,1,?) ";
						JsonArray paramQuery = new JsonArray()
								.add(user).add(sensor).add(time);
						connection.queryWithParams(query,
								paramQuery,
								res -> {
									if (res.succeeded()) {
										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
									}else {
										routingContext.response().setStatusCode(400).end(
												"Error: " + res.cause());	
									}
								});
					}else {
						routingContext.response().setStatusCode(400).end(
								"Error: " + conn.cause());
					}
				});
				
				
				
				//routingContext.response().setStatusCode(200).
				//	end(Json.encodePrettily(database.get(param)));
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
			
	//		DomoState state = Json.decodeValue(routingContext.getBodyAsString(), DomoState.class);
	//		database.put(state.getId(), state);
	//		routingContext.response().setStatusCode(201).end(Json.encode(state));
	}
	
	private void powerOff(RoutingContext routingContext) {
		
		PowerIss orden = Json.decodeValue(routingContext.getBody(), PowerIss.class);
		String user = orden.getUser();
		String sensor = orden.getSensor();
		
		long time = Calendar.getInstance().getTimeInMillis();
		
		JsonObject peticion = new JsonObject();
		peticion.put("tipo", "apaga");
		peticion.put("temp", 0);
		peticion.put("user", user);
		peticion.put("sensor", sensor);
		
		mqttClient.publish("topic_2", Buffer.buffer(Json.encode(peticion)), MqttQoS.AT_LEAST_ONCE, false, false);
		
			try {

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO registro_sensor(id,user,sensor,power,fechaHora)"
								+ "VALUES ('',?,?,0,?) ";
						JsonArray paramQuery = new JsonArray()
								.add(user).add(sensor).add(time);
						connection.queryWithParams(query,
								paramQuery,
								res -> {
									if (res.succeeded()) {
										routingContext.response().end(Json.encodePrettily(res.result().getRows()));
									}else {
										routingContext.response().setStatusCode(400).end(
												"Error: " + res.cause());	
									}
								});
					}else {
						routingContext.response().setStatusCode(400).end(
								"Error: " + conn.cause());
					}
				});
				
				
				
				//routingContext.response().setStatusCode(200).
				//	end(Json.encodePrettily(database.get(param)));
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
			
	//		DomoState state = Json.decodeValue(routingContext.getBodyAsString(), DomoState.class);
	//		database.put(state.getId(), state);
	//		routingContext.response().setStatusCode(201).end(Json.encode(state));
		}
	
	
	}
