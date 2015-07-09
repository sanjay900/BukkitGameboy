package com.sanjay900.jgbe.emu;
interface CPURunner {
	public void suspend();
	public void resume();
	public boolean isRunning();
	public boolean hasThread(Thread t);
}
