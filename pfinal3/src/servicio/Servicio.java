package servicio;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import logica.Proceso;
import logica.Proceso.estadoElec;

//creaa los procesos
@Path("servicio") // ruta a la clase
@Singleton
public class Servicio {

	static ConcurrentHashMap<Integer, String> listaservicios = new ConcurrentHashMap<>();
	public ArrayList<Proceso> procesos = new ArrayList<>();
	public String estado;
	public Proceso pglobal = null;
	public ArrayList<Integer> idprocesos = new ArrayList<Integer>();
	public Client client = ClientBuilder.newClient();
	public InetAddress inetaddress;

	// Inicializa los servicios sin arrancarlos y los añade a la lista de procesos
	// del nodo
	@GET
	@Path("servicio")
	@Produces(MediaType.TEXT_PLAIN)
	public String servicio(@QueryParam("id") int id) {
		pglobal = new Proceso(id, idprocesos);
		procesos.add(pglobal);
		return "Se ha arrancado el servicio";

	}

	// Busca la id del proceso en el nodo
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("arrancar")

	public String arrancar(@QueryParam("id") int id) {
		System.out.println("Intentando arrancar  proceso: " + id);
		Proceso p = buscarporid(id);

		if (p == null) { // Si ya se ha ejecutado servicio nunca va a ser null aunque la busqueda falle,
							// solucion, variable local de metodos siempre null al inicial
			return "no hay proceso con ese id";
		}
		// Si encuentra el proceso con esa id lo arranca y lo inicia
		p.arrancar();

		return "El proceso con id:" + id + " ha sido arrancado \n";

	}

	// Detiene el proceso con id dada
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("parar")

	public String parar(@QueryParam("id") int id) {
		// Busca el proceso con esa id
		Proceso p = buscarporid(id);

		if (p == null) { // MISMO ERROR NULL
			return "no hay proceso con ese id";
		}
		// Una vez encontrado el proceso se detiene
		p.parar();
		procesos.remove(p);

		URI uri = UriBuilder.fromUri("http://localhost:8088/pfinal3").build();
		WebTarget target = client.target(uri);
		// Llamada al servicio pasando id del proceso a iniciar
		System.out.println(target.path("servicio").path("servicio").queryParam("id", "" + id)
				.request(MediaType.TEXT_PLAIN).get(String.class));

		return "El proceso con id:" + id + " ha sido parado \n";

	}

	// Devuelve el estado (arrancado, parado...) de un proceso con id dada
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("consultarestado")

	public String consultarestado(@QueryParam("id") int id) {
		// Recorre la lista de procesos para buscar el deseado con su id
		for (Proceso p : procesos) {
			if (p.getID() == id) {
				// Proceso encontrado
				// Se consulta el estado del proceso p

				// FORMA DE IMPRIMIR POR PANTALLA????????????????????????????
				String estado = (p.consultaestado() == 1) ? String.format("El proceso con id: %d está arrancado", id)
						: String.format("El proceso con id: %d está parado", id);

				switch (p.consultarestadoelecc()) { // consultarestadoelecc similar a get pero filtrando info
				case 0:
					estado += " y en estado de acuerdo,"; // Estado de acuerdo
					break;
				case 1:
					estado += " y en elección activa,"; // Elección activa
					break;
				case 2:
					estado += " y en elección pasiva,"; // Elección pasiva
					break;
				}
				
				estado+="y el coordinador es" + p.getCoordinador();

				return estado;
			}
		}
		// Proceso no encontrado
		return String.format("No se encontró un proceso con id: %d", id);
	}

	// Convierte lista de servicios en Hashmap y lo miestra por pantalla.
	@GET
	@Path("actualizarServicios")
	@Produces(MediaType.TEXT_PLAIN)
	public String actualizarServicios(@QueryParam("servicios") String jsonServicios) {

		ObjectMapper mapper = new ObjectMapper();

		try {
			listaservicios = mapper.readValue(jsonServicios, ConcurrentHashMap.class); // Convierta la lista de
																						// servicios en un hashmap
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) { // Exceptciones de la conversión
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Error al actualizar servicios\n";
		}

		for (Map.Entry<Integer, String> entry : listaservicios.entrySet()) {
			System.out.println("Clave: " + entry.getKey() + " -> Valor: " + entry.getValue()); // Imrpime contenido de
																								// lista servicios
		}

		return "La lista se ha actualizado correctamente";

	}

	// Actualiza con nueva info la lista de id's del nodo
	@GET
	@Path("actualizarid")
	@Produces(MediaType.TEXT_PLAIN)
	public String actualizarid(@QueryParam("ids") String ids) {
		// Convierte el parámetro en lista de id's (array de strings)
		String[] idsArray = ids.split(",");
		ArrayList<Integer> nuevosIds = new ArrayList<>();

		for (String idStr : idsArray) {
			nuevosIds.add(Integer.parseInt(idStr)); // Almacena los id's en el arraylist
		}

		for (int id : nuevosIds) {
			if (!idprocesos.contains(id)) {
				idprocesos.add(id);// Añade el array list a la lista general de id's
			}
		}

		return "id actualizados\n";

	}

