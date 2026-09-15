package com.filestorage.identity.messaging;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import com.filestorage.common.EventEnvelope;

public interface DomainEventPublisher {
    public void publish(EventEnvelope event);
}

