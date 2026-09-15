package com.filestorage.identity.dto;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;


public record RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password, String displayName) {
}
