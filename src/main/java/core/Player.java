package core;

import java.io.Serializable;

import state_machine.core.FiniteStateMachine;
import state_machines.StateMachines;
import utils.CaptionGenerator;

public class Player implements Serializable {
	private static final long serialVersionUID = 1L;
	private long userId = 0l;
	private String name;
	private String background;
	private String image;
	private FiniteStateMachine imageSetted;
	private FiniteStateMachine nameSetted;
	private FiniteStateMachine backgroundSetted;
	private FiniteStateMachine allLinesSetted;
	private FiniteStateMachine singeLineSetted;
	private String imgbbUrl;
	private CaptionGenerator cg = new CaptionGenerator();;

	public Player(long userId) {
		this.userId = userId;
		name = "";
		background = "";
		image = "";
		imageSetted = StateMachines.imageSetMachineBuilder();
		nameSetted = StateMachines.imageSetMachineBuilder();
		backgroundSetted = StateMachines.imageSetMachineBuilder();
		allLinesSetted = StateMachines.imageSetMachineBuilder();
		singeLineSetted = StateMachines.imageSetMachineBuilder();
		cg = new CaptionGenerator();
	}

	public Player() {
		userId = 0l;
		name = "";
		background = "";
		image = "";
		imageSetted = StateMachines.imageSetMachineBuilder();
		nameSetted = StateMachines.imageSetMachineBuilder();
		backgroundSetted = StateMachines.imageSetMachineBuilder();
		allLinesSetted = StateMachines.imageSetMachineBuilder();
		singeLineSetted = StateMachines.imageSetMachineBuilder();
		cg = new CaptionGenerator();
	}

	public long userId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getBackground() {
		return background;
	}

	public String getName() {
		return name;
	}

	public void setBackground(String background) {
		this.background = background;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImgbbUrl() {
		return imgbbUrl;
	}

	public void setImgbbUrl(String url) {
		this.imgbbUrl = url;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public FiniteStateMachine getImageStateMachine() {
		return imageSetted;
	}

	public FiniteStateMachine getNameStateMachine() {
		return nameSetted;
	}

	public FiniteStateMachine getBackgroundStateMachine() {
		return backgroundSetted;
	}

	public FiniteStateMachine allLinesStateMachine() {
		return allLinesSetted;
	}

	public FiniteStateMachine singeLineStateMachine() {
		return singeLineSetted;
	}

	public void setCaptionGenerator(CaptionGenerator cg) {
		this.cg = cg;
	}

	public CaptionGenerator getCaptionGenerator() {
		return cg;
	}

	public boolean equals(Object o) {
		if (!(o instanceof Player || o == null))
			return false;
		Player player = (Player) o;
		if (player.userId == this.userId)
			return true;
		return false;

	}

	@Override
	public String toString() {
		return "Player [userId=" + userId + "]";
	}

}
