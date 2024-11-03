package tukano.impl.data;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Likes {

	@Id
	String id;

	ShortContainerType type = ShortContainerType.LIKE;

	@Id 
	String userId;
	
	@Id 
	String shortId;

	String ownerId;
	
	public Likes() {}

	public Likes(String id, String userId, String shortId, String ownerId) {
		this.id = id;
		this.userId = userId;
		this.shortId = shortId;
		this.ownerId = ownerId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getShortId() {
		return shortId;
	}

	public void setShortId(String shortId) {
		this.shortId = shortId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ShortContainerType getType() {
		return type;
	}

	public void setType(ShortContainerType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Likes [id=" + id + ", userId=" + userId + ", shortId=" + shortId + ", ownerId=" + ownerId + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(ownerId, shortId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Likes other = (Likes) obj;
		return Objects.equals(id, other.id) && Objects.equals(ownerId, other.ownerId) && Objects.equals(shortId, other.shortId)
				&& Objects.equals(userId, other.userId);
	}
}
