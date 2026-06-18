# Field encryption (`@DataEncrypt`)

archidata can transparently encrypt selected fields when they are serialized to the database and
decrypt them on read — provided the server is explicitly authorized to decrypt.

## Overview

- Encryption happens at the field-codec level: the stored value is an opaque Base64 *envelope*
  that embeds the scheme identifier; the clear value never reaches the database.
- Decryption on read is **disabled by default**. Without the `DATA_DECRYPT_ENABLE` flag, encrypted
  fields are left unset (`null`) on read and the stored data stays protected.
- Keys are referenced by **logical name** and resolved at runtime from the
  `EncryptionKeyStore`, so they can be injected from Docker secrets or registered dynamically (per
  organisation / sub-domain).

## Supported schemes (JDK-native, no extra dependency)

| Scheme   | Type        | Encrypt key | Decrypt key | Notes |
|----------|-------------|-------------|-------------|-------|
| AES-GCM  | symmetric   | the key     | the key     | one key encrypts and decrypts |
| X25519   | asymmetric  | public      | private     | sealed box (ephemeral ECDH + AES-GCM) |
| RSA      | asymmetric  | public      | private     | RSA-OAEP-SHA256, small payloads only |

> Ed25519 keys are signature keys and **cannot** encrypt. Generate X25519 keys for the asymmetric
> sealed-box scheme (the JDK does not provide an Ed25519→X25519 conversion).

The scheme is auto-detected from the key material: a 16/24/32-byte raw or Base64 value is an AES key;
a PEM `PUBLIC KEY` / `PRIVATE KEY` block is RSA or X25519 depending on its embedded algorithm.

## Annotating a field

Currently supported on `String` fields.

```java
public class UserSecret extends GenericData {
    // Symmetric, base key ("default"):
    @DataEncrypt
    public String token;

    // Asymmetric, distinct named keys (supports encrypt-only deployments):
    @DataEncrypt(encryptKey = "org42-pub", decryptKey = "org42-priv")
    public String iban;
}
```

- `@DataEncrypt` alone uses the base key named `default`.
- `encryptKey` / `decryptKey` override the base key; pointing `decryptKey` at a key that has no
  private material yields an encrypt-only field (the node can store it but cannot read it back).

## Injecting keys

### From Docker secrets (file-based, recommended)

Any secret can be provided either inline via `<NAME>` or, following the Docker convention, via a
file path in `<NAME>_FILE` (the secret mounted under `/run/secrets/...`).

| Variable | Meaning |
|----------|---------|
| `DATA_DECRYPT_ENABLE` | `true` to allow decryption on read (default `false`) |
| `DATA_ENCRYPT_KEY` / `DATA_ENCRYPT_KEY_FILE` | the base key (logical name `default`) |
| `DATA_ENCRYPT_KEYS_DIR` | a directory; each file is loaded as a named key (file name without extension) |

### At runtime (dynamic, per organisation / sub-domain)

```java
EncryptionKeyStore.register("org42-pub", new X25519SealedBoxProvider(publicKey, null));
EncryptionKeyStore.register("org42-priv", new X25519SealedBoxProvider(null, privateKey));
```

## Generating keys

```java
// Symmetric (Base64, write to a secret file):
String aes = EncryptionKeyGenerator.generateAesKeyBase64();

// Asymmetric X25519 / RSA (PEM):
KeyPair x = EncryptionKeyGenerator.generateX25519();
String pubPem  = EncryptionKeyGenerator.toPublicPem(x.getPublic());
String privPem = EncryptionKeyGenerator.toPrivatePem(x.getPrivate());
```

Never commit production keys to the repository.