	// Propagar el ok a cada nodo o al proceso en caso de que se encuentre en el
	// nodo actual

	// Hace computar al proceso coordinador
	// Si el coordinador es localhost ejecuta computar si no http al coordinador
	// remoto para que ejecute computar
	@GET
	@Path("llamarcoordinador")
	@Produces(MediaType.TEXT_PLAIN)
	public String llamarcoordinador(@QueryParam("coordinador") int coordinador) {

		String coordinadorGet = String.valueOf(coordinador);
		String respuesta = null;
		String ipcomputar = listaservicios.get(coordinadorGet); // ERROR IP COMPUTAR ES NULL
		// En otro nodo
		URI uri = UriBuilder.fromUri("http://" + ipcomputar + ":8088/pfinal3").build();
		WebTarget target = client.target(uri);
		// Llama al servicio computar del nodo donde está el coordinador
		respuesta = target.path("servicio").path("recibircoordinador").queryParam("coordinador", "" + coordinador)
				.request(MediaType.TEXT_PLAIN).get(String.class);

		return respuesta;

	}

	@GET
	@Path("recibircoordinador")
	@Produces(MediaType.TEXT_PLAIN)
	public String recibircoordinador(@QueryParam("coordinador") int coordinador) {

		Proceso p = buscarporid(coordinador);
		String respuesta = String.valueOf(p.computar());

		return respuesta;

	}

	// Propagar la eleccion o mandarla a los procesos que estan en nuestra maquina
	// accediendo a sus metodos directamente

	@GET
	@Path("enviarmensaje")
	@Produces(MediaType.TEXT_PLAIN)
	public void enviarmensaje(@QueryParam("origen") int origen, @QueryParam("destino") int destino,
			@QueryParam("tipo") String tipo) {
		System.out.println("han enviado mensaje: " + origen + destino + tipo);

		String idGet = String.valueOf(destino);
		String ipreenvio = listaservicios.get(idGet);

		URI uri = UriBuilder.fromUri("http://" + ipreenvio + ":8088/pfinal3").build();
		WebTarget target = client.target(uri);
		System.out.println(target.path("servicio").path("recibirmensaje").queryParam("origen", "" + origen)
				.queryParam("destino", destino).queryParam("tipo", tipo).request(MediaType.TEXT_PLAIN)
				.get(String.class));

	}

	@GET
	@Path("getcoordinador")
	@Produces(MediaType.TEXT_PLAIN)
	public String getcoordinador(@QueryParam("id") int id) {

		Proceso p = buscarporid(id);
		return String.valueOf(p.getCoordinador());

	}

	@GET
	@Path("recibirmensaje")
	@Produces(MediaType.TEXT_PLAIN)
	public void recibirmensaje(@QueryParam("origen") int origen, @QueryParam("destino") int destino,
			@QueryParam("tipo") String tipo) {
		System.out.println("he recibido mensaje: " + origen + destino + tipo);

		if (tipo.equals("eleccion")) {
			Proceso p = buscarporid(destino);
			if (p.getID() == destino) {
				if (p.consultaestado() == 1) {
					try {
						if (p.estadoactual ==estadoElec.ACUERDO) {
							p.convocareleccion(origen);
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if (tipo.equals("ok")) {

			for (Proceso p : procesos) {
				int id = p.getID();
				if (id == destino) {
					if (p.consultaestado() == 1) {
						p.setMensaje(origen, destino, tipo);
						synchronized (p.testigook) {
							if(p.estadoactual !=estadoElec.ACUERDO) {
								p.estadoactual = estadoElec.ELECCION_PASIVA;
							}
							p.testigook.notify();
						
						}
					}
				}

			}

		}

		if (tipo.equals("coordinador")) {

			for (Proceso p : procesos) {
				int id = p.getID();
				if (id == destino) {
					if (p.consultaestado() == 1) {
						p.setCoordinador(origen);
						p.estadoactual = estadoElec.ACUERDO;
						p.eleccionterminada=true;
						synchronized (p.testigocoord) {
							p.testigocoord.notify();

						}

					}
				}
			}

		}
	}

	// Método NO servicio que busca un proceso por su id en la lista de procesos
	// "procesos"
	public Proceso buscarporid(int id) {

		for (Proceso p1 : procesos) {
			if (p1.getID() == id) {
				return p1;
			}
		}
		return null;
	}

	public Proceso actualizarid(int id) {

		for (Proceso p1 : procesos) {
			if (p1.getID() == id) {
				return p1;
			}
		}
		return null;
	}

}