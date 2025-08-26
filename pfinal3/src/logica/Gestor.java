package logica;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Gestor {

	static int numproc;
	private static Scanner sc = new Scanner(System.in);
	static ConcurrentHashMap<Integer, String> servicios = new ConcurrentHashMap<>();
	static ArrayList<Integer> idprocesos = new ArrayList<Integer>();

	static Client client = ClientBuilder.newClient();

	static String menu = "\n\nEsto es todo lo que puedes hacer con los hilos:\n \t1 --> Arrancar un proceso\n \t2 --> Parar un proceso\n "
			+ "\t3 --> Comprobar estado de los procesos\n \t4 --> Comprobar coordinador\n "
			+ "\t5 --> Arrancar todos los procesos\n" + "\t6--> Parar todos los procesos\n"
			+ "\t5 --> Inicializar\n\th --> Menu de ayuda\n \tq --> Salir";

	static String menuAyuda = "==================================================="
			+ "\n|     Has entrado en el menu de ayuda al usuario  |\n"
			+ "===================================================" + "\nLa opcion 1 arranca el proceso que pidas"
			+ "\nLa opcion 2 para el proceso seleccionado" + "\nLa opcion 3 comprueba el estado de cada proceso"
			+ "\nLa opcion4 te dice el coordinador de los procesos" + "\nLa opcion 5 te arranca todos los procesos"
			+ "\n La opcion 6 para todos los procesos" + "\nLa opcion h despliega este menu.\n\n"
			+ "\nLa opcion q te saca de la app.\n\n";

	private static void runMenu() {
		/* Metodo que enseña el menu para accionar los hilos */
		String opcion = null;
		Integer proceso;
		boolean salir = false;
		do {
			System.out.println(menu);
			System.out.print("Opcion: ");
			opcion = sc.next().toLowerCase();
			switch (opcion) {

			case "1":
				// Arrancar un proceso
				System.out.printf("Has seleccionado arrancar un proceso, dime cual quieres arrancar \n");
				proceso = sc.nextInt();

				arrancar(proceso);

				break;

			case "2":
				// Parar un proceso
				System.out.printf("Has seleccionado parar un proceso, dime cual quieres parar \n");
				proceso = sc.nextInt();

				parar(proceso);

				break;

			case "3":
				// Comprobar estado de los procesos (Elección Activa, Pasiva...)
				System.out.printf("Has seleccionado comprobar estado de los procesos, dime cual quieres comprobar \n");
				for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
					Integer procesos = entry.getKey();
					String ipcomprobar = servicios.get(procesos);
					URI uri = UriBuilder.fromUri("http://" + ipcomprobar + ":8088/pfinal3").build();
					WebTarget target = client.target(uri);

					System.out.println(target.path("servicio").path("consultarestado").queryParam("id", "" + procesos)
							.request(MediaType.TEXT_PLAIN).get(String.class));
				}
				break;

			case "4":

				// Comprobar quien es el coordinador
				System.out.printf(
						"Has seleccionado comprobar el coordinador de los procesos, dime cual quieres comprobar \n");

				for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
					Integer procesos = entry.getKey();
					comprobarcoord(procesos);
				}
				break;

			case "5":
				// Arrancar todos los procesos
				System.out.printf("Has seleccionado arrancar todos los procesos \n");
				for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
					Integer procesos = entry.getKey();
					arrancar(procesos);
				}
				break;

			case "6":
				// Parar todos los procesos
				System.out.printf("Has seleccionado parar todos los procesos \n");
				for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
					Integer procesos = entry.getKey();
					parar(procesos);
				}
				break;

			case "h":
				System.out.println(menuAyuda);
				break;

			case "q":
				salir = true;
				break;
			default:
				System.out.println("\nOPCIÓN INCORRECTA: Pruebe de nuevo\n");
				break;
			}
		} while (!salir);
	}

	public static void comprobarcoord(Integer proceso) {
		String ipcoord = servicios.get(proceso);
		URI uri = UriBuilder.fromUri("http://" + ipcoord + ":8088/pfinal3").build();
		WebTarget target = client.target(uri);

		System.out.println(target.path("servicio").path("getcoordinador").queryParam("id", "" + proceso)
				.request(MediaType.TEXT_PLAIN).get(String.class));
	}

	public static void arrancar(Integer proceso) {
		String iparrancar = servicios.get(proceso);

		URI uri = UriBuilder.fromUri("http://" + iparrancar + ":8088/pfinal3").build();
		WebTarget target = client.target(uri);

		System.out.println(target.path("servicio").path("arrancar").queryParam("id", "" + proceso)
				.request(MediaType.TEXT_PLAIN).get(String.class));

	}

	public static void parar(Integer proceso) {

		String ipparar = servicios.get(proceso);

		URI uri = UriBuilder.fromUri("http://" + ipparar + ":8088/pfinal3").build();
		WebTarget target = client.target(uri);

		System.out.println(target.path("servicio").path("parar").queryParam("id", "" + proceso)
				.request(MediaType.TEXT_PLAIN).get(String.class));

	}

	public static void enviarcontacto(String ip) {

		try {

			ObjectMapper mapper = new ObjectMapper();
			String jsonServicios = mapper.writeValueAsString(servicios);
			String servicioscodificados = null;

			try {
				servicioscodificados = URLEncoder.encode(jsonServicios, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			URI uri = UriBuilder.fromUri("http://" + ip + ":8088/pfinal3").build();
			WebTarget target = client.target(uri);

			System.out.println(target.path("servicio").path("actualizarServicios")
					.queryParam("servicios", "" + servicioscodificados).request(MediaType.TEXT_PLAIN)
					.get(String.class));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void enviarid(String ip) {

		URI uri = UriBuilder.fromUri("http://" + ip + ":8088/pfinal3").build();
		WebTarget target = client.target(uri);
		String idsString = idprocesos.stream().map(String::valueOf) // Convierte cada Integer en String
				.collect(Collectors.joining(","));

		System.out.println(target.path("servicio").path("actualizarid").queryParam("ids", "" + idsString)
				.request(MediaType.TEXT_PLAIN).get(String.class));

	}

	public static void main(String[] args) {

		// Solicita nº maquinas, ip's e id's de proceso

		int n, numprocesos;
		Integer id;

		do {
			System.out.println("Introduce el número de máquinas:");
			n = sc.nextInt();
			sc.nextLine();
			if (n < 0) {
				System.out.println("\nINTRODUZCA UN NUMERO VALIDO\n");
			}
		} while (n < 0);

		for (int i = 0; i < n; i++) {
			System.out.println("Introduce la IP de la máquina " + (i + 1) + ":");
			String ip = sc.next();
			do {
				System.out.println("Introduce cuántos procesos hay en esa máquina:");
				numprocesos = sc.nextInt();
				sc.nextLine();
				if (n < 0) {
					System.out.println("\n INTRODUZCA UN NUMERO CORRECTO\n");
				}
			} while (n < 0);
			for (int j = 0; j < numprocesos; j++) {
				do {
					System.out.println("Introduce el id del proceso " + (j + 1) + " en esa máquina:");
					id = sc.nextInt();

				} while (n < 0);
				idprocesos.add(id);
				servicios.put(id, ip);
				numproc++;
			}
		}

		// Crea el mapa con los pares ID-IP y los envía a cada proceso

		for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
			String ip = entry.getValue();

			// Enviar mapa a esta IP
			enviarcontacto(ip);
			// Enviar vector de id's
			enviarid(ip);
		}

		// Inicializa los procesos del mapa en su servicio(IP-Nodo)

		for (Map.Entry<Integer, String> entry : servicios.entrySet()) {
			String ipcrear = entry.getValue();
			URI uri = UriBuilder.fromUri("http://" + ipcrear + ":8088/pfinal3").build();
			WebTarget target = client.target(uri);
			// Llamada al servicio pasando id del proceso a iniciar
			System.out.println(target.path("servicio").path("servicio").queryParam("id", "" + entry.getKey())
					.request(MediaType.TEXT_PLAIN).get(String.class));
		}

		// Arranca el menú una vez creados
		runMenu();
	}

}