package backend.academy.linktracker.bot.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagParserTest {

    TagParser tagParser = new TagParser();

    @Test
    void parseTags_multiple_comma_separated_tags() {
        assertThat(tagParser.parseTags("тег1, тег2, тег3")).containsExactly("тег1", "тег2", "тег3");
    }

    @Test
    void parseTags_trims_whitespace_around_each_tag() {
        assertThat(tagParser.parseTags("  тег1  ,  тег2  ")).containsExactly("тег1", "тег2");
    }

    @Test
    void parseTags_filters_blank_parts_from_extra_commas() {
        assertThat(tagParser.parseTags(",тег1,,тег2,")).containsExactly("тег1", "тег2");
    }

    @Test
    void parseTags_normalizes_multiple_spaces_inside_tag() {
        assertThat(tagParser.parseTags("мой  тег,другой   тег")).containsExactly("мой тег", "другой тег");
    }

    @Test
    void parseTags_empty_input_returns_empty_list() {
        assertThat(tagParser.parseTags("")).isEmpty();
    }
}
