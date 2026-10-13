package com.example.pokedex.domain.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.InvalidRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProprietaryFieldsTest {

	@Test
	void keepsValidValuesTrimmed() {
		ProprietaryFields fields = new ProprietaryFields("  Pikachu de Ash ", " Kanto ", List.of(" starter ", "favorite"));

		assertThat(fields.localName()).isEqualTo("Pikachu de Ash");
		assertThat(fields.localRegion()).isEqualTo("Kanto");
		assertThat(fields.tags()).containsExactly("starter", "favorite");
	}

	@Test
	void aBlankLocalNameOrRegionMeansNotSet() {
		ProprietaryFields fields = new ProprietaryFields("   ", "", List.of());

		assertThat(fields.localName()).isNull();
		assertThat(fields.localRegion()).isNull();
	}

	@Test
	void missingTagsMeanNoTags() {
		assertThat(new ProprietaryFields(null, null, null).tags()).isEmpty();
	}

	@Test
	void tagsAreStoredInLowerCase() {
		assertThat(new ProprietaryFields(null, null, List.of("Starter", "FAN-Favorite")).tags())
				.containsExactly("starter", "fan-favorite");
	}

	@Test
	void rejectsALocalNameThatIsTooLong() {
		assertRejected(() -> new ProprietaryFields("x".repeat(101), null, List.of()), "localName");
	}

	@Test
	void acceptsALocalNameOfTheMaximumLength() {
		assertThat(new ProprietaryFields("x".repeat(100), null, List.of()).localName()).hasSize(100);
	}

	@Test
	void rejectsALocalRegionThatIsTooLong() {
		assertRejected(() -> new ProprietaryFields(null, "x".repeat(101), List.of()), "localRegion");
	}

	@Test
	void rejectsABlankTag() {
		assertRejected(() -> new ProprietaryFields(null, null, List.of("starter", "  ")), "tags");
		assertRejected(() -> new ProprietaryFields(null, null, Arrays.asList("starter", null)), "tags");
	}

	@Test
	void rejectsDuplicatedTagsWhateverTheCase() {
		assertRejected(() -> new ProprietaryFields(null, null, List.of("starter", "Starter")), "tags");
	}

	@Test
	void rejectsATagThatIsTooLong() {
		assertRejected(() -> new ProprietaryFields(null, null, List.of("x".repeat(31))), "tags");
	}

	@Test
	void rejectsTooManyTags() {
		List<String> tags = IntStream.rangeClosed(1, 21).mapToObj(i -> "tag" + i).toList();

		assertRejected(() -> new ProprietaryFields(null, null, tags), "tags");
	}

	@Test
	void tagsCannotBeChangedFromOutside() {
		List<String> tags = new ArrayList<>(List.of("starter"));
		ProprietaryFields fields = new ProprietaryFields(null, null, tags);

		tags.add("changed");

		assertThat(fields.tags()).containsExactly("starter");
	}

	private static void assertRejected(Runnable create, String field) {
		assertThatThrownBy(create::run)
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo(field);
	}
}
