package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_links")
@Getter
@Setter
@NoArgsConstructor
public class ChatLinkEntity {
    @EmbeddedId
    private ChatLinkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("linkId")
    @JoinColumn(name = "link_id")
    private LinkEntity link;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "filter", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] filters = new String[0];
}
