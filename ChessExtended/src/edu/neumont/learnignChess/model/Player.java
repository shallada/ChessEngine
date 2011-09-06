package edu.neumont.learnignChess.model;


public abstract class Player {
	
	protected Team team;
	
	public Player(Team team) {
		this.team = team;
	}
	
	public Team getTeam() {
		return team;
	}
	
	public abstract Move getMove();
	
	public abstract Pawn.IPromotionListener getPromotionListener();
}
