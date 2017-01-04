package io.pivotal.security.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.controller.v1.PasswordGenerationParameters;
import io.pivotal.security.fake.FakeEncryptionService;
import io.pivotal.security.service.EncryptionService;
import io.pivotal.security.util.DatabaseProfileResolver;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.itThrows;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;

import java.time.Instant;
import java.util.UUID;

@RunWith(Spectrum.class)
@ActiveProfiles(value = {"unit-test", "FakeEncryptionService"}, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
public class NamedPasswordSecretTest {
  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  EncryptionService encryptionService;

  NamedPasswordSecret subject;

  PasswordGenerationParameters generationParameters;

  {
    wireAndUnwire(this, false);

    beforeEach(() -> {
      subject = new NamedPasswordSecret("Foo");
      ((FakeEncryptionService) encryptionService).resetEncryptionCount();

      generationParameters = new PasswordGenerationParameters();
      generationParameters.setExcludeLower(true);
      generationParameters.setExcludeSpecial(true);
      generationParameters.setLength(10);
    });

    it("returns type password", () -> {
      assertThat(subject.getSecretType(), equalTo("password"));
    });

    describe("with or without alternative names", () -> {
      beforeEach(() -> {
        subject = new NamedPasswordSecret("foo");
      });

      it("only encrypts the value once for the same secret", () -> {
        subject.setValue("my-value");
        assertThat(((FakeEncryptionService) encryptionService).getEncryptionCount(), equalTo(1));

        subject.setValue("my-value");
        assertThat(((FakeEncryptionService) encryptionService).getEncryptionCount(), equalTo(1));
      });

      it("sets the nonce and the encrypted value", () -> {
        subject.setValue("my-value");
        assertThat(subject.getEncryptedValue(), notNullValue());
        assertThat(subject.getNonce(), notNullValue());
      });

      it("can decrypt values", () -> {
        subject.setValue("my-value");
        assertThat(subject.getValue(), equalTo("my-value"));
      });

      itThrows("when setting a value that is null", IllegalArgumentException.class, () -> {
        subject.setValue(null);
      });

      it("only encrypts the generationParameters once for the same secret", () -> {
        subject.setGenerationParameters(generationParameters);
        assertThat(((FakeEncryptionService) encryptionService).getEncryptionCount(), equalTo(1));

        PasswordGenerationParameters generationParameters2 = new PasswordGenerationParameters();
        generationParameters2.setExcludeLower(true);
        generationParameters2.setExcludeSpecial(true);
        generationParameters2.setLength(10);
        subject.setGenerationParameters(generationParameters2);
        assertThat(((FakeEncryptionService) encryptionService).getEncryptionCount(), equalTo(1));
      });

      it("sets the parametersNonce and the encryptedGenerationParameters", () -> {
        subject.setGenerationParameters(generationParameters);
        assertThat(subject.getEncryptedGenerationParameters(), notNullValue());
        assertThat(subject.getParametersNonce(), notNullValue());
      });

      it("can decrypt values", () -> {
        subject.setValue("length10pw");
        subject.setGenerationParameters(generationParameters);
        assertThat(subject.getGenerationParameters().getLength(), equalTo(10));
        assertThat(subject.getGenerationParameters().isExcludeLower(), equalTo(true));
        assertThat(subject.getGenerationParameters().isExcludeUpper(), equalTo(false));
      });
    });

    describe("#copyInto", () -> {
      it("should copy the correct properties into the other object", () -> {
        Instant frozenTime = Instant.ofEpochSecond(1400000000L);
        UUID uuid = UUID.randomUUID();

        PasswordGenerationParameters parameters = new PasswordGenerationParameters();
        parameters.setExcludeNumber(true);
        parameters.setExcludeLower(true);
        parameters.setExcludeUpper(false);

        subject = new NamedPasswordSecret("foo");
        subject.setValue( "value");
        subject.setGenerationParameters(parameters);
        subject.setUuid(uuid);
        subject.setUpdatedAt(frozenTime);

        NamedPasswordSecret copy = new NamedPasswordSecret();
        subject.copyInto(copy);

        PasswordGenerationParameters copyParameters = copy.getGenerationParameters();

        assertThat(copy.getName(), equalTo("foo"));
        assertThat(copy.getValue(), equalTo("value"));
        assertThat(copyParameters.isExcludeNumber(), equalTo(true));
        assertThat(copyParameters.isExcludeLower(), equalTo(true));
        assertThat(copyParameters.isExcludeUpper(), equalTo(false));

        assertThat(copy.getUuid(), not(equalTo(uuid)));
        assertThat(copy.getUpdatedAt(), not(equalTo(frozenTime)));
      });
    });
  }
}
