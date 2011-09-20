import java.awt.GridLayout;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import edu.neumont.learningChess.api.ExtendedMove;
import edu.neumont.learningChess.api.MoveHistory;
import edu.neumont.learningChess.api.ThemeNames;
import edu.neumont.learningChess.controller.GameController;
import edu.neumont.learningChess.controller.GameController.PlayerType;
import edu.neumont.learningChess.controller.GameOverType;
import edu.neumont.learningChess.json.Jsonizer;
import edu.neumont.learningChess.model.ServerPlayer;
import edu.neumont.learningChess.model.TextCommandProcessor;
import edu.neumont.learningChess.model.TextCommandProcessorOutput;
import edu.neumont.learningChess.model.User;

public class Main {

	private static String theme = null;
	private static GameController.PlayerType white = null;
	private static GameController.PlayerType black = null;

	public static void main(String[] args) {
		boolean loggedIn = false;
		while (!loggedIn) {
			int Choice = JOptionPane.showConfirmDialog(null, "Do you have an account?", "Login", JOptionPane.YES_NO_OPTION);
//			System.out.println(Choice);
//			boolean doAgain = true;
//			while (doAgain) {
//				{
					switch (Choice) {
					case 0:
						loggedIn = Login();
//						doAgain = !loggedIn;
						break;
					case 1:
						loggedIn = Register();
//						doAgain = !loggedIn;
						break;
//					default:
//						doAgain = false;
//					}
				}
			
		}
		do {
			ThemeNames[] values = ThemeNames.values();
			String[] themeNames = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				themeNames[i] = values[i].toString();
			}
			JComboBox themeBox = new JComboBox(themeNames);
			JComboBox whiteComboBox = new JComboBox(new Object[] { GameController.PlayerType.Human, GameController.PlayerType.LearningServer, GameController.PlayerType.AI });
			JComboBox blackComboBox = new JComboBox(new Object[] { GameController.PlayerType.Human, GameController.PlayerType.LearningServer, GameController.PlayerType.AI });

			if (white != null && black != null && theme != null) {
				whiteComboBox.setSelectedItem(white);
				blackComboBox.setSelectedItem(black);
				themeBox.setSelectedItem(theme);
			} else {
				blackComboBox.setSelectedIndex(1);
			}

			JPanel comboBoxes = new JPanel();
			comboBoxes.setLayout(new GridLayout(3, 3, 0, 15));
			comboBoxes.add(new JLabel("White:"));
			comboBoxes.add(whiteComboBox);
			comboBoxes.add(new JLabel("Black:"));
			comboBoxes.add(blackComboBox);
			comboBoxes.add(new JLabel("Piece theme:"));
			comboBoxes.add(themeBox);

			JOptionPane.showMessageDialog(null, comboBoxes, "Select Players", JOptionPane.INFORMATION_MESSAGE);
			theme = themeBox.getSelectedItem().toString();
			white = GameController.PlayerType.valueOf(whiteComboBox.getSelectedItem().toString());
			black = GameController.PlayerType.valueOf(blackComboBox.getSelectedItem().toString());

			GameController.setShowBoard(true);
			GameController game = new GameController(white, black, theme);
			GameOverType gameOverType = game.play();
			if (game.isCheckmate() || game.isStalemate()) {
				game.disableClosing();
				PlayerType winnerType = null;
				if (gameOverType == GameOverType.checkmate)
					winnerType = game.getCurrentTeam().isWhite() ? black : white;
				tellTheServer(game.getGameHistory(), white.toString(), black.toString(), winnerType);
				game.close();
			}
		} while (JOptionPane.showConfirmDialog(null, "do you want to play again?", "play again?", JOptionPane.YES_NO_OPTION) == 0);
	}

	private static boolean Register() {
		boolean loggedIn = false;
		JPanel RegisterOptionMenu = new JPanel();
		JTextField userNameField = new JTextField();
		JPasswordField userPasswordField = new JPasswordField();
		JPasswordField userConfirmedPasswordField = new JPasswordField();

		RegisterOptionMenu.add(new JLabel("User Name"));
		RegisterOptionMenu.add(userNameField);
		RegisterOptionMenu.add(new JLabel("Password"));
		RegisterOptionMenu.add(userPasswordField);
		RegisterOptionMenu.add(new JLabel("Password confirm"));
		RegisterOptionMenu.add(userConfirmedPasswordField);
		RegisterOptionMenu.setLayout(new GridLayout(3, 2, 0, 15));
		
		JOptionPane.showMessageDialog(null, RegisterOptionMenu, "Register", JOptionPane.OK_OPTION);
		
		if (!new String((userPasswordField).getPassword()).equals(new String(userConfirmedPasswordField.getPassword()))) {
			JOptionPane.showMessageDialog(null, "Passwords dont match");
			loggedIn = false;
		} else {
			User user = new User();
			user.setPassword(MD5(new String(userPasswordField.getPassword())));
			user.setUsername(userNameField.getText());
			Jsonizer.jsonize(user);
			try {
				URL url = new URL("http://chess.neumont.edu:80/ChessGame/register");
				URLConnection connection = url.openConnection();
				connection.setDoOutput(true);
				OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
				writer.write(Jsonizer.jsonize(user));
				writer.flush();

				InputStreamReader in = new InputStreamReader(connection.getInputStream());
				StringBuilder jsonStringBuilder = new StringBuilder();
				int bytesRead;
				while ((bytesRead = in.read()) > -1) {
					if ((char) bytesRead != '\n' && (char) bytesRead != '\r')
						jsonStringBuilder.append((char) bytesRead);
				}
				loggedIn = Jsonizer.dejsonize(jsonStringBuilder.toString(), User.class) != null;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return loggedIn;
	}

	private static boolean Login() {

		boolean loggedIn = false;

		JPanel LoginOptionMenu = new JPanel();
		JTextField userNameField = new JTextField();
		JPasswordField userPasswordField = new JPasswordField();
		LoginOptionMenu.add(new JLabel("User Name"));
		LoginOptionMenu.add(userNameField);
		LoginOptionMenu.add(new JLabel("Password"));
		LoginOptionMenu.add(userPasswordField);
		LoginOptionMenu.setLayout(new GridLayout(2, 1, 0, 15));
		JOptionPane.showMessageDialog(null, LoginOptionMenu, "Login", JOptionPane.OK_OPTION);

		User user = new User();
		user.setPassword(MD5(new String(userPasswordField.getPassword())));
		user.setUsername((userNameField.getText()));
		Jsonizer.jsonize(user);

		try {
			URL url = new URL("http://chess.neumont.edu:80/ChessGame/login");
			URLConnection openConnection = url.openConnection();
			openConnection.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(openConnection.getOutputStream());
			writer.write(Jsonizer.jsonize(user));
			writer.flush();

			StringBuilder jsonStringBuilder = new StringBuilder();
			InputStreamReader in = new InputStreamReader(openConnection.getInputStream());
			int bytesRead;
			while ((bytesRead = in.read()) > 0) {
				if ((char) bytesRead != '\n' && (char) bytesRead != '\r')
					jsonStringBuilder.append((char) bytesRead);
			}
			loggedIn = Jsonizer.dejsonize(jsonStringBuilder.toString(), boolean.class);
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return loggedIn;
	}

	private static void tellTheServer(Iterator<ExtendedMove> moveHistoryIterator, String whiteName, String blackName, PlayerType winnerType) {
		List<ExtendedMove> moves = new ArrayList<ExtendedMove>();
		while (moveHistoryIterator.hasNext())
			moves.add(moveHistoryIterator.next());

		MoveHistory moveHistory = new MoveHistory(moves);
		moveHistory.setWhitePlayerName(whiteName);
		moveHistory.setBlackPlayerName(blackName);
		moveHistory.setWinnerType(winnerType);
		String endpoint;
		if (ServerPlayer.IS_LOCAL) {
			endpoint = "http://localhost:8080/LearningChessWebServer/analyzehistory";
		} else {
			endpoint = "http://chess.neumont.edu:80/ChessGame/analyzehistory";
		}

		try {
			URL url = new URL(endpoint);
			URLConnection connection = url.openConnection();
			connection.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			String jsonOut = Jsonizer.jsonize(moveHistory);
			writer.write(jsonOut);
			writer.flush();
			int lengthFromClient = moveHistory.getMoves().size();
			InputStreamReader in = new InputStreamReader(connection.getInputStream());
			StringBuilder jsonStringBuilder = new StringBuilder();
			int bytesRead;
			while ((bytesRead = in.read()) > -1) {
				if ((char) bytesRead != '\n' && (char) bytesRead != '\r')
					jsonStringBuilder.append((char) bytesRead);
			}
			int lengthFromServer = 0;
			try {
				String jsonString = jsonStringBuilder.toString();
				lengthFromServer = Integer.parseInt(jsonString);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			if (lengthFromClient != lengthFromServer)
				throw new RuntimeException("Lengths are different!");
			else
				System.out.println("Lengths are the same");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String MD5(String str) {
		MessageDigest md;
		byte[] md5hash = new byte[32];
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes("iso-8859-1"), 0, str.length());
			md5hash = md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return convertToHex(md5hash);

	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static void old_main(String[] args) {

		TextCommandProcessorOutput output = new TextCommandProcessorOutput(System.out);
		TextCommandProcessor processor = new TextCommandProcessor();
		try {
			processor.processCommands(System.in, output);
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
	}
}
