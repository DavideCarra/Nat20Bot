package state_machines;

import state_machine.core.Event;
import state_machine.core.FiniteStateMachine;
import state_machine.core.State;
import state_machine.core.Transition;

public class StateMachines {

	public static FiniteStateMachine imageSetMachineBuilder() {
		State setted = new State("setted");
		State setting = new State("setting");
		State not_setted = new State("not_setted");
		Transition inSetting = new Transition(not_setted, setting, new Event("settingEvent"));
		Transition set = new Transition(setting, setted, new Event("setEvent"));
		Transition reset = new Transition(setted, not_setted, new Event("resetEvent"));
		FiniteStateMachine  imageSetMachine = new FiniteStateMachine(not_setted, setting, setted);
		imageSetMachine.addTransition(inSetting);
		imageSetMachine.addTransition(set);
		imageSetMachine.addTransition(reset);
		return  imageSetMachine;
	}

}
