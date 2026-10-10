package com.icr.atlas.eventPublishers;

public interface AtlasSubscriber<T> {
	void update(Object o);

}
