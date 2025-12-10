package org.atriasoft.archidata.model;

public class Token {
	public Long id;
	public Long userId;
	public String token;
	public String createTime;
	public String endValidityTime;

	public Token() {}

	public Token(final long id, final long userId, final String token, final String createTime,
			final String endValidityTime) {
		this.id = id;
		this.userId = userId;
		this.token = token;
		this.createTime = createTime;
		this.endValidityTime = endValidityTime;
	}

	@Override
	public String toString() {
		return "Token{" + "id=" + this.id + ", userId=" + this.userId + ", token='" + this.token + '\''
				+ ", createTime=" + this.createTime + ", endValidityTime=" + this.endValidityTime + '}';
	}
}
