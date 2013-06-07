package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class CreateChannelValidator
{
	private final ChannelDao channelDao;

	@Inject
	public CreateChannelValidator( ChannelDao channelDao )
	{
		this.channelDao = channelDao;
	}

	public void validate( ChannelCreationRequest channelCreationRequest )
		throws InvalidRequestException, AlreadyExistsException
	{
		String channelName = channelCreationRequest.getName();
		validateChannelName( channelName );
		validateChannelUniqueness( channelName );
	}

	private void validateChannelName( String channelName ) throws InvalidRequestException
	{
		if ( Strings.nullToEmpty( channelName ).trim().isEmpty() )
		{
			throw new InvalidRequestException( "{\"error\":\"Channel name cannot be blank\"}" );
		}
	}

	private void validateChannelUniqueness( String channelName ) throws AlreadyExistsException
	{
		if ( channelDao.channelExists( channelName ) )
		{
			throw new AlreadyExistsException( "{\"error\":\"Channel name " + channelName + " already exists\"}" );
		}
	}
}
