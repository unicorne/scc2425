package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import tukano.impl.Token;
import tukano.impl.data.ShortContainerType;

/**
 * Represents a Short video uploaded by an user.
 * 
 * A short has an unique shortId and is owned by a given user; 
 * Comprises of a short video, stored as a binary blob at some bloburl;.
 * A post also has a number of likes, which can increase or decrease over time. It is the only piece of information that is mutable.
 * A short is timestamped when it is created.
 *
 */
@Entity
public class Short {
	
	@Id
	String id;
	String ownerId;
	String blobUrl;
	long timestamp;
	int totalLikes;
	ShortContainerType type = ShortContainerType.SHORT;

	public Short() {}
	
	public Short(String id, String ownerId, String blobUrl, long timestamp, int totalLikes) {
		super();
		this.id = id;
		this.ownerId = ownerId;
		this.blobUrl = blobUrl;
		this.timestamp = timestamp;
		this.totalLikes = totalLikes;
	}

	public Short(String id, String ownerId, String blobUrl) {
		this(id, ownerId, blobUrl, System.currentTimeMillis(), 0);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String shortId) {
		this.id = shortId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getBlobUrl() {
		return blobUrl;
	}

	public void setBlobUrl(String blobUrl) {
		this.blobUrl = blobUrl;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getTotalLikes() {
		return totalLikes;
	}

	public void setTotalLikes(int totalLikes) {
		this.totalLikes = totalLikes;
	}

	public ShortContainerType getType() {
		return type;
	}

	public void setType(ShortContainerType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Short [shortId=" + id + ", ownerId=" + ownerId + ", blobUrl=" + blobUrl + ", timestamp="
				+ timestamp + ", totalLikes=" + totalLikes + "]";
	}
	
	public Short copyWithLikes_And_Token( long totLikes) {
		var urlWithToken = String.format("%s?token=%s", blobUrl, Token.get(blobUrl));
		return new Short(id, ownerId, urlWithToken, timestamp, (int)totLikes);
	}	
}