package com.ctg.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserIdExtractor {

    public UserId resolve(Jwt jwt) {
        Long uid = jwt.getClaim("uid");
        if (uid != null) {
            return UserId.of(uid);
        }

        String subj = jwt.getSubject();
        if (subj != null && subj.matches("\\d")) {
            try {
                return UserId.of(Long.parseLong(subj));
            } catch (NumberFormatException ex) {
                //log error
            }
        }

        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return UserId.of(email);
        }

        if (subj != null && subj.contains("@")) {
            return UserId.of(subj);
        }

        throw new IllegalStateException("Cannot resolve current user from JWT: neither uid nor email/sub present");
    }

    public sealed interface UserId permits UserId.Numeric, UserId.Subject {
        record Numeric(Long value) implements UserId { }
        record Subject(String value) implements UserId { }

        static Numeric of(Long value) {return new Numeric(value);}
        static Subject of(String value) {return new Subject(value);}

    }
}
