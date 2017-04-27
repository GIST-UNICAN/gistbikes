package unican.gist.gpsbike.model;

import com.google.gson.annotations.SerializedName;

public class VersionData{

	@SerializedName("version")
	private int version;

	public void setVersion(int version){
		this.version = version;
	}

	public int getVersion(){
		return version;
	}

	@Override
 	public String toString(){
		return 
			"VersionData{" + 
			"version = '" + version + '\'' + 
			"}";
		}
}