package logica;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class Proceso extends Thread {

	int id; // Id del proceso
	Integer coordinador; // Id del coordinador del grupo
	boolean estado; // Estado del proceso (Arrancado/Parado)

	public Object testigook = new Object();
	public Object testigocoord = new Object();

	boolean eleccionencurso = false;
	public boolean eleccionterminada = false;
	public int eleccionactual=0;
	public int viewnumber;


	List<String[]> mensajesPendientes = new ArrayList<>();

	public enum estadoElec {
		ACUERDO, ELECCION_ACTIVA, ELECCION_PASIVA
	} // Estado de LA ELECCIÓN proceso

	public estadoElec estadoactual; // estado de la eleccion
	public Client client = ClientBuilder.newClient();
	URI uri = UriBuilder.fromUri("http://localhost:8088/pfinal3").build(); // Todas las llamadas son localhost porque
																			// solo puede acceder a su servicio
	WebTarget target = client.target(uri);

	public ArrayList<Integer> procesos = new ArrayList<Integer>();
	public int idcoordinadorresp; // almacena la id del coordinador cuando le llega una respuesta coordinador

	// Variables que nos indican si hemos recibido respuesta
	public boolean algunOK = false;

	List<String> arrayRespuestas = new CopyOnWriteArrayList<String>();

	// CONSTRUCTOR de la clase Proceso
	public Proceso(int id, ArrayList<Integer> idprocesos) {
		this.id = id;
		this.coordinador = -2;
		this.estado = false;
		this.estadoactual = estadoElec.ACUERDO;
		this.procesos.addAll(idprocesos);

	}

	// Devuelce id del proceso
	public int getID() {
		return this.id;
	}

	public void enviarREST(int origen, int destino, String contenido) {

		System.out.println(target.path("servicio").path("enviarmensaje").queryParam("origen", "" + origen)
				.queryParam("destino", destino).queryParam("tipo", contenido).request(MediaType.TEXT_PLAIN)
				.get(String.class));

	}

	public void setCoordinador(int coordinador) {
		this.coordinador = coordinador;
	}

	public int getCoordinador() {
		return this.coordinador;
	}

	public void setMensaje(int origen, int destino, String tipo) {

		String[] mensaje = new String[] { String.valueOf(origen), String.valueOf(destino), tipo };
		mensajesPendientes.add(mensaje);

	}

	public void seteleccionencurso() {

		this.eleccionencurso = false;

	}

	public void convocareleccion(int idqueconvoca) throws InterruptedException {
		
		eleccionactual++;
		
		

		eleccionterminada = false;
		algunOK = false;
		this.estadoactual = estadoElec.ELECCION_ACTIVA;
			
		

		if (this.estado == true) {

			if (this.id > idqueconvoca ) {	//Solo enviar cuando eleccion activa
				

				enviarREST(this.id, idqueconvoca, "ok");
			}
			while (!eleccionterminada) {

				// comprobar que no soy el de id mas alto

				if (buscarMayor() == this.id) {
					this.coordinador = this.id;
					this.estadoactual = estadoElec.ACUERDO;
					for (int i : procesos) {
						if (i != this.id) {
							
							enviarREST(this.id, i, "coordinador");
						}
					}
					eleccionterminada = true;
				}

				else {
						System.out.println("Voy a hacer eleccion soy:" + this.id);
						for (int i : procesos) {
							if (this.id < i) {
								if (i != this.id) {
									enviarREST(this.id, i, "eleccion");
								}

							}
						}
					

						synchronized (testigook) {
							testigook.wait(1000);
							if (this.estadoactual == estadoElec.ELECCION_PASIVA) {
								synchronized (testigocoord) {
									testigocoord.wait(1000);
									// Buscar entre mis mensajes un COORDINADOR. Si lo hay, actualizamos
									// el coordinador y finalizamos las elecciones:
									eleccionterminada = true;
								}

							}

							else if (this.estadoactual == estadoElec.ELECCION_ACTIVA) {
								this.coordinador = this.id;
								estadoactual = estadoElec.ACUERDO;
								eleccionterminada = true;
								for (int i : procesos) {
									if (i != this.id) {
										System.out.println("mando coordinador: " + this.id);
										enviarREST(this.id, i, "coordinador");

									}
								}

							}
						}
					}

				}

				// Si no hemos recibido mensaje, el proceso asume que es el mayor
				// activo, así que se pone como coordinador a sí mismo, manda un mensaje a
				// todos los demás de tipo "COORDINADOR" y termina las elecciones

				// Si sí hemos recibido el OK, esperamos un mensaje COORDINADOR:
			}
		}
		


	public int consultaestado() {
		if (estado) {
			return 1;
		}
		return 0;
	}

	public int consultarestadoelecc() {

		if (estadoactual == estadoElec.ACUERDO) {
			return 0;
		} else if (estadoactual == estadoElec.ELECCION_ACTIVA) {
			return 1;
		} else if (estadoactual == estadoElec.ELECCION_PASIVA) {
			return 2;
		}
		return -1;

	}

	// Cambia la variable de proceso estado false a true
	public void arrancar() {

		if (estado) {
			System.out.printf("El proceso con id= %d ya esta arrancado \n", this.id);

		} else {
			System.out.printf("Arrancando el proceso con id= %d \n", this.id);
			estado = true;
			this.start();
			System.out.println("Arrancado  proceso: " + this.id);
		}

	}

	public void parar() {

		if (!estado) {
			System.out.printf("El proceso con id= %d ya esta parado \n", this.id);

		} else {

			estado = false;
			System.out.printf("Parando el proceso con id= %d \n", this.id);

		}
	}

	public int computar() {

		if (this.estado == false) {
			return -1;
		} else {
			try {
				Thread.sleep(new Random().nextInt(100) + 300); // Hacemos una espera simulando la computacion
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 1;
		}

	}

	public void run() {

		System.out.println("Hola soy el proceso: " + this.id);

		boolean finalizar = false;

		try {
			System.out.println("intento iniciar eleccion al nacer: " + this.id);
			this.convocareleccion(this.id);

			System.out.println("he hecho la eleccion: " + this.id + this.coordinador);

		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Bucle mientras el proceso esté arrancado (que no creado)
		while (!finalizar) {

			if (this.estado == false) {
				finalizar = true;
			} else {
				try {
					// Pausa de 0.5 a 1s
					Thread.sleep(new Random().nextInt(500) + 500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (this.estadoactual == estadoElec.ACUERDO && this.coordinador>0) {

					// Llamada computar() al nodo del proceso coordinador
					String respuesta = target.path("servicio").path("llamarcoordinador")
							.queryParam("coordinador", "" + this.coordinador).request(MediaType.TEXT_PLAIN)
							.get(String.class);
					// Si el coordinador se ha caido
					if (respuesta.equals("-1")) {
						this.coordinador=-1;
						System.out.println("Hay que hacer eleccion: " + this.id);
						// Iniciamos una nueva eleccion
						try {

							if (this.estadoactual != estadoElec.ELECCION_ACTIVA) {

								this.convocareleccion(this.id);
							}

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}

			}
		}

	}

	public int buscarMayor() {
		int mayor = 0;
		for (int id : procesos) {
			if (id > mayor) {
				mayor = id;
			}
		}

		return mayor;
	}

}