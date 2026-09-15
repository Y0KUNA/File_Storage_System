package com.filestorage.filesystem.messaging;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.EventEnvelope;

public interface DomainEventPublisher {
    public void publish(EventEnvelope event);
}
