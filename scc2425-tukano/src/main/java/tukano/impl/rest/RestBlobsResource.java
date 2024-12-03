package tukano.impl.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Cookie;
import tukano.api.Blobs;
import tukano.api.rest.RestBlobs;
import tukano.impl.JavaBlobs;

import java.io.IOException;

@Singleton
public class RestBlobsResource extends RestResource implements RestBlobs {

	final Blobs impl;
	
	public RestBlobsResource() {
		this.impl = JavaBlobs.getInstance();
	}
	
	@Override
	public void upload(String blobId, byte[] bytes, Cookie cookie) {
		super.resultOrThrow(impl.upload(blobId, bytes, cookie));
	}

	@Override
	public byte[] download(String blobId, Cookie cookie) {
		return super.resultOrThrow(impl.download( blobId, cookie));
	}

	@Override
	public void delete(String blobId, Cookie cookie) {
		super.resultOrThrow(impl.delete( blobId, cookie));
	}
	
	@Override
	public void deleteAllBlobs(String userId, Cookie cookie) {
		super.resultOrThrow(impl.deleteAllBlobs(userId, cookie));
	}
}
