package com.gurjeet.pm.adapter.in.websocket;

import com.gurjeet.pm.adapter.out.persistence.ProjectMemberRepository;
import com.gurjeet.pm.common.security.AuthUser;
import com.gurjeet.pm.common.security.JwtService;
import com.gurjeet.pm.domain.model.ProjectMemberId;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;
    private final ProjectMemberRepository memberRepository;

    public WsHandshakeInterceptor(JwtService jwtService, ProjectMemberRepository memberRepository) {
        this.jwtService = jwtService;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            String token = params.getFirst("token");
            String projectIdRaw = params.getFirst("projectId");
            if (token == null || projectIdRaw == null) return false;

            AuthUser user = jwtService.parse(token);
            UUID projectId = UUID.fromString(projectIdRaw);
            if (memberRepository.findById(new ProjectMemberId(projectId, user.id())).isEmpty()) {
                return false;
            }
            attributes.put("user", user);
            attributes.put("projectId", projectId);
            String lastSeq = params.getFirst("lastSeq");
            attributes.put("lastSeq", lastSeq == null ? -1L : Long.parseLong(lastSeq));
            String issueId = params.getFirst("issueId");
            if (issueId != null) attributes.put("issueId", UUID.fromString(issueId));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) { }
}
