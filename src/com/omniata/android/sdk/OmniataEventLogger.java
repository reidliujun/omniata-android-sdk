package com.omniata.android.sdk;

import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;

class OmniataEventLogger implements Runnable {
	
	BlockingQueue<JSONObject>			inputQueue;
	PersistentBlockingQueue<JSONObject> persistantQueue;
	
	public OmniataEventLogger(BlockingQueue<JSONObject> inputQueue, PersistentBlockingQueue<JSONObject> persistantQueue) {
		this.inputQueue 	 = inputQueue;
		this.persistantQueue = persistantQueue;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				persistantQueue.add(inputQueue.take());
			} catch (InterruptedException e) {
				continue;
			}
		}
	}
}
