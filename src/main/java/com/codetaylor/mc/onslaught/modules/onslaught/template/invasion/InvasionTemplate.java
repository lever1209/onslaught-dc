package com.codetaylor.mc.onslaught.modules.onslaught.template.invasion;

/** Responsible for holding invasion template data read from json files. */
public class InvasionTemplate {

	public String name = "";
	public InvasionTemplateSelector selector = new InvasionTemplateSelector();
	public InvasionTemplateMessages messages = new InvasionTemplateMessages();
	public InvasionTemplateCommands commands = new InvasionTemplateCommands();
	public InvasionTemplateEarlyEnd earlyEnd = new InvasionTemplateEarlyEnd();
	public InvasionTemplateWave[] waves = new InvasionTemplateWave[0];
}
