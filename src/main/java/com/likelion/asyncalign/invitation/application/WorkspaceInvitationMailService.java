package com.likelion.asyncalign.invitation.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceInvitationMailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendBaseUrl;

    public WorkspaceInvitationMailService(
            JavaMailSender mailSender,
            @Value("${app.email-verification.from:}") String from,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/$", "");
    }

    public void send(String email, String workspaceName, String inviterName, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject("[ditto] " + workspaceName + " 워크스페이스 초대");
        message.setText("""
                %s님이 %s 워크스페이스에 초대했습니다.

                아래 링크에서 초대를 확인하고 합류해 주세요.
                %s/invitations/%s

                초대 링크는 7일 동안 유효합니다.
                """.formatted(inviterName, workspaceName, frontendBaseUrl, token));
        mailSender.send(message);
    }
}
