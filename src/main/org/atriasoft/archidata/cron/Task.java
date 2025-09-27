package org.atriasoft.archidata.cron;

interface Task {
	String name();

	Runnable action();
}