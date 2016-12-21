package stroom.agent.collect;

public class SFTPFileDetails {
	private String name;
	private String dir;
	private long size;

	public SFTPFileDetails(String dir, String name, long size) {
		this.dir = dir;
		this.name = name;
		this.size = size;
	}

	public String getName() {
		return name;
	}

	public String getDir() {
		return dir;
	}

	public long getSize() {
		return size;
	}
	
	public String getPath() {
		return dir + "/" + name;
	}
	@Override
	public String toString() {
		return getPath();
	}
}