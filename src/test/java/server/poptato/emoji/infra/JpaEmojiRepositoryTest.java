package server.poptato.emoji.infra;

import java.util.List;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;
import server.poptato.emoji.domain.entity.Emoji;
import server.poptato.emoji.domain.value.GroupName;

@MySqlDataJpaTest
class JpaEmojiRepositoryTest extends DatabaseTestConfig {

    @Autowired
    private JpaEmojiRepository jpaEmojiRepository;

    /**
     * 테스트 트랜잭션 안에서 persist → flush 로 즉시 INSERT 하고 1차 캐시를 비워
     * 쿼리 결과 검증 시 영속성 컨텍스트 영향(동일성 보장 등)을 최소화함
     */
    private List<Emoji> persistEmojis(int count) {
        List<Emoji> saved = IntStream.rangeClosed(1, count)
                .mapToObj(i -> Emoji.builder()
                        .imageUrl("https://test/" + i + ".png")
                        .groupName(GroupName.운동)
                        .build())
                .map(tem::persist)
                .toList();
        tem.flush();
        tem.clear();
        return saved;
    }

    @Test
    @DisplayName("[SCN-REP-EMOJI-001][TC-REP-EMOJI-001] emoji id별로 imageUrl을 조회할 수 있다.")
    void findImageUrlById_projection() {
        // given
        List<Emoji> saved = persistEmojis(1);
        Long id = saved.get(0).getId();

        // when
        String found = jpaEmojiRepository.findImageUrlById(id);
        String notFound = jpaEmojiRepository.findImageUrlById(Long.MAX_VALUE);

        // then
        Assertions.assertThat(found).isEqualTo("https://test/1.png");
        Assertions.assertThat(notFound).isNull();
    }

    @Test
    @DisplayName("[SCN-REP-EMOJI-002][TC-REP-EMOJI-001] id가 3이상인 모든 이모지를 조회한다.")
    void findAllEmojis_id가_3이상인_emoji_조회() {
        // given
        persistEmojis(6);
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "id"));

        // when
        Page<Emoji> page = jpaEmojiRepository.findAllEmojis(pageable);

        // then
        Assertions.assertThat(page.getSize()).isEqualTo(5);
        Assertions.assertThat(page.getNumber()).isZero();
        Assertions.assertThat(page.getContent().size()).isBetween(0, 5);

        Assertions.assertThat(page.getContent())
                .allSatisfy(e -> Assertions.assertThat(e.getId()).isGreaterThanOrEqualTo(3L));

        List<Long> ids = page.getContent().stream().map(Emoji::getId).toList();
        Assertions.assertThat(ids).isSorted();
    }
}