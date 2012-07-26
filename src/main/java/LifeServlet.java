import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LifeServlet extends HttpServlet {
	// lock object
	private final Object lock = new Object();

	private int serviceCounter = 0; // кол-во одновременных подключений
	int countRequest = 0; // количество обращений
	private FileOutputStream out; // поток для файла
	private File fileCount;

	private boolean shuttingDown; // для корректного завершения

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		String outputFile = servletConfig.getInitParameter("outputfile");
		String outputCount = servletConfig.getInitParameter("outputcount");
		String homeDir = getServletContext().getRealPath("/");
		File file = new File(homeDir, outputFile);
		fileCount = new File(homeDir, outputCount);
		try {
			out = new FileOutputStream(file);
			writeToFile("method init");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void service(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ServletException,
			IOException {
		enteringServiceMethod();
		try {
			super.service(httpServletRequest, httpServletResponse); // родительский
																	// вызывает
																	// дугет
		} finally {
			removeServiceMethod();
		}
	}

	@Override
	protected void doGet(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ServletException,
			IOException {
		if (!shuttingDown) {
			writeToFile("method doGet client " + serviceCounter + " starts");
			// some long running operation
			PrintWriter out = httpServletResponse.getWriter();
			try {
				out.println("hi: amount current connections: " + serviceCounter);
				out.println("amount all requests: " + countRequest);
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			writeToFile("method doGet client " + serviceCounter + " ends");
		}
	}

	@Override
	public void destroy() {
		synchronized (lock) {
			shuttingDown = getNumServices() > 0; // кол-во запущенных сервисов
			writeToFile("trying to destroy");
			while (getNumServices() > 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		writeToFile("destroyed");

		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getServletInfo() {
		return "This servlet was created for life period test";
	}

	private int getNumServices() {
		synchronized (lock) {
			return serviceCounter;
		}
	}

	private void enteringServiceMethod() {
		synchronized (lock) {
			serviceCounter++;
			writeToFile("method enteringServiceMethod serviceCounter =  "
					+ serviceCounter);
			writeCount();
		}
	}

	private void removeServiceMethod() {
		synchronized (lock) {
			serviceCounter--;
			if (serviceCounter == 0 && shuttingDown) {
				notifyAll();// прекращает ожидание ваит
			}
		}

		writeToFile("method removeServiceMethod serviceCounter = "
				+ serviceCounter);
	}

	private void writeToFile(String text) {
		System.out.println(text);
		text += "\r\n";

		try {
			out.write(text.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeCount() {

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(fileCount));
			String s;
			if ((s = br.readLine()) != null) {
				countRequest = Integer.parseInt(s);
			}
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("error close file");
				}
			}
		}

		countRequest++;

		PrintWriter bw = null;
		try {
			bw = new PrintWriter(fileCount);
			bw.println(countRequest);
			bw.flush();
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}
	}
}