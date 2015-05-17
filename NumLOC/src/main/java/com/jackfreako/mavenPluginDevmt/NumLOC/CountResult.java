package com.jackfreako.mavenPluginDevmt.NumLOC;

public class CountResult {

	
	private long total = 0;
	private long empty = 0;
	private long files = 0;
	
	
	
	public long getTotal(){
		return this.total;
	}
	
	public long getEmpty(){
		return this.empty;
	}
	
	public long getFiles(){
		return this.files;
	}
	
	public void addTotal(long mTotal){
		this.total +=  mTotal;
	}
	
	public void addEmpty(long mEmpty){
		this.empty +=mEmpty;
	}
	
	public void incrementFiles(){
		this.files++;
	}
	
}
