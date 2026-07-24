package com.bte.credit_analysis_service.dto;

public record AuthResponse(String token, String role, String email) {}