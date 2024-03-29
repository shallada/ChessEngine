package edu.neumont.learningChess.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import edu.neumont.learningChess.api.ExtendedMove;
import edu.neumont.learningChess.api.PieceType;
import edu.neumont.learningChess.controller.GameController;
import edu.neumont.learningChess.json.Jsonizer;
import edu.neumont.learningChess.model.Pawn.IPromotionListener;

public class ServerPlayer extends Player {
	

	private PromotionListener promotionListener = null;
	
	private final String getMoveEndpoint;
	
	public static final boolean IS_LOCAL = false;

	private GameController gameController = null;
	
	public ServerPlayer(Team team, GameController game) {
		super(team);
		this.gameController = game;
		if(IS_LOCAL) {
			getMoveEndpoint = "http://localhost:8080/LearningChessWebServer/getmove";
		} else {
			getMoveEndpoint = "http://chess.neumont.edu:80/ChessGame/getmove";
		}
		promotionListener = new PromotionListener(new Queen());
	}
	
	

	@Override
	public Move getMove() {
		ExtendedMove extendedMoveFromServer = null;
//		MoveDescription mostRecentMoveDescription = gameController.getMostRecentMoveDescription();
		
		try {
			URL url = new URL(getMoveEndpoint);
			URLConnection connection = url.openConnection();
			connection.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			String jsonOut = Jsonizer.jsonize(gameController.getCurrentGameState());
			writer.write(jsonOut);
			writer.flush();
			
			InputStreamReader in = new InputStreamReader(connection.getInputStream());
			StringBuilder jsonStringBuilder = new StringBuilder();
			int bytesRead;
			 while ((bytesRead = in.read()) > -1) {
				 jsonStringBuilder.append((char)bytesRead);
			 }
			 extendedMoveFromServer = Jsonizer.dejsonize(jsonStringBuilder.toString(), ExtendedMove.class);
			 setPromotionPiece(extendedMoveFromServer.getPromotionPieceType());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return extendedMoveFromServer;
	}
	
	private void setPromotionPiece(PieceType pieceType){
		if(pieceType == PieceType.KING || pieceType == PieceType.PAWN)
			throw new RuntimeException("Can't promote to king or pawn");
		ChessPiece piece = new Queen();
		if (pieceType != null) {
			piece = ChessPiece.getChessPieceFromPieceType(pieceType, null, gameController);
		}
		promotionListener.setPromotionPiece(piece);
	}

	@Override
	public IPromotionListener getPromotionListener() {
		return promotionListener;
	}
}
